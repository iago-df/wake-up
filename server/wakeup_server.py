#!/usr/bin/env python3
"""
servidor de wake up: se ejecuta en el pc (o servidor) donde corre claude code
y le da a la app del movil los tokens de cada sesion y una forma de mandar ordenes.

solo usa la libreria estandar de python, no hay que instalar nada:

    python wakeup_server.py

la primera vez crea config.json con un token aleatorio. ese token y la url
(http://IP:8787) son lo que hay que poner en la app, en el apartado Claude.
"""
import glob
import hmac
import json
import os
import re
import secrets
import shutil
import socket
import subprocess
import threading
import time
import uuid
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

VERSION = "0.1"
HERE = os.path.dirname(os.path.abspath(__file__))
CONFIG_PATH = os.environ.get("WAKEUP_CONFIG", os.path.join(HERE, "config.json"))

DEFAULTS = {
    "name": socket.gethostname(),
    "token": "",
    "host": "0.0.0.0",
    "port": 8787,
    # carpeta donde claude code guarda las sesiones
    "claude_dir": os.path.join("~", ".claude"),
    "claude_bin": "claude",
    # argumentos extra para las ordenes, por ejemplo ["--permission-mode", "acceptEdits"]
    "claude_args": [],
    # minutos maximos que puede tardar una orden
    "order_timeout_min": 30,
}


def load_config():
    cfg = dict(DEFAULTS)
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, encoding="utf-8") as f:
            cfg.update(json.load(f))
    if os.environ.get("WAKEUP_TOKEN"):
        cfg["token"] = os.environ["WAKEUP_TOKEN"]
    if os.environ.get("WAKEUP_PORT"):
        cfg["port"] = int(os.environ["WAKEUP_PORT"])
    if not cfg["token"]:
        cfg["token"] = secrets.token_urlsafe(24)
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            json.dump(cfg, f, indent=2, ensure_ascii=False)
        print("config creado en", CONFIG_PATH)
    return cfg


CFG = load_config()

# ---------------------------------------------------------------- sesiones

_cache = {}  # ruta -> (mtime, size, resumen)
_cache_lock = threading.Lock()


def _parse_session(path):
    """lee un .jsonl de claude code y saca titulo, carpeta y tokens por dia"""
    title = None
    cwd = None
    last_prompt = None
    last_ts = None
    seen = set()
    days = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            # mirar solo las lineas que interesan, los ficheros son enormes
            if '"usage"' not in line and '"custom-title"' not in line and '"ai-title"' not in line \
                    and '"summary"' not in line and '"last-prompt"' not in line:
                continue
            try:
                d = json.loads(line)
            except ValueError:
                continue
            t = d.get("type")
            if t == "custom-title":
                title = d.get("customTitle") or title
            elif t == "ai-title":
                title = title or d.get("aiTitle") or d.get("title")
            elif t == "summary":
                title = title or d.get("summary")
            elif t == "last-prompt":
                last_prompt = d.get("lastPrompt") or last_prompt
            elif t == "assistant":
                msg = d.get("message") or {}
                usage = msg.get("usage") or {}
                cwd = d.get("cwd") or cwd
                ts = d.get("timestamp")
                if ts:
                    last_ts = ts
                # cada mensaje sale repetido una vez por bloque, contar solo una
                mid = msg.get("id") or d.get("uuid")
                if mid in seen:
                    continue
                seen.add(mid)
                tokens = (usage.get("input_tokens") or 0) + (usage.get("output_tokens") or 0) \
                    + (usage.get("cache_creation_input_tokens") or 0)
                if tokens and ts:
                    day = _local_day(ts)
                    days[day] = days.get(day, 0) + tokens
    return {"title": title, "cwd": cwd, "last_prompt": last_prompt, "last_ts": last_ts, "days": days}


def _local_day(ts):
    try:
        dt = datetime.fromisoformat(ts.replace("Z", "+00:00")).astimezone()
        return dt.strftime("%Y-%m-%d")
    except ValueError:
        return ts[:10]


def _summary(path):
    st = os.stat(path)
    with _cache_lock:
        hit = _cache.get(path)
        if hit and hit[0] == st.st_mtime and hit[1] == st.st_size:
            return hit[2]
    data = _parse_session(path)
    with _cache_lock:
        _cache[path] = (st.st_mtime, st.st_size, data)
    return data


def usage():
    """tokens de los ultimos 7 dias, por dia y por sesion"""
    root = os.path.join(os.path.expanduser(CFG["claude_dir"]), "projects")
    today = datetime.now().date()
    week = [(today - timedelta(days=i)).strftime("%Y-%m-%d") for i in range(6, -1, -1)]
    cutoff = time.time() - 8 * 86400
    per_day = {d: 0 for d in week}
    sessions = {}

    # sesiones normales y las de los subagentes, que cuelgan de la sesion principal
    files = glob.glob(os.path.join(root, "*", "*.jsonl")) + glob.glob(os.path.join(root, "*", "*", "subagents", "*.jsonl"))
    for path in files:
        try:
            mtime = os.path.getmtime(path)
            if mtime < cutoff:
                continue
            s = _summary(path)
        except OSError:
            continue
        parts = os.path.normpath(path).split(os.sep)
        sub = "subagents" in parts
        sid = parts[-3] if sub else os.path.splitext(parts[-1])[0]
        tokens = sum(n for d, n in s["days"].items() if d in per_day)
        for d, n in s["days"].items():
            if d in per_day:
                per_day[d] += n
        cur = sessions.setdefault(sid, {"id": sid, "title": None, "project": None, "cwd": None, "tokens": 0, "updated": 0})
        cur["tokens"] += tokens
        if not sub:
            cur["updated"] = max(cur["updated"], mtime)
            cur["cwd"] = s["cwd"]
            cur["project"] = os.path.basename(s["cwd"].rstrip("\\/")) if s["cwd"] else None
            cur["title"] = s["title"] or _short(s["last_prompt"]) or cur["project"] or sid[:8]

    now = time.time()
    out = []
    for s in sessions.values():
        if not s["updated"] or s["tokens"] <= 0:
            continue
        out.append({
            "id": s["id"],
            "title": s["title"],
            "project": s["project"] or "",
            "tokens": s["tokens"],
            "active": now - s["updated"] < 300,
            "updated_at": datetime.fromtimestamp(s["updated"]).astimezone().isoformat(timespec="seconds"),
        })
        _session_cwd[s["id"]] = s["cwd"]
    out.sort(key=lambda x: x["updated_at"], reverse=True)
    return {
        "name": CFG["name"],
        "week_tokens": sum(per_day.values()),
        "days": [{"date": d, "tokens": per_day[d]} for d in week],
        "sessions": out[:40],
    }


def _short(text, n=60):
    if not text:
        return None
    text = re.sub(r"\s+", " ", text).strip()
    return text if len(text) <= n else text[: n - 1] + "…"


_session_cwd = {}

# ---------------------------------------------------------------- ordenes

_orders = {}
_orders_lock = threading.Lock()


def _run_order(order):
    binary = shutil.which(CFG["claude_bin"]) or CFG["claude_bin"]
    cmd = [binary, "-p", order["text"]]
    if order.get("session_id"):
        cmd += ["--resume", order["session_id"]]
    cmd += list(CFG.get("claude_args") or [])
    cwd = _session_cwd.get(order.get("session_id")) or os.path.expanduser("~")
    if not os.path.isdir(cwd):
        cwd = os.path.expanduser("~")
    try:
        p = subprocess.run(
            cmd, cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace",
            stdin=subprocess.DEVNULL, timeout=CFG["order_timeout_min"] * 60,
        )
        out = (p.stdout or "").strip()
        if p.returncode != 0:
            order["status"] = "error"
            order["output"] = out or (p.stderr or "").strip() or "claude terminó con código %d" % p.returncode
        else:
            order["status"] = "done"
            order["output"] = out
    except subprocess.TimeoutExpired:
        order["status"] = "error"
        order["output"] = "la orden tardó demasiado y se canceló"
    except OSError as e:
        order["status"] = "error"
        order["output"] = "no se pudo ejecutar claude: %s" % e
    order["finished_at"] = datetime.now().astimezone().isoformat(timespec="seconds")


def new_order(text, session_id=None):
    order = {
        "id": uuid.uuid4().hex[:12],
        "text": text,
        "session_id": session_id,
        "status": "running",
        "output": "",
        "created_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "finished_at": None,
    }
    with _orders_lock:
        _orders[order["id"]] = order
        # no guardar mas de 50
        for old in list(_orders)[:-50]:
            del _orders[old]
    threading.Thread(target=_run_order, args=(order,), daemon=True).start()
    return order


# ---------------------------------------------------------------- http


class Handler(BaseHTTPRequestHandler):
    server_version = "wakeup/" + VERSION

    def log_message(self, fmt, *args):
        print("%s %s" % (self.address_string(), fmt % args))

    def _send(self, code, body):
        raw = json.dumps(body, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def _auth(self):
        got = self.headers.get("Authorization", "")
        ok = got.startswith("Bearer ") and hmac.compare_digest(got[7:].strip(), CFG["token"])
        if not ok:
            self._send(401, {"error": "token inválido"})
        return ok

    def do_GET(self):
        path = self.path.split("?")[0].rstrip("/")
        if path == "/api/health":
            return self._send(200, {"ok": True, "name": CFG["name"], "version": VERSION})
        if not self._auth():
            return
        if path == "/api/usage":
            return self._send(200, usage())
        if path == "/api/orders":
            with _orders_lock:
                return self._send(200, {"orders": list(_orders.values())[-20:]})
        m = re.fullmatch(r"/api/orders/([0-9a-f]+)", path)
        if m:
            order = _orders.get(m.group(1))
            if not order:
                return self._send(404, {"error": "esa orden no existe"})
            return self._send(200, order)
        self._send(404, {"error": "no existe"})

    def do_POST(self):
        path = self.path.split("?")[0].rstrip("/")
        if not self._auth():
            return
        if path != "/api/orders":
            return self._send(404, {"error": "no existe"})
        try:
            length = int(self.headers.get("Content-Length", "0"))
            body = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
        except ValueError:
            return self._send(400, {"error": "json inválido"})
        text = (body.get("text") or "").strip() if isinstance(body, dict) else ""
        if not text:
            return self._send(400, {"error": "falta el texto de la orden"})
        sid = body.get("session_id") or None
        if sid and not re.fullmatch(r"[0-9a-fA-F-]{8,64}", sid):
            return self._send(400, {"error": "session_id inválido"})
        self._send(202, new_order(text, sid))


def main():
    srv = ThreadingHTTPServer((CFG["host"], CFG["port"]), Handler)
    print("wake up server %s · %s" % (VERSION, CFG["name"]))
    print("escuchando en http://%s:%d" % (CFG["host"], CFG["port"]))
    print("token:", CFG["token"])
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()

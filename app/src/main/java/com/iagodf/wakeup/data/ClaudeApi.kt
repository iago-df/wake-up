package com.iagodf.wakeup.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class Session(val id: String, val title: String, val project: String, val tokens: Long, val active: Boolean)
data class ClaudeUsage(val name: String, val weekTokens: Long, val days: List<Long>, val sessions: List<Session>)
data class Order(val id: String, val status: String, val output: String)

/** cliente del servidor de wake up (server/wakeup_server.py) */
object ClaudeApi {
    private fun base(cfg: ServerCfg): String {
        var u = cfg.url.trim().trimEnd('/')
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        return u
    }

    private fun call(cfg: ServerCfg, method: String, path: String, body: JSONObject? = null): JSONObject {
        val con = URL(base(cfg) + path).openConnection() as HttpURLConnection
        try {
            con.requestMethod = method
            con.connectTimeout = 6000
            con.readTimeout = 15000
            con.setRequestProperty("Authorization", "Bearer ${cfg.token.trim()}")
            if (body != null) {
                con.doOutput = true
                con.setRequestProperty("Content-Type", "application/json")
                con.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = con.responseCode
            val text = (if (code in 200..299) con.inputStream else con.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code == 401) throw IllegalStateException("token incorrecto")
            if (code !in 200..299) throw IllegalStateException(runCatching { JSONObject(text).getString("error") }.getOrDefault("error $code"))
            return JSONObject(text)
        } finally {
            con.disconnect()
        }
    }

    suspend fun usage(cfg: ServerCfg): ClaudeUsage = withContext(Dispatchers.IO) {
        val o = call(cfg, "GET", "/api/usage")
        val days = o.getJSONArray("days")
        val ses = o.getJSONArray("sessions")
        ClaudeUsage(
            o.optString("name"),
            o.getLong("week_tokens"),
            (0 until days.length()).map { days.getJSONObject(it).getLong("tokens") },
            (0 until ses.length()).map {
                val s = ses.getJSONObject(it)
                Session(s.getString("id"), s.optString("title"), s.optString("project"), s.getLong("tokens"), s.optBoolean("active"))
            },
        )
    }

    suspend fun send(cfg: ServerCfg, text: String, sessionId: String?): Order = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", text)
        if (sessionId != null) body.put("session_id", sessionId)
        order(call(cfg, "POST", "/api/orders", body))
    }

    suspend fun order(cfg: ServerCfg, id: String): Order = withContext(Dispatchers.IO) { order(call(cfg, "GET", "/api/orders/$id")) }

    private fun order(o: JSONObject) = Order(o.getString("id"), o.getString("status"), o.optString("output"))
}

class Bubble(val me: Boolean, text: String, pending: Boolean = false) {
    var text by mutableStateOf(text)
    var pending by mutableStateOf(pending)
}

/** estado del apartado claude, vive mientras la app esta abierta */
class ClaudeModel(private val state: AppState, private val scope: CoroutineScope) {
    var usage by mutableStateOf<ClaudeUsage?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var selected by mutableStateOf<String?>(null)
    var draft by mutableStateOf("")
    val chat = mutableStateListOf<Bubble>()

    val connected: Boolean get() = usage != null && error == null

    suspend fun refresh() {
        val cfg = state.server
        if (!cfg.ready) { usage = null; error = null; return }
        try {
            usage = ClaudeApi.usage(cfg)
            error = null
        } catch (e: Exception) {
            error = friendly(e)
        }
    }

    /** prueba una configuracion sin guardarla. devuelve el nombre del servidor */
    suspend fun test(cfg: ServerCfg): Result<String> = runCatching { ClaudeApi.usage(cfg).name }.recoverCatching { throw IllegalStateException(friendly(it)) }

    fun forget() { usage = null; error = null; selected = null }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        chat.add(Bubble(true, text))
        val reply = Bubble(false, "En ello…", pending = true)
        chat.add(reply)
        val cfg = state.server
        scope.launch {
            try {
                var o = ClaudeApi.send(cfg, text, selected)
                var tries = 0
                // la orden corre en el pc, voy preguntando hasta que acabe
                while (o.status == "running" && tries < 900) {
                    delay(2000)
                    o = runCatching { ClaudeApi.order(cfg, o.id) }.getOrDefault(o)
                    tries++
                }
                reply.text = when {
                    o.status == "running" -> "sigue trabajando en tu PC…"
                    o.output.isBlank() -> if (o.status == "done") "Hecho." else "algo falló"
                    else -> o.output.take(1500)
                }
            } catch (e: Exception) {
                reply.text = "no se pudo enviar: ${friendly(e)}"
            }
            reply.pending = false
            refresh()
        }
    }

    /** viene de "desarrollar con claude" en un proyecto */
    fun startFor(p: Project) {
        val match = usage?.sessions?.firstOrNull {
            it.title.contains(p.name, ignoreCase = true) || it.project.contains(p.name, ignoreCase = true)
        }
        selected = match?.id
        draft = "Sigue con ${p.name}: "
    }

    private fun friendly(e: Throwable): String = when (e) {
        is java.net.SocketTimeoutException, is java.net.ConnectException, is java.net.UnknownHostException, is java.net.NoRouteToHostException -> "no llego al servidor"
        is java.net.MalformedURLException -> "la url no es válida"
        else -> e.message ?: "error"
    }
}

/** 1,24 M / 312k */
fun fmtTokens(n: Long): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.2f", n / 1_000_000.0).replace('.', ',') + " M"
    n >= 1_000 -> "${n / 1000}k"
    else -> n.toString()
}

package com.iagodf.wakeup.ui.claude

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.ClaudeModel
import com.iagodf.wakeup.data.ServerCfg
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Field
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.Kicker
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.PillButton
import com.iagodf.wakeup.ui.Sheet
import com.iagodf.wakeup.ui.SheetTitle
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.tap
import kotlinx.coroutines.launch

/** aqui cada uno conecta su propio servidor: url + token */
@Composable
fun ServerSheet(state: AppState, model: ClaudeModel, onClose: () -> Unit) {
    val u = LocalU.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(state.server.name) }
    var url by remember { mutableStateOf(state.server.url) }
    var token by remember { mutableStateOf(state.server.token) }
    var msg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var busy by remember { mutableStateOf(false) }

    Sheet(onClose) {
        SheetTitle("Tu servidor", "el PC donde corre Claude Code")
        Kicker("Nombre")
        Gap(12)
        Field(name, { name = it.take(20) }, "PC de casa")
        Gap(22)
        Kicker("Url")
        Gap(12)
        Field(url, { url = it.trim() }, "http://100.64.0.1:8787", mono = true, keyboard = KeyboardType.Uri, size = 24)
        Gap(22)
        Kicker("Token")
        Gap(12)
        Field(token, { token = it.trim() }, "el que imprime el servidor al arrancar", mono = true, keyboard = KeyboardType.Password, secret = true, size = 24)

        Gap(18)
        val m = msg
        T(
            m?.second ?: "arranca server/wakeup_server.py en tu PC. lo mejor es llegar por Tailscale, no lo abras a internet.", 22,
            color = when { m == null -> C.Muted; m.first -> C.Green; else -> Color(0xFFFF6B6B) }, weight = 500, lh = 1.35f,
        )

        Gap(26)
        PillButton(
            if (busy) "Probando…" else "Probar y guardar",
            brush = Brush.linearGradient(listOf(Color(0xFF8F63FF), Color(0xFF5A3DF0))),
            color = Color.White, glow = Color(0xFF8F63FF),
        ) {
            if (busy || url.isBlank()) return@PillButton
            busy = true
            scope.launch {
                val cfg = ServerCfg(url, token, name.trim())
                model.test(cfg)
                    .onSuccess {
                        state.update(cfg.copy(name = cfg.name.ifBlank { it }))
                        model.refresh()
                        msg = true to "conectado a ${cfg.name.ifBlank { it }}"
                    }
                    .onFailure { msg = false to (it.message ?: "no se pudo conectar") }
                busy = false
            }
        }
        if (state.server.ready) {
            Gap(22)
            T(
                "Olvidar servidor", 23,
                Modifier.fillMaxWidth().tap { state.update(ServerCfg()); model.forget(); onClose() }.padding(vertical = u.dp(10)),
                color = Color(0xFFFF6B6B), weight = 600, align = TextAlign.Center,
            )
        }
        Gap(10)
    }
}

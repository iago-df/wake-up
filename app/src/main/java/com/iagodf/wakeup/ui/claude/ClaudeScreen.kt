package com.iagodf.wakeup.ui.claude

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.tr
import com.iagodf.wakeup.data.Bubble
import com.iagodf.wakeup.data.ClaudeModel
import com.iagodf.wakeup.data.Session
import com.iagodf.wakeup.data.fmtTokens
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.GlowCard
import com.iagodf.wakeup.ui.Header
import com.iagodf.wakeup.ui.Inter
import com.iagodf.wakeup.ui.Kicker
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.Mono
import com.iagodf.wakeup.ui.Pals
import com.iagodf.wakeup.ui.Panel
import com.iagodf.wakeup.ui.PillButton
import com.iagodf.wakeup.ui.ScreenColumn
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.glass
import com.iagodf.wakeup.ui.tap
import kotlinx.coroutines.delay

private val VIOLET = Color(0xFF8F63FF)

@Composable
fun ClaudeScreen(state: AppState, model: ClaudeModel) {
    val u = LocalU.current
    var sheet by remember { mutableStateOf(false) }
    val cfg = state.server
    val usage = model.usage

    // mientras se ve el apartado, refrescar cada 30 s
    LaunchedEffect(cfg) {
        while (true) { model.refresh(); delay(30_000) }
    }

    val ime = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val typing = ime > 0.dp

    Box(Modifier.fillMaxSize()) {
        ScreenColumn(bottom = 330) {
            Header(
                "Claude",
                when {
                    !cfg.ready -> tr("conecta tu servidor", "connect your server")
                    model.error != null -> model.error!!
                    else -> tr("tu PC, en directo", "your PC, live")
                },
            ) {
                // chip con el nombre del servidor
                Row(
                    Modifier.glass(CircleShape).tap { sheet = true }.padding(horizontal = u.dp(24), vertical = u.dp(12)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val col = when {
                        !cfg.ready -> Color(0xFF85859A)
                        model.error != null -> Color(0xFFFF6B6B)
                        else -> C.Green
                    }
                    Dot(col)
                    T(
                        if (!cfg.ready) tr("Conectar", "Connect") else cfg.name.ifBlank { usage?.name ?: tr("Servidor", "Server") },
                        22, color = col, weight = 600, maxLines = 1,
                    )
                }
            }

            Gap(24)
            Tokens(usage?.weekTokens, usage?.days ?: emptyList())

            Gap(22)
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = u.dp(34), vertical = u.dp(32))) {
                    Kicker(tr("Sesiones en tu PC", "Sessions on your PC"))
                    Gap(14)
                    val sessions = usage?.sessions ?: emptyList()
                    when {
                        !cfg.ready -> {
                            T(tr("todavía no hay ningún servidor conectado. arranca wakeup_server.py en tu PC y pon aquí su url y su token.", "no server connected yet. start wakeup_server.py on your PC and enter its url and token here."), 24, color = C.Muted, weight = 400, lh = 1.35f)
                            Gap(24)
                            PillButton(tr("Conectar servidor", "Connect server"), brush = Brush.linearGradient(listOf(VIOLET, Color(0xFF5A3DF0))), color = Color.White) { sheet = true }
                        }
                        sessions.isEmpty() -> T(
                            if (model.error != null) tr("no hay conexión con el servidor", "can't reach the server") else tr("sin sesiones esta semana", "no sessions this week"), 24,
                            Modifier.padding(vertical = u.dp(10)), color = C.Muted,
                        )
                    }
                    sessions.take(8).forEachIndexed { i, s ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(u.dp(1.5)).background(C.Line))
                        SessionRow(s, s.id == model.selected) { model.selected = if (model.selected == s.id) null else s.id }
                    }
                }
            }

            model.chat.forEach { ChatBubble(it) }
        }

        // barra para escribir ordenes, con el borde de colores girando
        val target = usage?.sessions?.firstOrNull { it.id == model.selected }
        InputBar(
            value = model.draft,
            onChange = { model.draft = it },
            hint = if (target != null) "${tr("Orden para", "Order for")} ${target.title}" else tr("Escribe una orden…", "Type an order…"),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = u.dp(30), end = u.dp(30), bottom = if (typing) ime + u.dp(20) else nav + u.dp(168)),
        ) {
            if (!cfg.ready) sheet = true else model.send()
        }
    }

    if (sheet) ServerSheet(state, model) { sheet = false }
}

@Composable
private fun Dot(color: Color) {
    val u = LocalU.current
    Box(
        Modifier
            .padding(end = u.dp(12))
            .size(u.dp(14))
            .drawBehind { drawCircle(color.copy(alpha = .35f), radius = size.width * 1.1f) }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Tokens(week: Long?, days: List<Long>) {
    val u = LocalU.current
    GlowCard(Pals.P, Modifier.fillMaxWidth().height(u.dp(330))) {
        Column(Modifier.padding(horizontal = u.dp(40), vertical = u.dp(36))) {
            T(tr("TOKENS · ESTA SEMANA", "TOKENS · THIS WEEK"), 21, color = C.Soft, weight = 600, ls = 5, maxLines = 1)
            T(if (week != null) fmtTokens(week) else "0", 132, weight = 600, ls = -6, lh = 1.06f, maxLines = 1)
        }
        // una barra por dia, la mas alta en blanco
        val list = if (days.size == 7) days else List(7) { 0L }
        val max = list.max().coerceAtLeast(1)
        Row(
            Modifier.align(Alignment.TopEnd).padding(end = u.dp(40), top = u.dp(44)).height(u.dp(150)),
            horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
            verticalAlignment = Alignment.Bottom,
        ) {
            list.forEach { v ->
                val hi = v == max && v > 0
                val shape = RoundedCornerShape(u.dp(9))
                Box(
                    Modifier
                        .width(u.dp(26))
                        .fillMaxHeight((v.toFloat() / max).coerceIn(.1f, 1f))
                        .then(if (hi) Modifier.shadow(u.dp(20), shape, ambientColor = Color.White, spotColor = Color.White) else Modifier)
                        .clip(shape)
                        .background(if (hi) Color.White else Color.White.copy(alpha = .22f))
                )
            }
        }
    }
}

@Composable
private fun SessionRow(s: Session, selected: Boolean, onClick: () -> Unit) {
    val u = LocalU.current
    Row(
        Modifier.fillMaxWidth().tap(onClick = onClick).padding(vertical = u.dp(17)),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = u.dp(20))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.active) Dot(C.Green)
                T(s.title, 28, color = if (selected) Color(0xFFB79CFF) else Color.White, weight = 600, maxLines = 1)
            }
            T(if (selected) "${s.project} · ${tr("las órdenes van aquí", "orders go here")}" else s.project, 22, color = C.Muted, weight = 400, maxLines = 1)
        }
        T(fmtTokens(s.tokens), 24, color = Color(0xFFD6D6E2), weight = 600, family = Mono, maxLines = 1)
    }
}

@Composable
private fun ChatBubble(b: Bubble) {
    val u = LocalU.current
    Gap(18)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (b.me) Alignment.CenterEnd else Alignment.CenterStart) {
        val r = u.dp(32)
        val shape = if (b.me) RoundedCornerShape(r, r, u.dp(8), r) else RoundedCornerShape(r, r, r, u.dp(8))
        Box(
            Modifier
                .widthIn(max = u.dp(570))
                .then(if (b.me) Modifier.shadow(u.dp(30), shape, ambientColor = VIOLET, spotColor = VIOLET) else Modifier)
                .clip(shape)
                .background(if (b.me) Brush.linearGradient(listOf(VIOLET, Color(0xFF5A3DF0))) else SolidColor(Color(0xFF15151C)))
                .then(if (b.me) Modifier else Modifier.border(u.dp(1.5), Color.White.copy(alpha = .08f), shape))
                .padding(horizontal = u.dp(28), vertical = u.dp(20)),
        ) { T(b.text, 26, color = if (b.pending) C.Soft else Color.White, weight = 500, lh = 1.3f) }
    }
}

@Composable
private fun InputBar(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier, onSend: () -> Unit) {
    val u = LocalU.current
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "angle",
    )
    val colors = listOf(VIOLET, Color(0xFFFF6AD5), Color(0xFFFFB36B), Color(0xFF5CC8FF), VIOLET)
    Box(
        modifier
            .fillMaxWidth()
            .height(u.dp(96))
            .shadow(u.dp(30), CircleShape, ambientColor = VIOLET, spotColor = VIOLET)
            .clip(CircleShape)
            .drawBehind {
                rotate(spin) { drawCircle(Brush.sweepGradient(colors, center), radius = size.width, center = center) }
            }
            .padding(u.dp(3))
            .clip(CircleShape)
            .background(Color(0xFF0A0A0F)),
    ) {
        Row(Modifier.fillMaxSize().padding(start = u.dp(34), end = u.dp(12)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) T(hint, 26, color = C.Muted, weight = 500, maxLines = 1)
                BasicTextField(
                    value, onChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontFamily = Inter, fontWeight = FontWeight(500), fontSize = u.sp(26)),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                )
            }
            // boton de enviar: circulo blanco con el triangulo
            Canvas(Modifier.padding(start = u.dp(12)).size(u.dp(66)).tap(onClick = onSend)) {
                drawCircle(Color.White)
                val w = size.width
                val p = Path().apply {
                    moveTo(w * .38f, w * .29f); lineTo(w * .71f, w * .5f); lineTo(w * .38f, w * .71f); close()
                }
                drawPath(p, Color(0xFF0A0A0F))
            }
        }
    }
}

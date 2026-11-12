package com.iagodf.wakeup.ui.tiempo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.Lang
import com.iagodf.wakeup.data.fmtDec
import com.iagodf.wakeup.data.tr
import com.iagodf.wakeup.data.Block
import com.iagodf.wakeup.data.Schedule
import com.iagodf.wakeup.data.fmtH
import com.iagodf.wakeup.data.fmtHM
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.GlowCard
import com.iagodf.wakeup.ui.Header
import com.iagodf.wakeup.ui.Kicker
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.Pal
import com.iagodf.wakeup.ui.Pals
import com.iagodf.wakeup.ui.Panel
import com.iagodf.wakeup.ui.RoundBtn
import com.iagodf.wakeup.ui.ScreenColumn
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.glass
import com.iagodf.wakeup.ui.tap
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val DATE: DateTimeFormatter
    get() = if (Lang.en) DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
    else DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

private data class HeroData(val kicker: String, val big: String, val label: String, val sub: String, val pal: Pal, val lit: Int, val small: Boolean = false)

@Composable
fun TiempoScreen(state: AppState, onCalendar: () -> Unit, onDay: () -> Unit) {
    val u = LocalU.current
    val ctx = LocalContext.current
    var range by rememberSaveable { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    val today = state.today

    val hero = when (range) {
        0 -> HeroData(
            tr("TIEMPO LIBRE CONSUMIDO", "FREE TIME CONSUMED"), "${state.pct(today)}%", tr("de tu tiempo libre de hoy", "of your free time today"),
            "${fmtHM(state.phoneMinutes(today))} · ${state.unlocks(today)} ${if (state.unlocks(today) == 1) tr("desbloqueo", "unlock") else tr("desbloqueos", "unlocks")}", Pals.O, state.pct(today) * 60 / 100,
        )
        1 -> HeroData(
            tr("ÚLTIMOS 7 DÍAS", "LAST 7 DAYS"), "${state.weekPct}%", tr("de tu tiempo libre esta semana", "of your free time this week"),
            "${fmtHM(state.weekPhone)} ${tr("de móvil", "on your phone")}", Pals.P, state.weekPct * 60 / 100,
        )
        else -> {
            val y = state.lifeYears
            val txt = if (y >= 10f) y.roundToInt().toString() else fmtDec(y)
            HeroData(
                tr("TU VIDA", "YOUR LIFE"), "$txt ${if (txt == "1") tr("año", "year") else tr("años", "years")}", tr("mirando una pantalla, a este ritmo", "staring at a screen, at this pace"),
                tr("de aquí a los 80", "from now until you're 80"), Pals.R, (y / (80 - state.age).coerceAtLeast(1) * 60).roundToInt(), small = true,
            )
        }
    }

    ScreenColumn {
        Header(tr("Tiempo", "Time"), today.format(DATE).lowercase()) { RoundBtn("⚙") { settings = true } }
        Gap(24)
        Segmented(listOf(tr("Hoy", "Today"), tr("7 días", "7 days"), tr("Vida", "Life")), range) { range = it }

        if (!state.hasAccess) {
            Gap(22)
            GlowCard(Pals.R, Modifier.fillMaxWidth().tap { openUsageAccess(ctx) }, radius = 42, cy = 1.3f) {
                Column(Modifier.padding(horizontal = u.dp(34), vertical = u.dp(28))) {
                    T(tr("Falta un permiso", "Permission needed"), 30, weight = 650)
                    T(tr("toca aquí y activa el acceso de uso para wake up", "tap here and turn on usage access for wake up"), 23, color = C.Text, weight = 500)
                }
            }
        }

        Gap(22)
        Hero(hero)

        // libre real y clases
        val week = range == 1
        val free = if (week) (state.weekFree - state.weekPhone) else (state.freeMinutes(today) - state.phoneMinutes(today))
        val classes = if (week) state.weekClass else state.classMinutes(today)
        Gap(22)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(22))) {
            Stat(tr("LIBRE REAL", "REAL FREE"), hours(free.coerceAtLeast(0)), Pals.G, Modifier.weight(1f))
            Stat(tr("CLASES", "CLASSES"), hours(classes), Pals.B, Modifier.weight(1f))
        }

        // lo que pudo ser
        Gap(22)
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = u.dp(34), vertical = u.dp(32))) {
                Kicker(tr("Lo que pudo ser · semana", "What could have been · week"))
                Gap(14)
                val lost = state.lostHours()
                if (state.projects.isEmpty()) T(tr("añade proyectos para ver a quién le quitas las horas", "add projects to see who you take the hours from"), 24, color = C.Muted)
                state.projects.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = u.dp(13)), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        T(p.name, 28, Modifier.weight(1f), weight = 500, maxLines = 1)
                        T("+${lost[p.id] ?: 0} h", 32, color = C.OrangeSoft, weight = 650)
                    }
                }
            }
        }

        // horario de hoy
        Gap(22)
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = u.dp(34), vertical = u.dp(32))) {
                Row(Modifier.fillMaxWidth().tap(onClick = onCalendar), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Kicker(tr("Horario · hoy", "Schedule · today"))
                    T(tr("SEMANA ›", "WEEK ›"), 21, color = Color.White, weight = 600, ls = 3)
                }
                Gap(14)
                val blocks = Schedule.blocksOn(today)
                if (blocks.isEmpty()) {
                    T(tr("hoy no hay clases", "no classes today"), 26, Modifier.padding(vertical = u.dp(10)), color = C.Muted, weight = 500)
                }
                blocks.forEachIndexed { i, b ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(u.dp(1.5)).background(C.Line))
                    ClassRow(b, state) { state.toggleAttended(b, today) }
                }
            }
        }

        // mi dia: horas reales de hoy por proyecto (lo apuntado menos el rato de movil)
        Gap(22)
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = u.dp(34), vertical = u.dp(32))) {
                Row(Modifier.fillMaxWidth().tap(onClick = onDay), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Kicker(tr("Mi día · real", "My day · real"))
                    T(tr("APUNTAR ›", "LOG ›"), 21, color = Color.White, weight = 600, ls = 3)
                }
                Gap(14)
                val done = state.projects.map { it to state.realMinutes(it.id, today) }.filter { it.second > 0 }
                if (done.isEmpty()) {
                    T(tr("todavía no has apuntado nada hoy", "nothing logged today yet"), 26, Modifier.fillMaxWidth().tap(onClick = onDay).padding(vertical = u.dp(10)), color = C.Muted, weight = 500)
                }
                done.forEach { (p, min) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = u.dp(13)), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        T(p.name, 28, Modifier.weight(1f), weight = 500, maxLines = 1)
                        T(fmtHM(min), 30, color = C.Green, weight = 650)
                    }
                }
            }
        }
    }

    if (settings) SettingsSheet(state) { settings = false }
}

private fun hours(min: Int): String = if (min >= 600) "${(min / 60f).roundToInt()} h" else fmtH(min)

@Composable
private fun ClassRow(b: Block, state: AppState, onToggle: () -> Unit) {
    val u = LocalU.current
    val went = b.key(state.today) in state.attended
    val pending = !went && Schedule.isPending(b, state.today, state.now)
    Row(
        Modifier.fillMaxWidth().tap(onClick = onToggle).padding(vertical = u.dp(17)),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            T(b.code, 28, weight = 500, maxLines = 1)
            T("${b.start} – ${b.end} · ${b.room}", 22, color = C.Muted, weight = 400, maxLines = 1)
        }
        val (text, color) = when {
            went -> tr("Fui", "Went") to C.Green
            pending -> tr("Luego", "Later") to Color(0xFFB0B0BE)
            else -> tr("No fui", "Missed") to Color(0xFFFF6B6B)
        }
        val bg by animateColorAsState(color.copy(alpha = if (pending) .08f else .15f), label = "pill")
        Box(
            Modifier
                .drawBehind {
                    if (!went && !pending) drawCircle(
                        Brush.radialGradient(listOf(C.Red.copy(alpha = .3f), Color.Transparent), radius = size.width * .8f),
                        radius = size.width * .8f,
                    )
                }
                .clip(CircleShape)
                .background(bg)
                .border(u.dp(1.5), color.copy(alpha = if (pending) .2f else .38f), CircleShape)
                .padding(horizontal = u.dp(26), vertical = u.dp(11)),
        ) { T(text, 23, color = color, weight = 650, maxLines = 1) }
    }
}

@Composable
private fun Stat(label: String, value: String, pal: Pal, modifier: Modifier) {
    val u = LocalU.current
    GlowCard(pal, modifier.height(u.dp(226)), radius = 42) {
        Column(Modifier.padding(horizontal = u.dp(32), vertical = u.dp(30))) {
            T(label, 21, color = C.Label, weight = 600, ls = 4, maxLines = 1)
            T(value, 86, weight = 600, ls = -4, lh = 1.1f, maxLines = 1)
        }
    }
}

@Composable
private fun Hero(d: HeroData) {
    val u = LocalU.current
    val c1 by animateColorAsState(d.pal.c1, tween(500), label = "c1")
    val c2 by animateColorAsState(d.pal.c2, tween(500), label = "c2")
    val lit by animateFloatAsState(d.lit.coerceIn(0, 60).toFloat(), tween(700), label = "lit")
    GlowCard(c1, c2, Modifier.fillMaxWidth().height(u.dp(486)), cy = 1.17f) {
        Column(Modifier.fillMaxSize().padding(horizontal = u.dp(40), vertical = u.dp(36))) {
            AnimatedContent(d, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) }, label = "hero") { h ->
                Column {
                    T(h.kicker, 21, color = C.Soft, weight = 600, ls = 5, maxLines = 1)
                    Box(Modifier.height(u.dp(208)), contentAlignment = Alignment.CenterStart) {
                        if (h.small) T(h.big, if (h.big.length <= 8) 150 else 150 * 8 / h.big.length, weight = 600, ls = -8, lh = 1f, maxLines = 1)
                        else T(h.big, 200, weight = 600, ls = -10, lh = 1f, maxLines = 1)
                    }
                    T(h.label, 28, color = C.Text, weight = 500, maxLines = 1)
                    Gap(8)
                    T(h.sub, 23, color = Color(0xFFA0A0AE), weight = 400, maxLines = 1)
                }
            }
            Spacer(Modifier.weight(1f))
            Ticks(lit)
        }
    }
}

/** la regla de 60 rayitas de la tarjeta grande */
@Composable
private fun Ticks(lit: Float) {
    val u = LocalU.current
    Canvas(Modifier.fillMaxWidth().height(u.dp(46))) {
        val w = u.dp(4).toPx()
        val short = u.dp(26).toPx()
        val r = CornerRadius(u.dp(3).toPx())
        val step = (size.width - w) / 59f
        for (i in 0 until 60) {
            val h = if (i % 5 == 0) size.height else short
            val on = i < lit.roundToInt()
            val x = i * step
            if (on) drawRoundRect(Color.White.copy(alpha = .28f), Offset(x - w, size.height - h - w), Size(w * 3, h + w * 2), CornerRadius(w * 1.5f))
            drawRoundRect(if (on) Color.White else Color.White.copy(alpha = .2f), Offset(x, size.height - h), Size(w, h), r)
        }
    }
}

/** selector hoy / 7 dias / vida con la pastilla de cristal que se desliza */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val u = LocalU.current
    val pos by animateFloatAsState(selected.toFloat(), spring(dampingRatio = .78f, stiffness = 320f), label = "seg")
    BoxWithConstraints(Modifier.fillMaxWidth().height(u.dp(88)).glass(CircleShape)) {
        val pad = u.dp(6)
        val segW = (maxWidth - pad * 2) / options.size
        val squash = sin(PI * (pos - floor(pos))).toFloat().coerceAtLeast(0f)
        Box(
            Modifier
                .offset(x = pad + segW * pos, y = pad)
                .width(segW)
                .height(u.dp(76))
                .graphicsLayer { scaleX = 1f + .16f * squash; scaleY = 1f - .12f * squash }
                .shadow(u.dp(18), CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF4B4B52), Color(0xFF1F1F25))))
                .border(
                    u.dp(1.5),
                    Brush.verticalGradient(0f to Color.White.copy(alpha = .6f), .4f to Color.White.copy(alpha = .18f), 1f to Color.White.copy(alpha = .18f)),
                    CircleShape,
                )
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, o ->
                val col by animateColorAsState(if (i == selected) Color.White else Color(0xFF8F8F9C), label = "segc")
                Box(Modifier.weight(1f).fillMaxHeight().tap { onSelect(i) }, contentAlignment = Alignment.Center) {
                    T(o, 27, color = col, weight = 600, align = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

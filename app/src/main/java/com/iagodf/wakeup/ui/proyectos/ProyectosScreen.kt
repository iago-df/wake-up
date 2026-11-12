package com.iagodf.wakeup.ui.proyectos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.Project
import com.iagodf.wakeup.data.tr
import com.iagodf.wakeup.data.fmtH
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.Header
import com.iagodf.wakeup.ui.Inter
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.RoundBtn
import com.iagodf.wakeup.ui.ScreenColumn
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.tap

@Composable
fun ProyectosScreen(state: AppState, onLog: () -> Unit, onClaude: (Project) -> Unit) {
    var open by remember { mutableStateOf<Long?>(null) }
    val lost = state.lostHours()
    ScreenColumn {
        Header(tr("Proyectos", "Projects"), tr("por prioridad", "by priority")) {
            RoundBtn("+") {
                val p = Project(System.currentTimeMillis(), tr("Nuevo", "New"), 2, 0, state.projects.size % LOOKS.size)
                state.upsert(p)
                open = p.id
            }
        }
        state.projects.forEachIndexed { i, p ->
            Gap(22)
            ProjectCard(i + 1, p, lost[p.id] ?: 0, state.weekReal(p.id)) { open = p.id }
        }
        if (state.projects.isEmpty()) {
            Gap(60)
            T(tr("todavía no hay proyectos.\ntoca + para añadir el primero", "no projects yet.\ntap + to add the first one"), 27, Modifier.fillMaxWidth(), color = Color(0xFF8D8D9B), align = TextAlign.Center)
        }
    }
    val current = state.projects.firstOrNull { it.id == open }
    if (current != null) {
        ProjectSheet(state, current, onClose = { open = null }, onLog = { open = null; onLog() }, onClaude = { open = null; onClaude(current) })
    }
}

/** tarjeta grande de paisaje, con el nombre gigante cortado por abajo */
@Composable
fun ProjectCard(rank: Int, p: Project, lost: Int, real: Int, onClick: () -> Unit) {
    val u = LocalU.current
    val shape = RoundedCornerShape(u.dp(46))
    val look = LOOKS[p.look.mod(LOOKS.size)]
    val bmp = remember(p.look) { landscape(look, 1020, 615).asImageBitmap() }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(u.dp(410))
            .clip(shape)
            .border(u.dp(1.5), Color.White.copy(alpha = .1f), shape)
            .tap(onClick = onClick),
    ) {
        Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Chip("#$rank · ${fmtH(real)} ${tr("de", "of")} ${p.hoursPerWeek} h", Color.White.copy(alpha = .2f), Modifier.align(Alignment.TopStart))
        Chip("+$lost h ${tr("perdidas", "lost")}", Color(0x800A0A0E), Modifier.align(Alignment.TopEnd))

        // el nombre ocupa todo el ancho, como en el video (maximo 200px)
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val size = remember(p.name, widthPx) {
            val probe = measurer.measure(
                p.name, TextStyle(fontFamily = Inter, fontWeight = FontWeight(750), fontSize = 100.sp, letterSpacing = (-4.8).sp), maxLines = 1, softWrap = false,
            )
            val perPx = probe.size.width / with(density) { 100.sp.toPx() }
            (widthPx * .97f / perPx).coerceAtMost(with(density) { u.dp(200).toPx() })
        }
        val sizeDesign = size / with(density) { u.dp(1).toPx() }
        T(
            p.name, sizeDesign,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().offset(y = u.dp(sizeDesign * .25f)),
            weight = 750, ls = -sizeDesign * .048f, lh = 1f, align = TextAlign.Center, maxLines = 1,
            brush = Brush.verticalGradient(0f to Color.White.copy(alpha = .98f), .2f to Color.White.copy(alpha = .98f), 1f to Color.White.copy(alpha = .4f)),
        )
    }
}

@Composable
private fun Chip(text: String, bg: Color, modifier: Modifier) {
    val u = LocalU.current
    Box(
        modifier
            .padding(u.dp(24))
            .clip(CircleShape)
            .background(bg)
            .border(u.dp(1.5), Color.White.copy(alpha = .45f), CircleShape)
            .padding(horizontal = u.dp(24), vertical = u.dp(11)),
    ) { T(text, 22, weight = 600, maxLines = 1) }
}

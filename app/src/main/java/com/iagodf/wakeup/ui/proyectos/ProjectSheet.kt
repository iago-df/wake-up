package com.iagodf.wakeup.ui.proyectos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.Project
import com.iagodf.wakeup.data.tr
import com.iagodf.wakeup.data.fmtH
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Field
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.Kicker
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.PillButton
import com.iagodf.wakeup.ui.RoundBtn
import com.iagodf.wakeup.ui.Sheet
import com.iagodf.wakeup.ui.SheetTitle
import com.iagodf.wakeup.ui.Stepper
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.tap

@Composable
fun ProjectSheet(state: AppState, p: Project, onClose: () -> Unit, onLog: () -> Unit, onClaude: () -> Unit) {
    val u = LocalU.current
    val rank = state.projects.indexOfFirst { it.id == p.id } + 1
    var name by remember(p.id) { mutableStateOf(p.name) }
    var confirm by remember { mutableStateOf(false) }
    Sheet(onClose) {
        SheetTitle(p.name.ifBlank { tr("Proyecto", "Project") }, "#$rank ${tr("de", "of")} ${state.projects.size}")
        Field(name, { name = it.take(18); state.upsert(p.copy(name = name.trim())) }, tr("nombre", "name"))
        Gap(16)

        Stepper(tr("Horas por semana", "Hours per week"), "${p.hoursPerWeek} h", tr("lo que le quiero dedicar", "what I want to give it"),
            onMinus = { state.upsert(p.copy(hoursPerWeek = (p.hoursPerWeek - 1).coerceAtLeast(1))) },
            onPlus = { state.upsert(p.copy(hoursPerWeek = (p.hoursPerWeek + 1).coerceAtMost(60))) })

        // prioridad: subir o bajar en la lista
        Row(Modifier.fillMaxWidth().padding(vertical = u.dp(15)), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(tr("Prioridad", "Priority"), 28, weight = 500)
                T(tr("el orden en que reparto las horas", "the order the hours are handed out"), 21, color = C.Muted, weight = 400)
            }
            RoundBtn("↓", size = 66) { state.move(p.id, 1) }
            T("#$rank", 32, Modifier.width(u.dp(132)), weight = 650, align = TextAlign.Center, maxLines = 1)
            RoundBtn("↑", size = 66) { state.move(p.id, -1) }
        }

        Gap(18)
        // horas reales de la semana: lo apuntado en mi dia menos el rato de movil
        val real = state.weekReal(p.id)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Kicker(tr("Esta semana", "This week"))
            T("${fmtH(real)} ${tr("de", "of")} ${p.hoursPerWeek} h", 23, weight = 650)
        }
        Gap(14)
        WeekBar(real / (p.hoursPerWeek * 60f))
        Gap(12)
        T(tr("horas reales: lo que apuntas en mi día menos el rato de móvil", "real hours: what you log in my day minus the phone time"), 21, color = C.Muted, weight = 400)
        Gap(18)
        PillButton(tr("Apuntar horas", "Log hours"), onClick = onLog)

        Gap(30)
        Kicker(tr("Paisaje", "Landscape"))
        Gap(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
            LOOKS.forEachIndexed { i, look ->
                val bmp = remember(i) { landscape(look, 170, 130).asImageBitmap() }
                val shape = RoundedCornerShape(u.dp(20))
                Image(
                    bmp, null,
                    Modifier
                        .weight(1f)
                        .height(u.dp(84))
                        .clip(shape)
                        .border(u.dp(if (p.look == i) 3 else 1.5), Color.White.copy(alpha = if (p.look == i) 1f else .14f), shape)
                        .tap { state.upsert(p.copy(look = i)) },
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Gap(34)
        PillButton(
            tr("Desarrollar con Claude", "Build with Claude"),
            brush = Brush.linearGradient(listOf(Color(0xFF8F63FF), Color(0xFF5A3DF0))),
            color = Color.White, glow = Color(0xFF8F63FF), onClick = onClaude,
        )
        Gap(22)
        T(
            if (confirm) tr("toca otra vez para eliminarlo", "tap again to delete it") else tr("Eliminar proyecto", "Delete project"), 23,
            Modifier.fillMaxWidth().tap { if (confirm) { state.delete(p.id); onClose() } else confirm = true }.padding(vertical = u.dp(10)),
            color = Color(0xFFFF6B6B), weight = 600, align = TextAlign.Center,
        )
        Gap(10)
    }
}

/** cuanto llevo de las horas de la semana */
@Composable
fun WeekBar(fraction: Float) {
    val u = LocalU.current
    Box(Modifier.fillMaxWidth().height(u.dp(28)).clip(CircleShape).background(Color.White.copy(alpha = .08f))) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF0A9D6A), Color(0xFF62FFA0))))
        )
    }
}

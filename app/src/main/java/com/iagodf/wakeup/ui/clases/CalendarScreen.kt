package com.iagodf.wakeup.ui.clases

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.Block
import com.iagodf.wakeup.data.Schedule
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.Header
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.Mono
import com.iagodf.wakeup.ui.RoundBtn
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.glass
import com.iagodf.wakeup.ui.tap
import com.iagodf.wakeup.ui.tiempo.Segmented
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val DAYS = listOf("L", "M", "X", "J", "V")

// el horario oficial va de 08:30 a 14:30 y de 15:30 a 19:30, en filas de media hora
private val MORNING = LocalTime.of(8, 30)
private val AFTERNOON = LocalTime.of(15, 30)
private const val MORNING_SLOTS = 12
private const val AFTERNOON_SLOTS = 8
private const val BREAK = .45f
private const val SLOTS = MORNING_SLOTS + AFTERNOON_SLOTS + BREAK

/** en que fila (de media hora) cae una hora */
private fun row(t: LocalTime): Float {
    val m = t.toSecondOfDay() / 60
    return if (t < AFTERNOON) (m - MORNING.toSecondOfDay() / 60) / 30f
    else MORNING_SLOTS + BREAK + (m - AFTERNOON.toSecondOfDay() / 60) / 30f
}

/**
 * pantalla de clases: el horario de la semana a cuadros (blanco = fui, negro = no fui)
 * y "mi dia", donde apunto lo que hice en cada tramo
 */
@Composable
fun CalendarScreen(state: AppState, startTab: Int, onClose: () -> Unit) {
    val u = LocalU.current
    BackHandler(onBack = onClose)
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val monday = state.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
    val semester = Schedule.semesterOn(monday) ?: Schedule.semesterOn(monday.plusDays(4))

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .tap {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = u.dp(34), end = u.dp(34), top = u.dp(18), bottom = u.dp(26)),
    ) {
        Header(
            if (tab == 0) "Clases" else "Mi día",
            if (tab == 0) (semester?.name ?: "sin clases") else "lo que hiciste de verdad",
        ) { RoundBtn("✕", onClick = onClose) }
        Gap(22)
        Segmented(listOf("Clases", "Mi día"), tab) { tab = it }
        Gap(22)
        if (tab == 0) WeekGrid(state, monday, offset) { offset = it } else DayLog(state)
    }
}

@Composable
private fun ColumnScope.WeekGrid(state: AppState, monday: LocalDate, offset: Int, setOffset: (Int) -> Unit) {
    val u = LocalU.current
    val friday = monday.plusDays(4)
    val dates = (0..4).map { monday.plusDays(it.toLong()) }
    val blocks: List<Pair<LocalDate, Block>> = dates.flatMap { d -> Schedule.blocksOn(d).map { d to it } }
    val went = blocks.count { (d, b) -> b.key(d) in state.attended }

    // cambiar de semana
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundBtn("‹", size = 78) { setOffset(offset - 1) }
        Box(
            Modifier.weight(1f).padding(horizontal = u.dp(14)).height(u.dp(78)).glass(CircleShape).tap { setOffset(0) },
            contentAlignment = Alignment.Center,
        ) {
            val range = if (monday.month == friday.month) "${monday.dayOfMonth} – ${friday.dayOfMonth} ${MONTHS[friday.monthValue - 1]}"
            else "${monday.dayOfMonth} ${MONTHS[monday.monthValue - 1]} – ${friday.dayOfMonth} ${MONTHS[friday.monthValue - 1]}"
            T(if (offset == 0) "esta semana · $range" else range, 25, weight = 600, maxLines = 1)
        }
        RoundBtn("›", size = 78) { setOffset(offset + 1) }
    }
    Gap(22)

    val timeW = u.dp(72)
    // cabecera con los dias
    Row(Modifier.fillMaxWidth().padding(start = timeW)) {
        dates.forEachIndexed { i, d ->
            val isToday = d == state.today
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                T(DAYS[i], 19, color = if (isToday) C.Orange else C.Muted, weight = 600, family = Mono, ls = 2)
                T(d.dayOfMonth.toString(), 34, color = if (isToday) Color.White else Color(0xFF6E6E7C), weight = 650, ls = -1)
            }
        }
    }
    Gap(10)

    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        val slotH: Dp = maxHeight / SLOTS
        val colW: Dp = (maxWidth - timeW) / 5
        val gap = u.dp(8)

        // horas y lineas guia
        val hours = (0 until MORNING_SLOTS step 2).map { MORNING.plusMinutes(it * 30L) } +
            (0 until AFTERNOON_SLOTS step 2).map { AFTERNOON.plusMinutes(it * 30L) }
        hours.forEach { t ->
            val y = slotH * row(t)
            T(t.toString(), 16, Modifier.offset(y = y + u.dp(6)).width(timeW - u.dp(8)), color = Color(0xFF5E5E6C), weight = 500, family = Mono, maxLines = 1)
            Box(Modifier.offset(x = timeW, y = y).width(maxWidth - timeW).height(1.dp).background(Color.White.copy(alpha = .045f)))
        }

        if (blocks.isEmpty()) {
            T(
                "esta semana no hay clases", 27, Modifier.align(Alignment.Center).padding(start = timeW),
                color = C.Muted, weight = 500, align = TextAlign.Center,
            )
        }

        blocks.forEach { (date, block) ->
            val on = block.key(date) in state.attended
            Cell(
                block, on,
                Modifier
                    .offset(x = timeW + colW * (block.day - 1) + gap / 2, y = slotH * row(block.start) + gap / 2)
                    .size(colW - gap, slotH * (Block.MINUTES / 30f) - gap),
            ) { state.toggleAttended(block, date) }
        }
    }

    Gap(18)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Legend(true); T("  fui", 21, color = C.Soft, weight = 500)
            Box(Modifier.width(u.dp(26)))
            Legend(false); T("  no fui", 21, color = C.Soft, weight = 500)
        }
        if (blocks.isNotEmpty()) T("$went de ${blocks.size} h", 23, weight = 650, family = Mono)
    }
}

@Composable
private fun Legend(on: Boolean) {
    val u = LocalU.current
    val shape = RoundedCornerShape(u.dp(7))
    Box(
        Modifier.size(u.dp(24)).clip(shape).background(if (on) Color.White else Color.Black)
            .border(u.dp(1.5), Color.White.copy(alpha = if (on) 1f else .3f), shape)
    )
}

@Composable
private fun Cell(block: Block, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val u = LocalU.current
    val shape = RoundedCornerShape(u.dp(22))
    val bg by animateColorAsState(if (on) Color.White else Color.Black, label = "cellbg")
    val fg by animateColorAsState(if (on) Color(0xFF07070A) else Color.White, label = "cellfg")
    val pop by animateFloatAsState(if (on) 1f else .96f, spring(dampingRatio = .45f, stiffness = 500f), label = "cellpop")
    Column(
        modifier
            .graphicsLayer { scaleX = pop; scaleY = pop }
            .shadow(if (on) u.dp(22) else 0.dp, shape, ambientColor = Color.White, spotColor = Color.White)
            .clip(shape)
            .background(bg)
            .border(u.dp(1.5), Color.White.copy(alpha = if (on) 0f else .2f), shape)
            .tap(onClick = onClick)
            .padding(horizontal = u.dp(4)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        T(block.code, if (block.code.length > 8) 19 else 23, color = fg, weight = 800, ls = -.6, maxLines = 1)
        T(block.room, 16, color = fg.copy(alpha = .6f), weight = 600, family = Mono, maxLines = 1)
    }
}

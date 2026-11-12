package com.iagodf.wakeup.ui.clases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.Project
import com.iagodf.wakeup.data.SLOTS_PER_DAY
import com.iagodf.wakeup.data.SLOT_MIN
import com.iagodf.wakeup.data.fmtH
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.GlowCard
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.Mono
import com.iagodf.wakeup.ui.Pal
import com.iagodf.wakeup.ui.Pals
import com.iagodf.wakeup.ui.RoundBtn
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.glass
import com.iagodf.wakeup.ui.proyectos.lookColor
import com.iagodf.wakeup.ui.tap
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** pincel especial: marca que en ese tramo el movil era para el proyecto y no resta */
private const val PHONE_OK = -2L
private const val FIRST_SLOT = 14 // 07:00
private val DAY = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.forLanguageTag("es"))

/**
 * mi dia: el dia en tramos de media hora. las clases a las que fui salen rellenas,
 * el resto lo pinto yo con el proyecto en el que estuve, y cada tramo enseña
 * los minutos de movil que hubo para que no me engañe
 */
@Composable
fun ColumnScope.DayLog(state: AppState) {
    val u = LocalU.current
    var back by rememberSaveable { mutableIntStateOf(0) }
    val date = state.today.minusDays(back.toLong())
    var brush by rememberSaveable { mutableLongStateOf(state.projects.firstOrNull()?.id ?: PHONE_OK) }
    if (brush != PHONE_OK && state.projects.none { it.id == brush }) brush = state.projects.firstOrNull()?.id ?: PHONE_OK

    // cambiar de dia (solo hacia atras, el futuro no se puede apuntar)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundBtn("‹", size = 78) { back++ }
        Box(
            Modifier.weight(1f).padding(horizontal = u.dp(14)).height(u.dp(78)).glass(CircleShape).tap { back = 0 },
            contentAlignment = Alignment.Center,
        ) { T(if (back == 0) "hoy · ${date.format(DAY)}" else date.format(DAY), 25, weight = 600, maxLines = 1) }
        RoundBtn("›", size = 78) { if (back > 0) back-- }
    }

    // resumen del dia
    Gap(18)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
        Mini("REAL", fmtH(state.dayReal(date)), Pals.G, Modifier.weight(1f))
        Mini("MÓVIL", fmtH(state.phoneMinutes(date)), Pals.O, Modifier.weight(1f))
        Mini("CLASES", fmtH(state.classMinutes(date)), Pals.B, Modifier.weight(1f))
    }

    // con que pinto
    Gap(18)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
        state.projects.forEach { p -> BrushChip(p.name, lookColor(p.look), brush == p.id) { brush = p.id } }
        BrushChip("Móvil ✓", C.Orange, brush == PHONE_OK) { brush = PHONE_OK }
    }
    Gap(10)
    T(
        when {
            state.projects.isEmpty() -> "crea un proyecto para poder apuntar"
            brush == PHONE_OK -> "toca un tramo apuntado si el móvil era para el proyecto: no le resta"
            else -> "toca los tramos en los que estuviste con esto. otra vez para borrar"
        },
        20, color = C.Muted, weight = 400, maxLines = 1,
    )
    Gap(12)

    // empieza a las 07:00, o antes si hubo algo antes
    val first = (0 until FIRST_SLOT).firstOrNull {
        state.slotPhone(date, it) > 0 || state.logged(date, it) != null || state.classAt(date, it) != null
    } ?: FIRST_SLOT
    val scroll = rememberScrollState()
    val rowPx = with(LocalDensity.current) { u.dp(72).toPx() }
    LaunchedEffect(date) {
        // hoy: bajar hasta la hora que es
        val now = if (back == 0) (state.now.hour * 60 + state.now.minute) / SLOT_MIN else first
        scroll.scrollTo(((now - 3 - first).coerceAtLeast(0) * rowPx).toInt())
    }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll)) {
        for (slot in first until SLOTS_PER_DAY) {
            SlotRow(state, date, slot, brush)
            Spacer(Modifier.height(u.dp(6)))
        }
        Gap(30)
    }
}

@Composable
private fun Mini(label: String, value: String, pal: Pal, modifier: Modifier) {
    val u = LocalU.current
    GlowCard(pal, modifier.height(u.dp(128)), radius = 30, cy = 1.25f) {
        Column(Modifier.padding(horizontal = u.dp(22), vertical = u.dp(18))) {
            T(label, 17, color = C.Label, weight = 600, ls = 3, maxLines = 1)
            T(value, 42, weight = 650, ls = -1.5, lh = 1.15f, maxLines = 1)
        }
    }
}

@Composable
private fun BrushChip(text: String, color: Color, on: Boolean, onClick: () -> Unit) {
    val u = LocalU.current
    Row(
        Modifier
            .height(u.dp(68))
            .then(if (on) Modifier.clip(CircleShape).background(Color.White) else Modifier.glass(CircleShape))
            .tap(onClick = onClick)
            .padding(horizontal = u.dp(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(u.dp(18)).clip(CircleShape).background(color))
        Spacer(Modifier.width(u.dp(12)))
        T(text, 23, color = if (on) Color(0xFF0A0A0F) else Color.White, weight = 650, maxLines = 1)
    }
}

@Composable
private fun SlotRow(state: AppState, date: LocalDate, slot: Int, brush: Long) {
    val u = LocalU.current
    val cls = state.classAt(date, slot)
    val pid = state.logged(date, slot)
    val project: Project? = state.projects.firstOrNull { it.id == pid }
    val phone = state.slotPhone(date, slot)
    val elapsed = state.slotElapsed(date, slot)
    val current = date == state.today && elapsed in 1 until SLOT_MIN
    val shape = RoundedCornerShape(u.dp(18))
    val time = "%02d:%02d".format(slot * SLOT_MIN / 60, slot * SLOT_MIN % 60)

    Row(Modifier.fillMaxWidth().height(u.dp(66)), verticalAlignment = Alignment.CenterVertically) {
        T(
            time, 18, Modifier.width(u.dp(84)),
            color = when { current -> C.Orange; slot % 2 == 0 -> C.Muted; else -> Color(0xFF55555F) },
            weight = if (current) 700 else 500, family = Mono, maxLines = 1,
        )
        val cell = Modifier.weight(1f).fillMaxHeight().clip(shape)
        when {
            // clase: ya viene rellena, no se toca aqui
            cls != null -> {
                val went = cls.key(date) in state.attended
                Row(
                    cell.background(if (went) Color.White else Color.Black)
                        .border(u.dp(1.5), Color.White.copy(alpha = if (went) 0f else .2f), shape)
                        .padding(horizontal = u.dp(22)),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    val fg = if (went) Color(0xFF07070A) else Color.White.copy(alpha = .6f)
                    T("${cls.code} · ${cls.room}", 23, Modifier.weight(1f), color = fg, weight = 800, maxLines = 1)
                    when {
                        !went -> T("luego", 19, color = fg, weight = 500)
                        phone > 0 -> T("$phone min móvil", 19, color = Color(0xFFB3261E), weight = 700)
                    }
                }
            }
            // apuntado a un proyecto: la parte de movil sale oscurecida
            project != null -> {
                val color = lookColor(project.look)
                val ok = state.isPhoneOk(date, slot)
                val lost = if (ok || elapsed == 0) 0 else minOf(phone, elapsed)
                Box(
                    cell.background(Brush.horizontalGradient(listOf(color, color.copy(alpha = .72f))))
                        .tap { if (brush == PHONE_OK) state.togglePhoneOk(date, slot) else state.setSlot(date, slot, if (pid == brush) null else brush) },
                ) {
                    if (lost > 0) Box(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(lost / SLOT_MIN.toFloat()).background(Color(0xB8050508))
                    )
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = u.dp(22)),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T(project.name, 24, Modifier.weight(1f), weight = 750, maxLines = 1)
                        T(
                            when {
                                elapsed == 0 -> ""
                                ok -> "${state.slotReal(date, slot)} min · móvil ✓"
                                lost > 0 -> "${state.slotReal(date, slot)} min · −$lost móvil"
                                else -> "${state.slotReal(date, slot)} min"
                            },
                            19, weight = 650, maxLines = 1,
                        )
                    }
                }
            }
            // hueco sin apuntar: si hubo movil se ve cuanto
            else -> {
                Box(
                    cell.background(C.Panel)
                        .border(u.dp(1.5), Color.White.copy(alpha = .06f), shape)
                        .tap { if (brush != PHONE_OK) state.setSlot(date, slot, brush) },
                ) {
                    if (phone > 0) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(phone / SLOT_MIN.toFloat()).background(C.Orange.copy(alpha = .22f)))
                        T("$phone min de móvil", 20, Modifier.align(Alignment.CenterStart).padding(start = u.dp(22)), color = C.OrangeSoft, weight = 600, maxLines = 1)
                    }
                }
            }
        }
    }
}

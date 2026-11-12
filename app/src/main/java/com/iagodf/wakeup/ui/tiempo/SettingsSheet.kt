package com.iagodf.wakeup.ui.tiempo

import android.content.Context
import android.content.Intent
import android.provider.Settings as SysSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.Lang
import com.iagodf.wakeup.data.tr
import com.iagodf.wakeup.widget.WakeWidget
import com.iagodf.wakeup.data.fmtH
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Gap
import com.iagodf.wakeup.ui.Kicker
import com.iagodf.wakeup.ui.Pals
import com.iagodf.wakeup.ui.PillButton
import com.iagodf.wakeup.ui.Sheet
import com.iagodf.wakeup.ui.SheetTitle
import com.iagodf.wakeup.ui.Stepper
import com.iagodf.wakeup.ui.T
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.glass
import com.iagodf.wakeup.ui.tap
import com.iagodf.wakeup.widget.Scene

fun openUsageAccess(ctx: Context) {
    ctx.startActivity(Intent(SysSettings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun h(v: Float) = fmtH((v * 60).toInt())

@Composable
fun SettingsSheet(state: AppState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val s = state.settings
    Sheet(onClose) {
        SheetTitle(tr("Ajustes", "Settings"), tr("tu día, tu vida y el widget", "your day, your life and the widget"))
        Kicker(tr("Idioma", "Language"))
        Gap(12)
        Segmented(listOf("English", "Español"), if (Lang.en) 0 else 1) {
            Lang.set(ctx, it == 0)
            WakeWidget.render(ctx.applicationContext, state)
        }
        Gap(34)
        Kicker(tr("Tu día", "Your day"))
        T(tr("lo que no es tiempo libre", "what isn't free time"), 21, color = C.Muted, weight = 400)
        Gap(4)
        Stepper(tr("Sueño", "Sleep"), h(s.sleepH),
            onMinus = { state.update(s.copy(sleepH = (s.sleepH - .5f).coerceAtLeast(3f))) },
            onPlus = { state.update(s.copy(sleepH = (s.sleepH + .5f).coerceAtMost(12f))) })
        Stepper(tr("Tareas básicas", "Chores"), h(s.basicsH), tr("ducha, transporte, recados", "shower, commute, errands"),
            onMinus = { state.update(s.copy(basicsH = (s.basicsH - .5f).coerceAtLeast(0f))) },
            onPlus = { state.update(s.copy(basicsH = (s.basicsH + .5f).coerceAtMost(8f))) })
        Stepper(tr("Comidas", "Meals"), h(s.mealsH),
            onMinus = { state.update(s.copy(mealsH = (s.mealsH - .5f).coerceAtLeast(0f))) },
            onPlus = { state.update(s.copy(mealsH = (s.mealsH + .5f).coerceAtMost(6f))) })
        Gap(10)
        T(tr("un día sin clases te deja ${fmtH(24 * 60 - s.busyMinutes)} libres", "a day without classes leaves you ${fmtH(24 * 60 - s.busyMinutes)} free"), 23, color = C.OrangeSoft, weight = 500)

        Gap(34)
        Kicker(tr("Tu vida", "Your life"))
        Stepper(tr("Año de nacimiento", "Birth year"), s.birthYear.toString(), tr("para calcular los años de pantalla", "to work out your years of screen"),
            onMinus = { state.update(s.copy(birthYear = (s.birthYear - 1).coerceAtLeast(1940))) },
            onPlus = { state.update(s.copy(birthYear = (s.birthYear + 1).coerceAtMost(2020))) })

        Gap(34)
        Kicker("Widget")
        Gap(14)
        WidgetPreview(state)

        if (!state.hasAccess) {
            Gap(30)
            PillButton(
                tr("Dar acceso al uso del móvil", "Give usage access"),
                brush = Brush.linearGradient(listOf(Pals.O.c1, Pals.O.c2)),
                color = Color.White, glow = Pals.O.c1,
            ) { openUsageAccess(ctx) }
        }
        Gap(20)
    }
}

private val MOMENTS: List<Pair<String, Float>>
    get() = listOf(tr("Amanecer", "Sunrise") to 7.5f, tr("Día", "Day") to 13f, tr("Atardecer", "Sunset") to 19.6f, tr("Noche", "Night") to 23f)

/** como se ve el widget ahora mismo, y en cada momento del dia */
@Composable
private fun WidgetPreview(state: AppState) {
    val u = LocalU.current
    val ctx = LocalContext.current
    var moment by remember { mutableIntStateOf(-1) }
    val now = state.now.hour + state.now.minute / 60f
    val hour = if (moment < 0) now else MOMENTS[moment].second
    val pct = state.pct(state.today)
    val min = state.phoneMinutes(state.today)
    // el grande (4x2) y el pequeño (2x2)
    val big = remember(hour, pct, min, Lang.en) { Scene.render(ctx, 820, 400, pct, min, hour).asImageBitmap() }
    val small = remember(hour, pct, min, Lang.en) { Scene.render(ctx, 400, 400, pct, min, hour).asImageBitmap() }
    Row(Modifier.fillMaxWidth().height(u.dp(214)), horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
        Image(big, null, Modifier.weight(820f).fillMaxHeight(), contentScale = ContentScale.FillBounds)
        Image(small, null, Modifier.weight(400f).fillMaxHeight(), contentScale = ContentScale.FillBounds)
    }
    Gap(14)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
        MOMENTS.forEachIndexed { i, m ->
            val on = moment == i
            Box(
                Modifier.weight(1f).height(u.dp(64)).glass(CircleShape, Color.White.copy(alpha = if (on) .2f else .07f)).tap { moment = if (on) -1 else i },
                contentAlignment = Alignment.Center,
            ) { T(m.first, 20, color = if (on) Color.White else C.Soft, weight = 600, maxLines = 1) }
        }
    }
    Gap(12)
    T(tr("añádelo desde la pantalla de inicio: mantén pulsado > widgets > wake up. hay uno grande y uno pequeño", "add it from your home screen: long press > widgets > wake up. there is a big one and a small one"), 21, color = C.Muted, weight = 400, lh = 1.35f)
}

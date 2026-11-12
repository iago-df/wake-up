package com.iagodf.wakeup.ui.tiempo

import android.content.Context
import android.content.Intent
import android.provider.Settings as SysSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.iagodf.wakeup.data.AppState
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

fun openUsageAccess(ctx: Context) {
    ctx.startActivity(Intent(SysSettings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun h(v: Float) = fmtH((v * 60).toInt())

@Composable
fun SettingsSheet(state: AppState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val s = state.settings
    Sheet(onClose) {
        SheetTitle("Tu día", "lo que no es tiempo libre")
        Stepper("Sueño", h(s.sleepH),
            onMinus = { state.update(s.copy(sleepH = (s.sleepH - .5f).coerceAtLeast(3f))) },
            onPlus = { state.update(s.copy(sleepH = (s.sleepH + .5f).coerceAtMost(12f))) })
        Stepper("Tareas básicas", h(s.basicsH), "ducha, transporte, recados",
            onMinus = { state.update(s.copy(basicsH = (s.basicsH - .5f).coerceAtLeast(0f))) },
            onPlus = { state.update(s.copy(basicsH = (s.basicsH + .5f).coerceAtMost(8f))) })
        Stepper("Comidas", h(s.mealsH),
            onMinus = { state.update(s.copy(mealsH = (s.mealsH - .5f).coerceAtLeast(0f))) },
            onPlus = { state.update(s.copy(mealsH = (s.mealsH + .5f).coerceAtMost(6f))) })
        Gap(10)
        T("un día sin clases te deja ${fmtH(24 * 60 - s.busyMinutes)} libres", 23, color = C.OrangeSoft, weight = 500)

        Gap(34)
        Kicker("Tu vida")
        Stepper("Año de nacimiento", s.birthYear.toString(), "para calcular los años de pantalla",
            onMinus = { state.update(s.copy(birthYear = (s.birthYear - 1).coerceAtLeast(1940))) },
            onPlus = { state.update(s.copy(birthYear = (s.birthYear + 1).coerceAtMost(2020))) })

        if (!state.hasAccess) {
            Gap(30)
            PillButton(
                "Dar acceso al uso del móvil",
                brush = Brush.linearGradient(listOf(Pals.O.c1, Pals.O.c2)),
                color = Color.White, glow = Pals.O.c1,
            ) { openUsageAccess(ctx) }
        }
        Gap(20)
    }
}

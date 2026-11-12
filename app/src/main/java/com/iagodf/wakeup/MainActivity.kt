package com.iagodf.wakeup

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.iagodf.wakeup.data.AppState
import com.iagodf.wakeup.data.ClaudeModel
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.NavBar
import com.iagodf.wakeup.ui.U
import com.iagodf.wakeup.ui.claude.ClaudeScreen
import com.iagodf.wakeup.ui.clases.CalendarScreen
import com.iagodf.wakeup.ui.proyectos.ProyectosScreen
import com.iagodf.wakeup.ui.tiempo.TiempoScreen
import com.iagodf.wakeup.widget.WakeWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // la app siempre es oscura, iconos de la barra en blanco
        enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), SystemBarStyle.dark(TRANSPARENT))
        val state = AppState(this)
        setContent {
            val cfg = LocalConfiguration.current
            val fontScale = LocalDensity.current.fontScale
            val u = remember(cfg.screenWidthDp, fontScale) { U(minOf(cfg.screenWidthDp, 520) / 748f, fontScale) }
            CompositionLocalProvider(LocalU provides u) { App(state) }
        }
    }
}

@Composable
fun App(state: AppState) {
    val u = LocalU.current
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current.applicationContext
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    val claude = remember { ClaudeModel(state, scope) }

    // refrescar el uso al volver a la app y cada minuto mientras esta abierta
    LifecycleResumeEffect(Unit) {
        scope.launch { state.refresh(); WakeWidget.render(ctx, state) }
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); state.refresh() }
    }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(420)) { it * dir } + fadeIn(tween(420))) togetherWith
                    (slideOutHorizontally(tween(420)) { -it * dir } + fadeOut(tween(300)))
            },
            label = "tab",
        ) { t ->
            when (t) {
                0 -> TiempoScreen(state) { calendar = true }
                1 -> ProyectosScreen(state) { p -> claude.startFor(p); tab = 2 }
                else -> ClaudeScreen(state, claude)
            }
        }
        // degradado para que el contenido se pierda debajo de la barra
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(u.dp(260))
                .background(Brush.verticalGradient(0f to Color.Transparent, .78f to C.Bg, 1f to C.Bg))
        )
        NavBar(tab, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = u.dp(36))) { tab = it }

        AnimatedVisibility(
            calendar,
            enter = slideInVertically(tween(380)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300)),
        ) { CalendarScreen(state) { calendar = false } }
    }
}

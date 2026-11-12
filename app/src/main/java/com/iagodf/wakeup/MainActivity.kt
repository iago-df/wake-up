package com.iagodf.wakeup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.iagodf.wakeup.ui.C
import com.iagodf.wakeup.ui.Header
import com.iagodf.wakeup.ui.LocalU
import com.iagodf.wakeup.ui.NavBar
import com.iagodf.wakeup.ui.ScreenColumn
import com.iagodf.wakeup.ui.TABS
import com.iagodf.wakeup.ui.U

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val cfg = LocalConfiguration.current
            val fontScale = LocalDensity.current.fontScale
            val u = remember(cfg.screenWidthDp, fontScale) { U(minOf(cfg.screenWidthDp, 520) / 748f, fontScale) }
            CompositionLocalProvider(LocalU provides u) { App() }
        }
    }
}

@Composable
fun App() {
    val u = LocalU.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
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
            ScreenColumn { Header(TABS[t], "pronto") }
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
    }
}

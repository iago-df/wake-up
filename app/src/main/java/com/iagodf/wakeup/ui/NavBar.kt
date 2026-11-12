package com.iagodf.wakeup.ui

import com.iagodf.wakeup.data.tr

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale

val TABS: List<String> get() = listOf(tr("Tiempo", "Time"), tr("Proyectos", "Projects"), "Claude")

@Composable
fun NavBar(selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val u = LocalU.current
    Row(
        modifier
            .padding(horizontal = u.dp(92))
            .fillMaxWidth()
            .height(u.dp(108))
            .shadow(u.dp(40), CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .background(Color(0xEE1A1A20))
            .border(
                u.dp(1.5),
                Brush.verticalGradient(0f to Color.White.copy(alpha = .2f), .3f to Color.White.copy(alpha = .07f), 1f to Color.White.copy(alpha = .07f)),
                CircleShape,
            )
            .padding(u.dp(8)),
    ) {
        TABS.forEachIndexed { i, name ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color.White.copy(alpha = .14f) else Color.Transparent, label = "navbg")
            val fg by animateColorAsState(if (on) Color.White else Color(0xFF85859A), label = "navfg")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(bg)
                    .tap { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // iconos del video: circulo, cuadrado redondeado y rombo
                Canvas(Modifier.size(u.dp(24))) {
                    when (i) {
                        0 -> drawCircle(fg)
                        1 -> drawRoundRect(fg, cornerRadius = CornerRadius(size.width * .29f))
                        else -> rotate(45f) { scale(.86f) { drawRoundRect(fg, cornerRadius = CornerRadius(size.width * .2f)) } }
                    }
                }
                Spacer(Modifier.height(u.dp(7)))
                T(name, 20, color = fg, weight = 600)
            }
        }
    }
}

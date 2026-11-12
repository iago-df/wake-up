package com.iagodf.wakeup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

data class Pal(val c1: Color, val c2: Color)

object Pals {
    val O = Pal(Color(0xFFFF7A3C), Color(0xFFE0336C))
    val P = Pal(Color(0xFF8F63FF), Color(0xFF2D1FD6))
    val B = Pal(Color(0xFF3FA4FF), Color(0xFF1546D1))
    val G = Pal(Color(0xFF62FFA0), Color(0xFF0A9D6A))
    val R = Pal(Color(0xFFFF4A4A), Color(0xFFA3122B))
    val Y = Pal(Color(0xFFFFD24A), Color(0xFFFF6A2B))
    val all = listOf(O, P, B, G, R, Y)
}

/**
 * el brillo de abajo de las tarjetas: una elipse blanca -> c1 -> c2 -> nada
 * cy es donde cae el centro respecto al alto de la tarjeta (1.02 = justo por debajo)
 */
fun DrawScope.aurora(c1: Color, c2: Color, cy: Float = 1.02f, rx: Float = .75f, ry: Float = .6f, alpha: Float = 1f) {
    val radX = size.width * rx
    val radY = size.height * ry
    if (radX <= 0f || radY <= 0f) return
    val brush = Brush.radialGradient(
        0f to Color.White,
        .14f to Color.White,
        .40f to c1,
        .68f to c2,
        1f to c2.copy(alpha = 0f),
        center = Offset.Zero,
        radius = radX,
    )
    translate(size.width / 2f, size.height * cy) {
        scale(1f, radY / radX, pivot = Offset.Zero) {
            drawCircle(brush, radius = radX, center = Offset.Zero, alpha = alpha)
        }
    }
}

@Composable
fun GlowCard(
    c1: Color,
    c2: Color,
    modifier: Modifier = Modifier,
    radius: Number = 46,
    cy: Float = 1.02f,
    content: @Composable BoxScope.() -> Unit,
) {
    val u = LocalU.current
    val shape = RoundedCornerShape(u.dp(radius))
    Box(
        modifier
            .clip(shape)
            .background(C.Card)
            .drawBehind { aurora(c1, c2, cy) }
            .border(u.dp(1.5), Color.White.copy(alpha = .09f), shape),
        content = content,
    )
}

@Composable
fun GlowCard(pal: Pal, modifier: Modifier = Modifier, radius: Number = 46, cy: Float = 1.02f, content: @Composable BoxScope.() -> Unit) =
    GlowCard(pal.c1, pal.c2, modifier, radius, cy, content)

/** cristal: blanco muy suave con el canto de arriba iluminado */
@Composable
fun Modifier.glass(shape: Shape, fill: Color = Color.White.copy(alpha = .07f)): Modifier {
    val u = LocalU.current
    return this
        .clip(shape)
        .background(fill)
        .border(
            u.dp(1.5),
            Brush.verticalGradient(0f to Color.White.copy(alpha = .22f), .35f to Color.White.copy(alpha = .08f), 1f to Color.White.copy(alpha = .08f)),
            shape,
        )
}

/** panel oscuro de las listas */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val u = LocalU.current
    val shape = RoundedCornerShape(u.dp(42))
    Box(
        modifier
            .clip(shape)
            .background(C.Panel)
            .border(u.dp(1.5), Color.White.copy(alpha = .08f), shape),
        content = content,
    )
}

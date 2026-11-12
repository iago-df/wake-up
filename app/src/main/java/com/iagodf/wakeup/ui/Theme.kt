package com.iagodf.wakeup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iagodf.wakeup.R

// colores sacados del video
object C {
    val Bg = Color(0xFF030305)
    val Card = Color(0xFF09090D)
    val Panel = Color(0xFF0B0B10)
    val Muted = Color(0xFF8D8D9B)
    val Soft = Color(0xFFB0B0BE)
    val Label = Color(0xFFC5C5D2)
    val Text = Color(0xFFECECF4)
    val Orange = Color(0xFFFF6A2B)
    val OrangeSoft = Color(0xFFFF8A4D)
    val Violet = Color(0xFF8F63FF)
    val Green = Color(0xFF62FFA0)
    val Red = Color(0xFFFF4A4A)
    val Line = Color.White.copy(alpha = .07f)
}

@OptIn(ExperimentalTextApi::class)
private fun vf(res: Int, w: Int) = Font(
    res, weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

val Inter = FontFamily(listOf(400, 500, 600, 650, 700, 750, 800).map { vf(R.font.inter, it) })
val Mono = FontFamily(listOf(400, 500, 600, 700).map { vf(R.font.jbmono, it) })
val Anton = FontFamily(Font(R.font.anton))

/**
 * todo el diseño esta medido en px sobre una pantalla de 748px de ancho (la del video),
 * asi que escalo con el ancho real del movil y queda igual en cualquiera
 */
class U(val k: Float, private val fontScale: Float) {
    fun dp(px: Number): Dp = (px.toFloat() * k).dp
    fun sp(px: Number): TextUnit = (px.toFloat() * k / fontScale).sp
}

val LocalU = staticCompositionLocalOf { U(0.52f, 1f) }

@Composable
fun T(
    text: String,
    size: Number,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    weight: Int = 500,
    ls: Number = 0,
    family: FontFamily = Inter,
    lh: Float? = null,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    brush: Brush? = null,
) {
    val u = LocalU.current
    val base = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight(weight),
        fontSize = u.sp(size),
        letterSpacing = u.sp(ls),
        lineHeight = if (lh != null) u.sp(size.toFloat() * lh) else TextUnit.Unspecified,
        textAlign = align ?: TextAlign.Unspecified,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    val style = if (brush != null) base.copy(brush = brush) else base.copy(color = color)
    BasicText(
        text, modifier, style,
        maxLines = maxLines,
        softWrap = maxLines != 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** click sin ripple */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

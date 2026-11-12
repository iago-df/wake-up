package com.iagodf.wakeup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign

/** fila con - valor + */
@Composable
fun Stepper(label: String, value: String, sub: String? = null, onMinus: () -> Unit, onPlus: () -> Unit) {
    val u = LocalU.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = u.dp(15)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            T(label, 28, weight = 500)
            if (sub != null) T(sub, 21, color = C.Muted, weight = 400)
        }
        RoundBtn("−", size = 66, onClick = onMinus)
        T(value, 32, Modifier.width(u.dp(132)), weight = 650, align = TextAlign.Center, maxLines = 1)
        RoundBtn("+", size = 66, onClick = onPlus)
    }
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    brush: Brush = Brush.linearGradient(listOf(Color.White, Color.White)),
    color: Color = Color(0xFF0A0A0F),
    glow: Color? = null,
    onClick: () -> Unit,
) {
    val u = LocalU.current
    Box(
        modifier
            .fillMaxWidth()
            .height(u.dp(96))
            .drawBehind {
                if (glow != null) drawCircle(
                    Brush.radialGradient(listOf(glow.copy(alpha = .45f), Color.Transparent), radius = size.width * .6f),
                    radius = size.width * .6f,
                )
            }
            .clip(CircleShape)
            .background(brush)
            .tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, 27, color = color, weight = 650, maxLines = 1) }
}

/** campo de texto con el estilo de cristal */
@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    size: Number = 27,
    family: FontFamily = if (mono) Mono else Inter,
) {
    val u = LocalU.current
    Box(
        modifier.fillMaxWidth().height(u.dp(92)).glass(CircleShape).padding(horizontal = u.dp(34)),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) T(hint, size, color = C.Muted, weight = 500, family = family, maxLines = 1)
        BasicTextField(
            value, onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontFamily = family, fontWeight = FontWeight(500), fontSize = u.sp(size)),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = false),
            visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
        )
    }
}

@Composable
fun SheetTitle(title: String, sub: String? = null) {
    val u = LocalU.current
    Column(Modifier.padding(top = u.dp(30), bottom = u.dp(14))) {
        T(title, 46, weight = 650, ls = -1.5)
        if (sub != null) T(sub, 23, color = C.Muted, weight = 500)
    }
}

@Composable
fun Gap(px: Number) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(LocalU.current.dp(px)))
}

@Composable
fun SpaceRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { content() }

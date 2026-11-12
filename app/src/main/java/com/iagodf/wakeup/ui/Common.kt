package com.iagodf.wakeup.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** cabecera de cada apartado: titulo grande, subtitulo gris y algo a la derecha */
@Composable
fun Header(title: String, sub: String, right: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(title, 54, weight = 650, ls = -2)
            T(sub, 23, color = C.Muted, weight = 500, maxLines = 1)
        }
        right()
    }
}

@Composable
fun RoundBtn(label: String, modifier: Modifier = Modifier, size: Number = 74, onClick: () -> Unit) {
    val u = LocalU.current
    Box(
        modifier.size(u.dp(size)).glass(CircleShape).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(label, 30, color = Color(0xFFCFCFDA), weight = 500) }
}

/** columna con scroll y los margenes del video (34 a los lados, hueco abajo para la barra) */
@Composable
fun ScreenColumn(bottom: Number = 240, content: @Composable ColumnScope.() -> Unit) {
    val u = LocalU.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = u.dp(34), end = u.dp(34), top = u.dp(18), bottom = u.dp(bottom))
            .navigationBarsPadding(),
        content = content,
    )
}

/** titulo pequeño en mayusculas de los paneles */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = C.Muted) =
    T(text.uppercase(), 21, modifier, color = color, weight = 600, ls = 5, maxLines = 1)

/** hoja que sube desde abajo, para ajustes y detalles. se cierra tocando fuera o arrastrandola hacia abajo */
@Composable
fun Sheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val u = LocalU.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val p = remember { Animatable(0f) }
        LaunchedEffect(Unit) { p.animateTo(1f, spring(dampingRatio = .82f, stiffness = 380f)) }

        // cuanto se ha arrastrado hacia abajo, en px
        var drag by remember { mutableFloatStateOf(0f) }
        var height by remember { mutableIntStateOf(0) }
        val close by rememberUpdatedState(onClose)
        val conn = remember {
            object : NestedScrollConnection {
                // al subir el dedo primero se recoloca la hoja y luego ya hace scroll el contenido
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (drag > 0f && available.y < 0f) {
                        val used = maxOf(available.y, -drag)
                        drag += used
                        return Offset(0f, used)
                    }
                    return Offset.Zero
                }

                // lo que sobra al tirar hacia abajo (contenido ya arriba del todo) mueve la hoja
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f && source == NestedScrollSource.UserInput) {
                        drag += available.y
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (drag <= 0f) return Velocity.Zero
                    if (drag > height * .22f || available.y > 1400f) {
                        animate(drag, height.toFloat(), animationSpec = tween(180)) { v, _ -> drag = v }
                        close()
                    } else {
                        animate(drag, 0f, animationSpec = spring(dampingRatio = .8f, stiffness = 420f)) { v, _ -> drag = v }
                    }
                    return available
                }
            }
        }

        Box(Modifier.fillMaxSize().tap(onClick = onClose).imePadding(), contentAlignment = Alignment.BottomCenter) {
            val shape = RoundedCornerShape(topStart = u.dp(56), topEnd = u.dp(56))
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { height = it.height }
                    .graphicsLayer { translationY = (1f - p.value) * size.height + drag; alpha = p.value.coerceIn(0f, 1f) }
                    .clip(shape)
                    .background(Color(0xFF0D0D13))
                    .border(u.dp(1.5), Color.White.copy(alpha = .1f), shape)
                    .tap {}
                    .navigationBarsPadding()
                    .nestedScroll(conn)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = u.dp(40), vertical = u.dp(36)),
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(u.dp(84), u.dp(8)).clip(CircleShape).background(Color.White.copy(alpha = .18f))
                )
                content()
            }
        }
    }
}

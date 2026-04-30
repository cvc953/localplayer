package com.cvc953.localplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
/*
 * Componente reutilizable que proporciona funcionalidad de deslizar horizontalmente.
 *
 * Cuando el usuario desliza el contenido más allá del umbral hacia la derecha, se dispara el callback [onSwipeThreshold].
 * Cuando el usuario desliza el contenido más allá del umbral hacia la izquierda, se dispara el callback [onSwipeLeftThreshold].
 * El componente mostrará un ícono y animará el contenido suavemente.
 *
 */
@Composable
fun DraggableSwipeRow(
    onSwipeThreshold: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeLeftThreshold: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val dragOffsetX = remember { Animatable(0f) }
    val itemScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val maxOffsetPx =
        if (rowWidthPx > 0) {
            rowWidthPx.toFloat()
        } else {
            with(density) { 120.dp.toPx() }
        }

    val thresholdPx =
        if (rowWidthPx > 0) {
            (rowWidthPx * 0.4f)
        } else {
            with(density) { 72.dp.toPx() }
        }

    val dragState =
        rememberDraggableState { delta ->
            itemScope.launch {
                dragOffsetX.snapTo(
                    (dragOffsetX.value + delta).coerceIn(
                        -maxOffsetPx,
                        maxOffsetPx,
                    ),
                )
            }
        }

    val progress =
        (dragOffsetX.value / maxOffsetPx).coerceIn(-1f, 1f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .onSizeChanged {
                    rowWidthPx = it.width
                }.draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        itemScope.launch {
                            // Drag hacia la derecha
                            if (dragOffsetX.value > thresholdPx) {
                                onSwipeThreshold()
                                dragOffsetX.animateTo(
                                    maxOffsetPx,
                                    animationSpec = tween(100),
                                )
                                dragOffsetX.snapTo(0f)
                            }
                            // Drag hacia la izquierda
                            else if (dragOffsetX.value < -thresholdPx && onSwipeLeftThreshold != null) {
                                onSwipeLeftThreshold()
                                dragOffsetX.animateTo(
                                    -maxOffsetPx,
                                    animationSpec = tween(100),
                                )
                                dragOffsetX.snapTo(0f)
                            }
                            // Vuelve a la posición inicial
                            else {
                                dragOffsetX.animateTo(
                                    0f,
                                    animationSpec = tween(100),
                                )
                            }
                        }
                    },
                ),
    ) {
        // Fondo con ícono que se muestra cuando el usuario arrastra
        if (progress != 0f) {
            val isRightDrag = dragOffsetX.value > 0
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                contentAlignment = if (isRightDrag) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                val iconWidth = with(density) { 24.dp.toPx() }
                val spacerWidth = with(density) { 40.dp.toPx() }
                val iconTriggerOffset = iconWidth + spacerWidth
                val iconOffsetPx =
                    if (kotlin.math.abs(dragOffsetX.value) > iconTriggerOffset) {
                        kotlin.math.abs(dragOffsetX.value) - iconTriggerOffset
                    } else {
                        0f
                    }
                val iconOffsetDp = with(density) { iconOffsetPx.toDp() }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .background(
                                if (isRightDrag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            ).padding(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isRightDrag) Arrangement.Start else Arrangement.End,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .offset(x = if (isRightDrag) iconOffsetDp else -iconOffsetDp),
                    ) {
                        Icon(
                            imageVector = if (isRightDrag) Icons.AutoMirrored.Filled.QueueMusic else Icons.Filled.PlaylistAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        // Contenido que se desplaza
        val offsetDp = with(density) { dragOffsetX.value.toDp() }
        Box(
            modifier =
                Modifier.offset(
                    x = offsetDp,
                ),
        ) {
            content()
        }
    }
}

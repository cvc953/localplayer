package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("ktlint:standard:function-naming")
/*
 * Simple custom vertical slider with rounded track and circular thumb.
 * valueRange is expected to be floats (e.g. -1500f..1500f).
 */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 2.dp,
    thumbRadius: Dp = 10.dp,
    activeColor: Color = Color(0xFFB58CFF),
    inactiveColor: Color = Color(0xFFFFFFFF),
    backgroundColor: Color = Color.Transparent,
) {
    val range = remember(valueRange) { valueRange.endInclusive - valueRange.start }
    val layoutHeight = remember { mutableFloatStateOf(0f) }

    Box(
        modifier =
            modifier
                .onSizeChanged { layoutHeight.value = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val h = layoutHeight.value.takeIf { it > 0f } ?: return@detectDragGestures
                            val y = pos.y.coerceIn(0f, h)
                            val norm = 1f - (y / h)
                            val newValue = valueRange.start + norm * range
                            onValueChange(newValue)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val h = layoutHeight.value.takeIf { it > 0f } ?: return@detectDragGestures
                            val y = change.position.y.coerceIn(0f, h)
                            val norm = 1f - (y / h)
                            val newValue = valueRange.start + norm * range
                            onValueChange(newValue)
                        },
                    )
                },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawVerticalSlider(this, value, valueRange, trackWidth.toPx(), thumbRadius.toPx(), activeColor, inactiveColor, backgroundColor)
        }
    }
}

private fun drawVerticalSlider(
    scope: DrawScope,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    trackPx: Float,
    thumbPx: Float,
    activeColor: Color,
    inactiveColor: Color,
    backgroundColor: Color,
) {
    with(scope) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val trackLeft = centerX - trackPx / 2f
        val pad = 6f
        val trackTop = pad
        val trackBottom = h - pad
        val trackHeight = trackBottom - trackTop

        val range = valueRange.endInclusive - valueRange.start
        val norm = ((value - valueRange.start) / range).coerceIn(0f, 1f)
        val thumbY = trackTop + (1f - norm) * trackHeight

        // inactive track (thin)
        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.25f),
            topLeft = Offset(trackLeft, trackTop),
            size = Size(trackPx, trackHeight),
            cornerRadius =
                CornerRadius(trackPx / 2f, trackPx / 2f),
        )

        // active track (from thumb to bottom), draw thin overlay
        val activeTop = thumbY
        val activeHeight = trackBottom - activeTop
        drawRoundRect(
            color = activeColor.copy(alpha = 0.95f),
            topLeft = Offset(trackLeft, activeTop),
            size = Size(trackPx, activeHeight),
            cornerRadius =
                CornerRadius(trackPx / 2f, trackPx / 2f),
        )

        // subtle tick dots along track (small)
        val dots = 5
        for (i in 0 until dots) {
            val y = trackTop + i * (trackHeight / (dots - 1))
            drawCircle(color = inactiveColor.copy(alpha = 0.08f), radius = 1.5f, center = Offset(centerX, y))
        }

        // thumb (slightly larger than track, but still slim)
        drawCircle(color = activeColor, radius = thumbPx, center = Offset(centerX, thumbY))
        // subtle highlight
        drawCircle(color = activeColor.copy(alpha = 0.18f), radius = thumbPx * 0.5f, center = Offset(centerX, thumbY))
    }
}

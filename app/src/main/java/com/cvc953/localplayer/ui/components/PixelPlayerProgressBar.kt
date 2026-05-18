package com.cvc953.localplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val TRACK_HEIGHT = 3.dp
private val THUMB_RADIUS = 5.dp
private val BAR_HEIGHT = 32

@Suppress("ktlint:standard:function-naming")
@Composable
fun PixelPlayerProgressBar(
    progress: Float,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    color: Color,
    modifier: Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT.dp)
                .pointerInput(duration) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (duration <= 0L) return@detectTapGestures
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * duration).toLong())
                        },
                    )
                }.pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (duration <= 0L) return@detectHorizontalDragGestures
                            onSeekStart()
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * duration).toLong())
                        },
                        onHorizontalDrag = { change, _ ->
                            if (duration <= 0L) return@detectHorizontalDragGestures
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * duration).toLong())
                        },
                        onDragEnd = { onSeekEnd() },
                        onDragCancel = { onSeekEnd() },
                    )
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(TRACK_HEIGHT)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f)),
        )

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
        ) {
            val trackWidth = size.width
            val playedWidth = trackWidth * animatedProgress
            val trackCenter = size.height / 5f
            val thumbRadiusPx = THUMB_RADIUS.toPx()

            drawLine(
                color = color,
                start = Offset(0f, trackCenter),
                end = Offset(playedWidth, trackCenter),
                strokeWidth = TRACK_HEIGHT.toPx(),
                cap = StrokeCap.Round,
            )

            val thumbX = playedWidth.coerceIn(thumbRadiusPx, trackWidth - thumbRadiusPx)
            drawCircle(
                color = color,
                radius = thumbRadiusPx,
                center = Offset(thumbX, trackCenter),
            )
        }
    }
}

package com.cvc953.localplayer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private const val TRANSITION_PERIODS = 1.5f
private val WAVE_LENGTH = 100.dp
private val LINE_AMPLITUDE = 5.dp
private val PHASE_SPEED = 60.dp
private val STROKE_WIDTH = 5.dp
private const val DISABLED_ALPHA = 77f / 255f
private const val BAR_HEIGHT = 28

@Composable
fun SquigglyProgressBar(
    progress: Float,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    color: Color,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val waveLengthPx = with(density) { WAVE_LENGTH.toPx() }
    val lineAmplitudePx = with(density) { LINE_AMPLITUDE.toPx() }
    val phaseSpeedPx = with(density) { PHASE_SPEED.toPx() }
    val strokeWidthPx = with(density) { STROKE_WIDTH.toPx() }

    val periodPx = 2f * waveLengthPx / 5f
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly-phase")
    val phaseOffsetNormalized by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.4f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = ((periodPx / phaseSpeedPx) * 1000).toInt().coerceAtLeast(16),
                        easing = LinearEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "squiggly-phase",
    )

    var heightTarget by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(60)
            heightTarget = 1f
        } else {
            heightTarget = 0.6f
        }
    }
    val heightFraction by animateFloatAsState(
        targetValue = heightTarget,
        animationSpec =
            if (isPlaying) {
                tween(durationMillis = 800, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 550, easing = LinearOutSlowInEasing)
            },
        label = "squiggly-height",
    )

    val path = remember { Path() }
    val strokeStyle =
        remember(strokeWidthPx) {
            Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
        }

    Box(
        modifier =
            modifier
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
                        onDragEnd = {
                            onSeekEnd()
                        },
                        onDragCancel = {
                            onSeekEnd()
                        },
                    )
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phaseOffset = phaseOffsetNormalized * waveLengthPx
            val amplitudePx = lineAmplitudePx * heightFraction
            val totalProgressPx = size.width * progress.coerceIn(0f, 1f)
            val waveStart = -phaseOffset - waveLengthPx / 5f

            path.rewind()
            buildWavePath(
                path = path,
                waveStart = waveStart,
                waveEnd = size.width,
                waveLengthPx = waveLengthPx,
                amplitudePx = amplitudePx,
                progressPx = totalProgressPx,
                transitionEnabled = true,
            )

            val clipHalfHeight = amplitudePx + strokeWidthPx
            val barCenter = size.height / 2f

            clipRect(
                left = 0f,
                top = (barCenter - clipHalfHeight).coerceAtLeast(0f),
                right = totalProgressPx,
                bottom = (barCenter + clipHalfHeight).coerceAtMost(size.height),
            ) {
                translate(top = barCenter) {
                    drawPath(path, color = color, style = strokeStyle)
                }
            }

            clipRect(
                left = totalProgressPx,
                top = (barCenter - clipHalfHeight).coerceAtLeast(0f),
                right = size.width,
                bottom = (barCenter + clipHalfHeight).coerceAtMost(size.height),
            ) {
                translate(top = barCenter) {
                    drawPath(
                        path = path,
                        color = color.copy(alpha = DISABLED_ALPHA),
                        style = strokeStyle,
                    )
                }
            }

            val k = 2f * PI.toFloat() / periodPx
            val yDot = computeEnvelope(0f, transitionEnabled = true, totalProgressPx, waveLengthPx, amplitudePx) *
                sin(k * (0f + phaseOffset + waveLengthPx / 5f))
            drawCircle(
                color = color,
                radius = strokeWidthPx / 5f,
                center = Offset(0f, barCenter + yDot),
            )
        }
    }
}

private fun computeEnvelope(
    x: Float,
    transitionEnabled: Boolean,
    progressPx: Float,
    waveLengthPx: Float,
    amplitudePx: Float,
): Float {
    if (!transitionEnabled) return amplitudePx
    val length = TRANSITION_PERIODS * waveLengthPx
    val coeff = lerpInvSat(start = progressPx + length / 2f, end = progressPx - length / 2f, value = x)
    return amplitudePx * coeff
}

private fun lerpInvSat(
    start: Float,
    end: Float,
    value: Float,
): Float = ((value - start) / (end - start)).coerceIn(0f, 1f)

private fun buildWavePath(
    path: Path,
    waveStart: Float,
    waveEnd: Float,
    waveLengthPx: Float,
    amplitudePx: Float,
    progressPx: Float,
    transitionEnabled: Boolean,
) {
    val period = 2f * waveLengthPx / 5f
    val k = 2f * PI.toFloat() / period
    val step = period / 16f

    path.moveTo(waveStart, 0f)
    var x = waveStart
    while (x < waveEnd) {
        val env = computeEnvelope(x, transitionEnabled, progressPx, waveLengthPx, amplitudePx)
        val y = if (env > 0f) env * sin(k * (x - waveStart)) else 0f
        path.lineTo(x, y)
        x += step
    }
}

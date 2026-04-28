package com.cvc953.localplayer.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.cvc953.localplayer.model.TtmlLine
import com.cvc953.localplayer.ui.LoadingDotsAnimation
import com.cvc953.localplayer.ui.LyricLine
import com.cvc953.localplayer.ui.WordByWordLine
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.viewmodel.LyricsViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import kotlin.math.abs

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricsScreen(
    lyricsViewModel: LyricsViewModel,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier.Companion,
    dominantColor: Color = Color.Companion.Black,
    useDynamicBackground: Boolean = true,
    onLineClick: (Long) -> Unit = {},
) {
    val ttml by lyricsViewModel.ttmlLyrics.collectAsState()
    val lyrics by lyricsViewModel.lyrics.collectAsState()
    val currentPosition by playbackViewModel.currentPosition.collectAsState()
    val localTtml = ttml
    if (localTtml != null) {
        TtmlLyricsView(
            lines = localTtml.lines,
            currentPosition = currentPosition,
            modifier = modifier,
            dominantColor = dominantColor,
            useDynamicBackground = useDynamicBackground,
        ) { pos ->
            try {
                playbackViewModel.seekTo(pos)
            } catch (_: Exception) {
            }
        }
    } else {
        LyricsView(
            lyrics = lyrics,
            currentPosition = currentPosition,
            modifier = modifier,
            dominantColor = dominantColor,
            useDynamicBackground = useDynamicBackground,
        ) { pos ->
            try {
                playbackViewModel.seekTo(pos)
            } catch (_: Exception) {
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricsView(
    lyrics: List<LrcLine>,
    currentPosition: Long,
    modifier: Modifier = Modifier.Companion,
    dominantColor: Color = Color.Companion.Black,
    useDynamicBackground: Boolean = true,
    onLineClick: (Long) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val forceLightForeground =
        useDynamicBackground &&
            MaterialTheme.colorScheme.background.luminance() > 0.5f
    val activeLyricColor =
        if (forceLightForeground) Color.Companion.White else MaterialTheme.colorScheme.onBackground
    /*val inactiveLyricColor =
        if (forceLightForeground) {
            Color.White.copy(alpha = 0.52f)
        } else {
            LocalExtendedColors.current.textSecondary
        }*/
    val inactiveLyricColor = Color.Companion.White.copy(alpha = 0.4f)

    val currentIndex =
        remember(lyrics, currentPosition) {
            lyrics.indexOfLast { it.timeMs <= currentPosition }
        }

    LaunchedEffect(currentIndex) {
        if (currentIndex < 0) return@LaunchedEffect
        try {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == currentIndex }

            if (!isVisible) {
                listState.scrollToItem(index = currentIndex)
            }

            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.visibleItemsInfo.size
            val itemInfo =
                layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == currentIndex } ?: return@LaunchedEffect

            // Posición actual del top del item relativa al viewport
            val itemTop = itemInfo.offset
            // Centro del item
            val itemCenter = itemTop + itemInfo.size / 2
            // Donde debe quedar el centro del item (tercio superior)
            val targetCenter = viewportHeight / 3
            // Delta real en pixels
            val delta = itemCenter - targetCenter

            listState.animateScrollBy(
                value = delta.toFloat(),
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            )
        } catch (_: Exception) {
        }
    }

    val gapThreshold = 7000L

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    if (useDynamicBackground) {
                        Brush.Companion.verticalGradient(
                            listOf(dominantColor.darken(0.5f), dominantColor.darken(0.1f)),
                        )
                    } else {
                        Brush.Companion.verticalGradient(
                            listOf(
                                Color.Companion.Transparent,
                                Color.Companion.Transparent,
                            ),
                        )
                    },
                ),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.Companion.fillMaxSize(),
        ) {
            if (lyrics.isNotEmpty()) {
                val firstLineStart = lyrics.first().timeMs
                if (firstLineStart > gapThreshold) {
                    val isIntroGapActive = currentPosition in 0 until firstLineStart
                    if (isIntroGapActive) {
                        item {
                            Box(
                                modifier =
                                    Modifier.Companion
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Companion.Center,
                            ) {
                                LoadingDotsAnimation(
                                    isVisible = true,
                                    durationMs = firstLineStart,
                                    elapsedMs = currentPosition,
                                    brightColor = activeLyricColor,
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(lyrics) { index, line ->
                val distance = abs(index - currentIndex)
                val itemAlpha by animateFloatAsState(
                    targetValue =
                        when (distance) {
                            0 -> 1f
                            1 -> 0.75f
                            2 -> 0.5f
                            else -> 0.28f
                        },
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label = "itemAlpha_$index",
                )

                Box(
                    modifier =
                        Modifier.Companion
                            .fillMaxSize()
                            .graphicsLayer { alpha = itemAlpha }
                            .clickable { onLineClick(line.timeMs) },
                ) {
                    LyricLine(
                        text = line.text,
                        active = index == currentIndex,
                        isSecondaryVoice = false,
                        activeColor = activeLyricColor,
                        inactiveColor = inactiveLyricColor,
                    )
                }

                if (index < lyrics.size - 1) {
                    val currentLineStart = line.timeMs
                    val nextLineStart = lyrics[index + 1].timeMs
                    val gapDuration = nextLineStart - currentLineStart
                    if (gapDuration > gapThreshold) {
                        val isGapActive =
                            currentPosition in currentLineStart..<nextLineStart
                        if (isGapActive) {
                            Box(
                                modifier =
                                    Modifier.Companion
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Companion.Center,
                            ) {
                                LoadingDotsAnimation(
                                    isVisible = true,
                                    durationMs = gapDuration,
                                    elapsedMs = currentPosition - currentLineStart,
                                    brightColor = activeLyricColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vista de letras con sincronización palabra por palabra para TTML
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun TtmlLyricsView(
    lines: List<TtmlLine>,
    currentPosition: Long,
    modifier: Modifier = Modifier.Companion,
    dominantColor: Color = Color.Companion.Black,
    useDynamicBackground: Boolean = true,
    onLineClick: (Long) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val forceLightForeground =
        useDynamicBackground &&
            MaterialTheme.colorScheme.background.luminance() > 0.5f
    val activeLyricColor =
        if (forceLightForeground) Color.Companion.White else MaterialTheme.colorScheme.onBackground
    /*val inactiveLyricColor =
        if (forceLightForeground) {
            Color.White.copy(alpha = 0.52f)
        } else {
            LocalExtendedColors.current.textSecondary
        }*/
    val inactiveLyricColor = Color.Companion.White.copy(alpha = 0.4f)

    val hasMultipleVoices =
        remember(lines) {
            lines.mapNotNull { it.agent }.distinct().size > 1
        }
    // Ancho maximo de las letras cuando hay multiples artistas
    val maxWidthFraction = if (hasMultipleVoices) 0.7f else 1f

    val currentIndex =
        remember(currentPosition) {
            lines.indexOfLast { it.timeMs <= currentPosition }.coerceAtLeast(0)
        }

    LaunchedEffect(currentIndex) {
        if (currentIndex < 0) return@LaunchedEffect
        try {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == currentIndex }

            if (!isVisible) {
                listState.scrollToItem(index = currentIndex)
            }

            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.visibleItemsInfo.size
            val itemInfo =
                layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == currentIndex } ?: return@LaunchedEffect

            // Posición actual del top del item relativa al viewport
            val itemTop = itemInfo.offset
            // Centro del item
            val itemCenter = itemTop + itemInfo.size / 2
            // Donde debe quedar el centro del item (tercio superior)
            val targetCenter = viewportHeight / 3
            // Delta real en pixels
            val delta = itemCenter - targetCenter

            listState.animateScrollBy(
                value = delta.toFloat(),
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            )
        } catch (_: Exception) {
        }
    }

    val gapThreshold = 7000L

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    if (useDynamicBackground) {
                        Brush.Companion.verticalGradient(
                            listOf(dominantColor.darken(0.5f), dominantColor.darken(0.1f)),
                        )
                    } else {
                        Brush.Companion.verticalGradient(
                            listOf(
                                Color.Companion.Transparent,
                                Color.Companion.Transparent,
                            ),
                        )
                    },
                ),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.Companion.fillMaxSize(),
        ) {
            if (lines.isNotEmpty()) {
                val firstLineStart = lines.first().timeMs
                if (firstLineStart > gapThreshold) {
                    val isIntroGapActive = currentPosition in 0 until firstLineStart
                    if (isIntroGapActive) {
                        item {
                            Box(
                                modifier =
                                    Modifier.Companion
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Companion.Center,
                            ) {
                                LoadingDotsAnimation(
                                    isVisible = true,
                                    durationMs = firstLineStart,
                                    elapsedMs = currentPosition,
                                    brightColor = activeLyricColor,
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(lines) { index, line ->
                val distance = abs(index - currentIndex)
                val itemAlpha by animateFloatAsState(
                    targetValue =
                        when (distance) {
                            0 -> 1f
                            1 -> 0.75f
                            2 -> 0.5f
                            else -> 0.28f
                        },
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label = "itemAlpha_$index",
                )

                Box(
                    modifier =
                        Modifier.Companion
                            .fillMaxSize()
                            .graphicsLayer { alpha = itemAlpha }
                            .clickable { onLineClick(line.timeMs) },
                ) {
                    if (line.syllabus.isNotEmpty()) {
                        WordByWordLine(
                            syllables = line.syllabus,
                            currentPosition = currentPosition,
                            isActive = index == currentIndex,
                            baseColor = inactiveLyricColor,
                            activeColor = activeLyricColor,
                            horizontalAlignment = line.alignment,
                            maxWidthFraction = maxWidthFraction,
                            modifier = Modifier.Companion.fillMaxWidth(),
                        )
                    } else {
                        LyricLine(
                            text = line.text,
                            active = index == currentIndex,
                            activeColor = activeLyricColor,
                            inactiveColor = inactiveLyricColor,
                            horizontalAlignment = line.alignment,
                            maxWidthFraction = maxWidthFraction,
                            modifier = Modifier.Companion.fillMaxWidth(),
                        )
                    }
                }

                if (index < lines.size - 1) {
                    val currentLineEnd = line.timeMs + line.durationMs
                    val nextLineStart = lines[index + 1].timeMs
                    val gapDuration = nextLineStart - currentLineEnd
                    if (gapDuration > gapThreshold) {
                        val isGapActive =
                            currentPosition in currentLineEnd..<nextLineStart
                        if (isGapActive) {
                            Box(
                                modifier =
                                    Modifier.Companion
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Companion.Center,
                            ) {
                                LoadingDotsAnimation(
                                    isVisible = true,
                                    durationMs = gapDuration,
                                    elapsedMs = currentPosition - currentLineEnd,
                                    brightColor = activeLyricColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Color.darken(factor: Float = 0.7f): Color =
    this.copy(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
    )

fun Color.luminance(factor: Float = 0.7f): Color =
    this.copy(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
    )

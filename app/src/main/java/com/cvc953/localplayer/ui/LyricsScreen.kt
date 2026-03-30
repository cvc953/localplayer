package com.cvc953.localplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.cvc953.localplayer.model.TtmlAlignment
import com.cvc953.localplayer.model.TtmlLine
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.viewmodel.LyricsViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricsScreen(
    lyricsViewModel: LyricsViewModel,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
    dominantColor: Color = Color.Black,
    useDynamicBackground: Boolean = true,
    onLineClick: (Long) -> Unit = {},
) {
    val ttml by lyricsViewModel.ttmlLyrics.collectAsState()
    val lyrics by lyricsViewModel.lyrics.collectAsState()
    val currentPosition by playbackViewModel.currentPosition.collectAsState()

    // preferir TTML cuando este disponible
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
    modifier: Modifier = Modifier,
    dominantColor: Color = Color.Black,
    useDynamicBackground: Boolean = true,
    onLineClick: (Long) -> Unit = {},
) {
    val listState = rememberLazyListState()

    val forceLightForeground = useDynamicBackground && MaterialTheme.colorScheme.background.luminance() > 0.5f
    val activeLyricColor = if (forceLightForeground) Color.White else MaterialTheme.colorScheme.onBackground
    val inactiveLyricColor =
        if (forceLightForeground) {
            Color.White.copy(alpha = 0.62f)
        } else {
            LocalExtendedColors.current.textSecondary
        }

    val currentIndex =
        remember(lyrics, currentPosition) {
            lyrics
                .indexOfLast { it.timeMs <= currentPosition }
        }

    // Scroll automático centrado
    LaunchedEffect(currentIndex) {
        if (currentIndex < 0) return@LaunchedEffect
        try {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -listState.layoutInfo.viewportSize.height / 100,
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
                        Brush.verticalGradient(
                            listOf(
                                dominantColor.darken(0.6f),
                                dominantColor.darken(0.1f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                            ),
                        )
                    },
                ),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Mostrar animación de puntos si hay un gap antes de la primera línea
            if (lyrics.isNotEmpty()) {
                val firstLineStart = lyrics.first().timeMs
                if (firstLineStart > gapThreshold) {
                    val isIntroGapActive = currentPosition in 0 until firstLineStart
                    if (isIntroGapActive) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
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
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
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

                // Detectar gap grande entre esta linea y la siguiente
                if (index < lyrics.size - 1) {
                    val currentLineStart = line.timeMs
                    val nextLineStart = lyrics[index + 1].timeMs
                    val gapDuration = nextLineStart - currentLineStart

                    // Si hay un gap > gapThreshold, mostrar animación de puntos
                    if (gapDuration > gapThreshold) {
                        // Mostrar la animacion solo si estamos dentro del gap
                        val isGapActive = currentPosition >= currentLineStart && currentPosition < nextLineStart

                        if (isGapActive) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
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
    modifier: Modifier = Modifier,
    dominantColor: Color = Color.Black,
    useDynamicBackground: Boolean = true,
    onLineClick: (Long) -> Unit = {},
) {
    val listState = rememberLazyListState()

    val forceLightForeground = useDynamicBackground && MaterialTheme.colorScheme.background.luminance() > 0.5f
    val activeLyricColor = if (forceLightForeground) Color.White else MaterialTheme.colorScheme.onBackground
    val inactiveLyricColor =
        if (forceLightForeground) {
            Color.White.copy(alpha = 0.62f)
        } else {
            LocalExtendedColors.current.textSecondary
        }

    // Detectar si hay múltiples voces (diferentes agentes)
    val hasMultipleVoices =
        remember(lines) {
            lines.mapNotNull { it.agent }.distinct().size > 1
        }
    // Porcentaje máximo de ancho: 100% si una voz, 66.6% (2/3) si múltiples voces
    val maxWidthFraction = if (hasMultipleVoices) 0.666f else 1f

    val currentIndex =
        remember(currentPosition) {
            lines
                .indexOfLast { it.timeMs <= currentPosition }
                .coerceAtLeast(0)
        }

    // Scroll automático centrado
    LaunchedEffect(currentIndex) {
        try {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -listState.layoutInfo.viewportSize.height / 100,
            )
        } catch (_: Exception) {
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    if (useDynamicBackground) {
                        Brush.verticalGradient(
                            listOf(
                                dominantColor.darken(0.6f),
                                dominantColor.darken(0.1f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                            ),
                        )
                    },
                ),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            val gapThreshold = 1500L

            if (lines.isNotEmpty()) {
                val firstLineStart = lines.first().timeMs
                if (firstLineStart > gapThreshold) {
                    item {
                        val isIntroGapActive = currentPosition in 0 until firstLineStart
                        if (isIntroGapActive) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
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
                // Si la línea tiene sílabas, mostrar sincronización palabra por palabra
                if (line.syllabus.isNotEmpty()) {
                    WordByWordLine(
                        syllables = line.syllabus,
                        currentPosition = currentPosition,
                        isActive = index == currentIndex,
                        baseColor = inactiveLyricColor,
                        activeColor = activeLyricColor,
                        horizontalAlignment = line.alignment,
                        maxWidthFraction = maxWidthFraction,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLineClick(line.timeMs) },
                    )
                } else {
                    // Fallback a línea simple
                    LyricLine(
                        text = line.text,
                        active = index == currentIndex,
                        activeColor = activeLyricColor,
                        inactiveColor = inactiveLyricColor,
                        horizontalAlignment = line.alignment,
                        maxWidthFraction = maxWidthFraction,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLineClick(line.timeMs) },
                    )
                }

                // Detectar gap grande entre esta linea y la siguiente
                if (index < lines.size - 1) {
                    val currentLineEnd = line.timeMs + line.durationMs
                    val nextLineStart = lines[index + 1].timeMs
                    val gapDuration = nextLineStart - currentLineEnd

                    // Si hay un gap > 1500ms, mostrar animación de puntos
                    if (gapDuration > gapThreshold) {
                        // Mostrar la animacion solo si estamos dentro del gap
                        val isGapActive = currentPosition >= currentLineEnd && currentPosition < nextLineStart

                        if (isGapActive) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
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

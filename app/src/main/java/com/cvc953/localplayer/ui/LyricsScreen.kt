package com.cvc953.localplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cvc953.localplayer.model.TtmlLine
import com.cvc953.localplayer.util.LrcLine

@Composable
fun LyricsView(
    lyrics: List<LrcLine>,
    currentPosition: Long,
    modifier: Modifier = Modifier,
    onLineClick: (Long) -> Unit = {}
) {
    val listState = rememberLazyListState()

    val currentIndex = remember(currentPosition) {
        lyrics.indexOfLast { it.timeMs <= currentPosition }
            .coerceAtLeast(0)
    }

    // Scroll automático centrado
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(
            index = currentIndex,
            scrollOffset = -listState.layoutInfo.viewportSize.height / 6
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0D0D),
                        Color.Black,
                        Color(0xFF0D0D0D)
                    )
                )
            )
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics) { index, line ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onLineClick(line.timeMs) }
                ) {
                    LyricLine(
                        text = line.text,
                        active = index == currentIndex
                    )
                }
            }
        }
    }
}

/**
 * Vista de letras con sincronización palabra por palabra para TTML
 */
@Composable
fun TtmlLyricsView(
    lines: List<TtmlLine>,
    currentPosition: Long,
    modifier: Modifier = Modifier,
    onLineClick: (Long) -> Unit = {}
) {
    val listState = rememberLazyListState()

    val currentIndex = remember(currentPosition) {
        lines.indexOfLast { it.timeMs <= currentPosition }
            .coerceAtLeast(0)
    }

    // Scroll automático centrado
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(
            index = currentIndex,
            scrollOffset = -listState.layoutInfo.viewportSize.height / 6
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0D0D),
                        Color.Black,
                        Color(0xFF0D0D0D)
                    )
                )
            )
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines) { index, line ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onLineClick(line.timeMs) }
                ) {
                    // Si la línea tiene sílabas, mostrar sincronización palabra por palabra
                    if (line.syllabus.isNotEmpty()) {
                        WordByWordLine(
                            syllables = line.syllabus,
                            currentPosition = currentPosition,
                            isActive = index == currentIndex
                        )
                    } else {
                        // Fallback a línea simple
                        LyricLine(
                            text = line.text,
                            active = index == currentIndex
                        )
                    }
                }

                // Detectar gap grande entre esta línea y la siguiente
                if (index < lines.size - 1) {
                    val currentLineEnd = line.timeMs + line.durationMs
                    val nextLineStart = lines[index + 1].timeMs
                    val gapDuration = nextLineStart - currentLineEnd

                    // Si hay un gap > 1500ms, mostrar animación de puntos
                    val gapThreshold = 1500L
                    if (gapDuration > gapThreshold) {
                        // Mostrar la animacion solo si estamos dentro del gap
                        val isGapActive = currentPosition >= currentLineEnd && currentPosition < nextLineStart

                        if (isGapActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingDotsAnimation(isVisible = true)
                            }
                        }
                    }
                }
            }
        }
    }
}


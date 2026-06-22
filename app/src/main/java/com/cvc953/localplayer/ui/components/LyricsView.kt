package com.cvc953.localplayer.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.cvc953.localplayer.ui.LoadingDotsAnimation
import com.cvc953.localplayer.ui.LyricLine
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.darken
import kotlin.math.abs

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

    val timedLyrics = lyrics.filter { !it.isMetadata }
    val metadataPairs = remember(lyrics) {
        lyrics.filter { it.isMetadata }.mapNotNull { line ->
            val colon = line.text.indexOf(": ")
            if (colon > 0) Pair(line.text.substring(0, colon), line.text.substring(colon + 2))
            else null
        }.distinctBy { it.first }
    }

    val currentIndex =
        remember(timedLyrics, currentPosition) {
            timedLyrics.indexOfLast { it.timeMs <= currentPosition }
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
            if (timedLyrics.isNotEmpty()) {
                val firstLineStart = timedLyrics.first().timeMs
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

            itemsIndexed(timedLyrics) { index, line ->
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
                        isSecondaryVoice = line.isSecondaryVoice,
                        activeColor = activeLyricColor,
                        inactiveColor = inactiveLyricColor,
                    )
                }

                if (index < timedLyrics.size - 1) {
                    val currentLineStart = line.timeMs
                    val nextLineStart = timedLyrics[index + 1].timeMs
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

            if (metadataPairs.isNotEmpty()) {
                item {
                    MetadataSection(
                        items = metadataPairs,
                        textColor = activeLyricColor,
                    )
                }
            }
        }
    }
}

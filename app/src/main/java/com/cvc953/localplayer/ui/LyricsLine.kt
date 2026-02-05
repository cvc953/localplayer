package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.util.LyricWord

@Composable
fun LyricLine(
        text: String,
        words: List<LyricWord> = emptyList(),
        currentPosition: Long = 0L,
        active: Boolean
) {
    val fontSize by animateFloatAsState(targetValue = if (active) 22f else 20f, label = "fontSize")

    val color by
            animateColorAsState(
                    targetValue = if (active) Color.White else Color.Gray,
                    label = "color"
            )

    val textToRender =
            if (words.isNotEmpty()) {
                buildAnnotatedString {
                    words.forEachIndexed { index, word ->
                        val isWordActive = active && currentPosition in word.startMs..word.endMs
                        val baseColor =
                                when {
                                    active -> Color(0xFF8E8E8E)
                                    else -> Color(0xFF666666)
                                }
                        val spanStyle =
                                if (isWordActive) {
                                    val duration = (word.endMs - word.startMs).coerceAtLeast(1L)
                                    val progress =
                                            ((currentPosition - word.startMs)
                                                            .coerceIn(0L, duration)
                                                            .toFloat() / duration.toFloat())
                                                    .coerceIn(0f, 1f)
                                    val brush =
                                            Brush.horizontalGradient(
                                                    colorStops =
                                                            arrayOf(
                                                                    0f to Color.White,
                                                                    progress to Color.White,
                                                                    progress to baseColor,
                                                                    1f to baseColor
                                                            )
                                            )
                                    SpanStyle(brush = brush, fontWeight = FontWeight.Bold)
                                } else {
                                    SpanStyle(color = baseColor, fontWeight = FontWeight.SemiBold)
                                }
                        withStyle(spanStyle) { append(word.text) }
                        if (index != words.lastIndex) append(" ")
                    }
                }
            } else {
                null
            }

    if (textToRender != null) {
        Text(
                text = textToRender,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize + 10).sp,
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Visible
        )
    } else {
        Text(
                text = text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                color = color,
                fontSize = fontSize.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize + 10).sp,
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Visible
        )
    }
}

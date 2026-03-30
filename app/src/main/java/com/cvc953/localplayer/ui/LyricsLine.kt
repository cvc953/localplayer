package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.TtmlAlignment

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricLine(
    text: String,
    active: Boolean,
    isSecondaryVoice: Boolean = false,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Gray,
    horizontalAlignment: TtmlAlignment = TtmlAlignment.LEFT,
    maxWidthFraction: Float = 1f,
) {
    LyricLine(
        text = text,
        active = active,
        isSecondaryVoice = isSecondaryVoice,
        modifier = Modifier,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        horizontalAlignment = horizontalAlignment,
        maxWidthFraction = maxWidthFraction,
    )
}

@Composable
fun LyricLine(
    text: String,
    active: Boolean,
    isSecondaryVoice: Boolean = false,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Transparent,
    horizontalAlignment: TtmlAlignment = TtmlAlignment.LEFT,
    maxWidthFraction: Float = 1f,
) {
    // Match TTML behavior: secondary voice is only visible on the active line.
    if (isSecondaryVoice && !active) return

    var lineCount by remember(text) { mutableStateOf(1) }

    val fontSize by animateFloatAsState(
        targetValue = if (isSecondaryVoice) 20f else 30f,
        label = "fontSize",
    )

    val textAlign =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> TextAlign.Left
            TtmlAlignment.RIGHT -> TextAlign.Right
        }

    val alignment =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> Alignment.TopStart
            TtmlAlignment.RIGHT -> Alignment.TopEnd
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(maxWidthFraction),
            contentAlignment = alignment,
        ) {
            Text(
                text = text.trimStart(),
                color = if (active) activeColor else inactiveColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
                lineHeight = if (isSecondaryVoice) (fontSize + 8).sp else (fontSize + 10).sp,
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Visible,
                onTextLayout = { result ->
                    lineCount = result.lineCount
                },
            )
        }
    }
}

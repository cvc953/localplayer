package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LyricLine(
    text: String,
    active: Boolean
) {
    var lineCount by remember(text) { mutableStateOf(1) }

    val fontSize by animateFloatAsState(
        targetValue = if (active) 30f else 28f,
        label = "fontSize"
    )

    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.35f,
        label = "alpha"
    )

    val color by animateColorAsState(
        targetValue = if (active) Color.White else Color.Gray,
        label = "color"
    )

    val textAlign = if (lineCount > 1) TextAlign.Start else TextAlign.Start

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = color,
        fontSize = fontSize.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        textAlign = textAlign,
        lineHeight = (fontSize + 10).sp,
        maxLines = Int.MAX_VALUE,
        softWrap = true,
        overflow = TextOverflow.Visible,
        onTextLayout = { result ->
            lineCount = result.lineCount
        },
        //alpha = alpha
    )
}

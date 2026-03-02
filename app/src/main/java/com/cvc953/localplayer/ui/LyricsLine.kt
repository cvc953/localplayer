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

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricLine(
    text: String,
    active: Boolean,
) {
    LyricLine(text = text, active = active, modifier = Modifier)
}

@Composable
fun LyricLine(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    var lineCount by remember(text) { mutableStateOf(1) }

    val fontSize by animateFloatAsState(
        targetValue = 30f,
        label = "fontSize",
    )

    val textAlign = TextAlign.Start

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = text.trimStart(),
            color = if (active) Color.White else Color.Gray,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left,
            lineHeight = (fontSize + 10).sp,
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Visible,
            onTextLayout = { result ->
                lineCount = result.lineCount
            },
        )
    }
}

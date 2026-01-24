package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val fontSize by animateFloatAsState(
        targetValue = if (active) 22f else 20f,
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

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = color,
        fontSize = fontSize.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        lineHeight = (fontSize + 10).sp,
        maxLines = Int.MAX_VALUE,
        softWrap = true,
        overflow = TextOverflow.Visible,
        //alpha = alpha
    )
}

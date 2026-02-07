package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
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

    val textAlign = TextAlign.Start

    // Puntos animados a la izquierda
    val dotCount = 4
    val baseSize = 8f
    val maxSize = 18f
    val colorSteps = listOf(
        Color(0xFF888888), // gris oscuro
        Color(0xFFAAAAAA), // gris medio
        Color(0xFFCCCCCC), // gris claro
        Color.White        // blanco
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Animar los puntos
        for (i in 0 until dotCount) {
            val dotProgress = if (active) (i + 1).toFloat() / dotCount else 0f
            val dotSize by animateFloatAsState(
                targetValue = baseSize + (maxSize - baseSize) * dotProgress,
                label = "dotSize$i"
            )
            val dotColor by animateColorAsState(
                targetValue = if (active) colorSteps[i] else colorSteps[0],
                label = "dotColor$i"
            )
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .graphicsLayer {
                        alpha = if (active) 1f else 0.5f
                    }
                    .padding(end = 2.dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth()) {
                    drawCircle(color = dotColor)
                }
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            color = if (active) Color.White else Color.Gray,
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
        )
    }
}

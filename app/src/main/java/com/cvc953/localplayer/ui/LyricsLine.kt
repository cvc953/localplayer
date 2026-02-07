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

    // Animación de tres puntos para instrumental
    if (text.trim() == "...") {
        val dotCount = 3
        val baseSize = 10f
        val maxSize = 20f
        val colorSteps = listOf(
            Color(0xFF666666), // gris más oscuro
            Color(0xFF999999), // gris medio
            Color(0xFFCCCCCC), // gris claro
            Color.White        // blanco
        )
        val duration = 1200 // ms
        val currentTime = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) }
        androidx.compose.runtime.LaunchedEffect(active) {
            while (active) {
                kotlinx.coroutines.delay(80)
                currentTime.value = System.currentTimeMillis()
            }
        }
        val animPhase = ((currentTime.value / (duration / (dotCount + 1))) % (dotCount + 1)).toInt()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            for (i in 0 until dotCount) {
                // El punto más a la izquierda es el primero en animar
                val progress = (animPhase - i).coerceAtLeast(0)
                // El color y tamaño cambian progresivamente de oscuro a blanco
                val colorIndex = progress.coerceIn(0, colorSteps.lastIndex)
                val dotColor by animateColorAsState(
                    targetValue = colorSteps[colorIndex],
                    label = "dotColor$i"
                )
                val dotSize by animateFloatAsState(
                    targetValue = baseSize + (maxSize - baseSize) * (colorIndex.toFloat() / colorSteps.lastIndex),
                    label = "dotSize$i"
                )
                Box(
                    modifier = Modifier
                        .size(dotSize.dp)
                        .padding(end = 4.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth()) {
                        drawCircle(color = dotColor)
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
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
}

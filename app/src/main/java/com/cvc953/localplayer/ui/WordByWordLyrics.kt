package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.TtmlSyllable

/**
 * Componente para mostrar una silaba individual con highlight animado
 * Inspirado en YouLyPlus para mostrar sincronizacion palabra por palabra
 */
@Composable
fun SyllableLyric(
    syllable: TtmlSyllable,
    currentPosition: Long,
    isLineActive: Boolean,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(0xFF707070)
    val baseFontSize = if (isLineActive) 30f else 28f

    val chars = syllable.text.toCharArray()
    val charCount = chars.size.coerceAtLeast(1)
    val progress = if (syllable.durationMs > 0) {
        ((currentPosition - syllable.timeMs).toFloat() / syllable.durationMs.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }
    val revealCount = if (isLineActive && currentPosition >= syllable.timeMs) {
        (progress * charCount).toInt().coerceIn(0, charCount)
    } else {
        0
    }

    Row(modifier = modifier.padding(horizontal = 1.5.dp)) {
        chars.forEachIndexed { index, ch ->
            val charColor by animateColorAsState(
                targetValue = if (index < revealCount) Color.White else baseColor,
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                label = "charColor"
            )
            Text(
                text = ch.toString(),
                color = charColor,
                fontSize = baseFontSize.sp,
                lineHeight = (baseFontSize + 10f).sp,
                fontWeight = if (isLineActive) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

/**
 * Componente para mostrar una linea completa con palabras sincronizadas
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordLine(
    syllables: List<TtmlSyllable>,
    currentPosition: Long,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        syllables.forEach { syllable ->
            // Una silaba esta activa si el tiempo actual esta dentro de su rango
            SyllableLyric(
                syllable = syllable,
                currentPosition = currentPosition,
                isLineActive = isActive
            )
        }
    }
}

/**
 * Animacion de tres puntos como Apple Music para pausas/instrumentales
 */
@Composable
fun LoadingDotsAnimation(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (!isVisible) return

    val transition = rememberInfiniteTransition(label = "dots")

    val dot1 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.6f at 0
                1.2f at 300
                0.6f at 900
            },
            initialStartOffset = StartOffset(0)
        ),
        label = "dot1"
    )

    val dot2 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.6f at 0
                1.2f at 300
                0.6f at 900
            },
            initialStartOffset = StartOffset(150)
        ),
        label = "dot2"
    )

    val dot3 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.6f at 0
                1.2f at 300
                0.6f at 900
            },
            initialStartOffset = StartOffset(300)
        ),
        label = "dot3"
    )

    Box(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = Color(0xFF505050),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .scale(dot1)
                    .padding(horizontal = 6.dp)
            )
            Text(
                text = "●",
                color = Color(0xFF505050),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .scale(dot2)
                    .padding(horizontal = 6.dp)
            )
            Text(
                text = "●",
                color = Color(0xFF505050),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .scale(dot3)
                    .padding(horizontal = 6.dp)
            )
        }
    }
}
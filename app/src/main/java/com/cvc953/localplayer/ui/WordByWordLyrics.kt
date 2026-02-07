package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.TtmlSyllable

/**
 * Componente para mostrar una sílaba individual con highlight animado
 * Inspirado en YouLyPlus para mostrar sincronización palabra por palabra
 */
@Composable
fun SyllableLyric(
    syllable: TtmlSyllable,
    currentPosition: Long,
    isLineActive: Boolean,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(0xFF707070)
    val baseFontSize = if (isLineActive) 21f else 20f

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
 * Componente para mostrar una línea completa con palabras sincronizadas
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
            // Una sílaba está activa si el tiempo actual está dentro de su rango
            SyllableLyric(
                syllable = syllable,
                currentPosition = currentPosition,
                isLineActive = isActive
            )
        }
    }
}

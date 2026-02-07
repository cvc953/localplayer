package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    isActive: Boolean,
    isLineActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFFFFFFFF) // Blanco brillante cuando está activo
            isLineActive -> Color(0xFFE0E0E0) // Gris claro cuando la línea está activa
            else -> Color(0xFF707070) // Gris oscuro cuando no está activo
        },
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "syllableColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 1f,
        animationSpec = tween(durationMillis = 100, easing = EaseInOut),
        label = "syllableScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            isLineActive -> 0.95f
            else -> 0.45f
        },
        animationSpec = tween(durationMillis = 120),
        label = "syllableAlpha"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isActive) 4f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "syllableElevation"
    )

    Text(
        text = syllable.text,
        color = color,
        fontSize = (19f * scale).sp,
        fontWeight = when {
            isActive -> FontWeight.Bold
            isLineActive -> FontWeight.SemiBold
            else -> FontWeight.Medium
        },
        modifier = modifier
            .padding(horizontal = 1.5.dp)
            .alpha(alpha)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isActive) -2f else 0f
            }
            .shadow(
                elevation = elevation.dp,
                spotColor = Color.White.copy(alpha = 0.3f)
            )
    )
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
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        syllables.forEach { syllable ->
            // Una sílaba está activa si el tiempo actual está dentro de su rango
            val syllableActive = isActive && 
                currentPosition >= syllable.timeMs && 
                currentPosition < (syllable.timeMs + syllable.durationMs)

            SyllableLyric(
                syllable = syllable,
                isActive = syllableActive,
                isLineActive = isActive
            )
        }
    }
}

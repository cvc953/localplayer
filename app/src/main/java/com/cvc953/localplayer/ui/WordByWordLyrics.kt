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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

    // Reducir padding si la sílaba continúa en la siguiente palabra
    val horizontalPadding = if (syllable.continuesWord) 0.dp else 3.dp

    Row(modifier = modifier.padding(end = horizontalPadding)) {
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
                fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.Bold
            )
        }
    }
}

/**
 * Componente para mostrar una silaba de fondo (entre parentesis) mas pequeña
 */
@Composable
fun BackgroundSyllableLyric(
    syllable: TtmlSyllable,
    currentPosition: Long,
    isLineActive: Boolean,
    modifier: Modifier = Modifier
) {
    // Solo mostrar si la línea está activa
    if (!isLineActive) return
    
    val baseColor = Color(0xFF585858)
    val baseFontSize = 20f

    val chars = syllable.text.toCharArray()
    val charCount = chars.size.coerceAtLeast(1)
    val progress = if (syllable.durationMs > 0) {
        ((currentPosition - syllable.timeMs).toFloat() / syllable.durationMs.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }
    val revealCount = if (currentPosition >= syllable.timeMs) {
        (progress * charCount).toInt().coerceIn(0, charCount)
    } else {
        0
    }

    // Reducir padding si la sílaba continúa en la siguiente palabra
    val horizontalPadding = if (syllable.continuesWord) 0.dp else 3.dp

    Row(modifier = modifier.padding(end = horizontalPadding)) {
        chars.forEachIndexed { index, ch ->
            val charColor by animateColorAsState(
                targetValue = if (index < revealCount) Color(0xFFB0B0B0) else baseColor,
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                label = "charColor"
            )
            Text(
                text = ch.toString(),
                color = charColor,
                fontSize = baseFontSize.sp,
                lineHeight = (baseFontSize + 8f).sp,
                fontWeight = FontWeight.Bold
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
    // Separar sílabas principales de las de fondo (entre paréntesis)
    val mainSyllables = syllables.filter { !it.isBackground }
    val backgroundSyllables = syllables.filter { it.isBackground }
    
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Agrupar sílabas que forman una palabra
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center
        ) {
            var wordBuffer = mutableListOf<TtmlSyllable>()
            @Composable
            fun flushWord() {
                if (wordBuffer.isNotEmpty()) {
                    Row {
                        wordBuffer.forEach { syllable ->
                            SyllableLyric(
                                syllable = syllable,
                                currentPosition = currentPosition,
                                isLineActive = isActive
                            )
                        }
                    }
                    wordBuffer = mutableListOf()
                }
            }
            mainSyllables.forEach { syllable ->
                wordBuffer.add(syllable)
                if (!syllable.continuesWord) {
                    flushWord()
                }
            }
            // Por si la última palabra no se ha vaciado
            flushWord()
        }
        
        // Línea de fondo (más pequeña, debajo) - solo visible cuando la línea está activa
        if (backgroundSyllables.isNotEmpty() && isActive) {
            Spacer(modifier = Modifier.padding(top = 2.dp))
            FlowRow(
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center
            ) {
                backgroundSyllables.forEach { syllable ->
                    BackgroundSyllableLyric(
                        syllable = syllable,
                        currentPosition = currentPosition,
                        isLineActive = isActive
                    )
                }
            }
        }
    }
}

/**
 * Animacion de tres puntos como Apple Music para pausas/instrumentales
 */
@Composable
fun LoadingDotsAnimation(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    durationMs: Long = 1000L, // Duración total del gap instrumental
    elapsedMs: Long = 0L // Tiempo transcurrido actual
) {
    if (!isVisible) return

    // Calcular el progreso de cada punto
    val progress = (elapsedMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    // Cada punto se vuelve blanco en tercios
    val dot1Active = progress >= 1f / 3f
    val dot2Active = progress >= 2f / 3f
    val dot3Active = progress >= 1f

    val baseColor = Color(0xFF505050)
    val activeColor = Color.White

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
                color = if (dot1Active) activeColor else baseColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = "●",
                color = if (dot2Active) activeColor else baseColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = "●",
                color = if (dot3Active) activeColor else baseColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
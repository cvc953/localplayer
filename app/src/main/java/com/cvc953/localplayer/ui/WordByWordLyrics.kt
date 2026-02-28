package com.cvc953.localplayer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.TtmlSyllable
import com.cvc953.localplayer.ui.theme.LocalExtendedColors

@Suppress("ktlint:standard:function-naming")
/*
 * Componente para mostrar una silaba individual con highlight animado
 * Inspirado en YouLyPlus para mostrar sincronizacion palabra por palabra
 */
@Composable
fun SyllableLyric(
    syllable: TtmlSyllable,
    currentPosition: Long,
    isLineActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseColor = Color(0xFF707070)
    val baseFontSize = 30f

    val text = syllable.text
    val charCount = text.length.coerceAtLeast(1)
    val rawProgress = if (syllable.durationMs > 0) {
        ((currentPosition - syllable.timeMs).toFloat() / syllable.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val targetProgress = if (isLineActive && currentPosition >= syllable.timeMs) rawProgress else 0f

    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
    val revealCount = (animatedProgress * charCount).toInt().coerceIn(0, charCount)

    val revealed = if (revealCount > 0) text.substring(0, revealCount) else ""
    val rest = if (revealCount < charCount) text.substring(revealCount) else ""

    val horizontalPadding = if (syllable.continuesWord) 0.dp else 3.dp

    Row(modifier = modifier.padding(end = horizontalPadding)) {
        Text(
            buildAnnotatedString {
                if (revealed.isNotEmpty()) {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = baseFontSize.sp)) {
                        append(revealed)
                    }
                }
                if (rest.isNotEmpty()) {
                    withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold, fontSize = baseFontSize.sp)) {
                        append(rest)
                    }
                }
            },
            lineHeight = (baseFontSize + 10f).sp,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
/*
 * Componente para mostrar una silaba de fondo (entre parentesis) mas pequeña
 */
@Composable
fun BackgroundSyllableLyric(
    syllable: TtmlSyllable,
    currentPosition: Long,
    isLineActive: Boolean,
    modifier: Modifier = Modifier,
) {
    // Solo mostrar si la línea está activa
    if (!isLineActive) return
    val baseColor = Color(0xFF585858)
    val baseFontSize = 20f

    val text = syllable.text
    val charCount = text.length.coerceAtLeast(1)
    val rawProgress = if (syllable.durationMs > 0) {
        ((currentPosition - syllable.timeMs).toFloat() / syllable.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val targetProgress = if (currentPosition >= syllable.timeMs) rawProgress else 0f
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
    val revealCount = (animatedProgress * charCount).toInt().coerceIn(0, charCount)

    val revealed = if (revealCount > 0) text.substring(0, revealCount) else ""
    val rest = if (revealCount < charCount) text.substring(revealCount) else ""

    val horizontalPadding = if (syllable.continuesWord) 0.dp else 3.dp

    Row(modifier = modifier.padding(end = horizontalPadding)) {
        Text(
            buildAnnotatedString {
                if (revealed.isNotEmpty()) {
                    withStyle(SpanStyle(color = Color(0xFFB0B0B0), fontSize = baseFontSize.sp, fontWeight = FontWeight.Bold)) {
                        append(revealed)
                    }
                }
                if (rest.isNotEmpty()) {
                    withStyle(SpanStyle(color = baseColor, fontSize = baseFontSize.sp, fontWeight = FontWeight.Bold)) {
                        append(rest)
                    }
                }
            },
            lineHeight = (baseFontSize + 8f).sp,
        )
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
    modifier: Modifier = Modifier,
) {
    // Separar sílabas principales de las de fondo (entre paréntesis)
    val mainSyllables = syllables.filter { !it.isBackground }
    val backgroundSyllables = syllables.filter { it.isBackground }

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Agrupar sílabas que forman una palabra
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center,
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
                                isLineActive = isActive,
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
                verticalArrangement = Arrangement.Center,
            ) {
                backgroundSyllables.forEach { syllable ->
                    BackgroundSyllableLyric(
                        syllable = syllable,
                        currentPosition = currentPosition,
                        isLineActive = isActive,
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
    elapsedMs: Long = 0L, // Tiempo transcurrido actual
) {
    if (!isVisible) return

    val dotCount = 3
    val colorSteps =
        listOf(
            // Color(0xFF505050), // gris oscuro
            // Color(0xFF888888), // gris medio
            // Color(0xFFCCCCCC), // gris claro
            // Color.White, // blanco
            LocalExtendedColors.current.textSecondarySoft,
            LocalExtendedColors.current.textSecondary,
            LocalExtendedColors.current.textSecondaryStrong,
            MaterialTheme.colorScheme.onBackground,
        )
    val minSize = 14f
    val maxSize = 22f
    val appearDuration = durationMs / (dotCount + 1)
    val lastDotWhiteThreshold = 500L

    Box(
        modifier =
            modifier
                .padding(horizontal = 24.dp, vertical = 0.dp)
                .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until dotCount) {
                // Cada punto tiene su propio tiempo de aparición y animación
                val dotAppear = appearDuration * (i + 1)
                val dotElapsed = (elapsedMs - dotAppear).coerceAtLeast(0L)
                val dotProgress = (dotElapsed.toFloat() / appearDuration).coerceIn(0f, 1f)

                // Color animado: de gris oscuro a blanco
                val colorIndex = (dotProgress * (colorSteps.lastIndex)).toInt().coerceIn(0, colorSteps.lastIndex)
                val baseColor = colorSteps[colorIndex]
                // El último punto se vuelve blanco 500ms antes del final
                val dotColor = if (i == dotCount - 1 && durationMs - elapsedMs <= lastDotWhiteThreshold) Color.White else baseColor

                // Tamaño animado: de min a max
                val dotSize = minSize + (maxSize - minSize) * dotProgress

                // Animación fluida
                val animatedSize by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = dotSize,
                    animationSpec =
                        androidx.compose.animation.core
                            .tween(durationMillis = 180),
                    label = "dotSize$i",
                )
                val animatedColor by androidx.compose.animation.animateColorAsState(
                    targetValue = dotColor,
                    animationSpec =
                        androidx.compose.animation.core
                            .tween(durationMillis = 180),
                    label = "dotColor$i",
                )
                Text(
                    text = "●",
                    color = animatedColor,
                    fontSize = animatedSize.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

package com.cvc953.localplayer.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.TtmlAlignment
import com.cvc953.localplayer.model.TtmlSyllable

@Suppress("ktlint:standard:function-naming")
/*
 * Componente para mostrar una silaba individual con highlight animado
 * Inspirado en Apple Music para mostrar sincronizacion palabra por palabra
 */
@Composable
fun SyllableLyric(
    syllable: TtmlSyllable,
    currentPosition: Long,
    isLineActive: Boolean,
    baseColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val baseFontSize = 30f

    val rawProgress =
        if (syllable.durationMs > 0) {
            ((currentPosition - syllable.timeMs).toFloat() / syllable.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    // Para sílabas sostenidas, clampear el progreso a 1.0 si ya llegó ahí
    val clampedProgress = if (syllable.isSustained && rawProgress >= 1f) 1f else rawProgress
    val targetProgress = if (isLineActive && currentPosition >= syllable.timeMs) clampedProgress else 0f

    val horizontalPadding = if (syllable.continuesWord) 0.dp else 3.dp

    Row(modifier = modifier.padding(end = horizontalPadding)) {
        ProgressiveFillSyllableText(
            text = syllable.text,
            progress = targetProgress,
            baseColor = baseColor,
            activeColor = activeColor,
            fontSizeSp = baseFontSize,
            lineHeightSp = baseFontSize + 10f,
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
    baseColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    // Solo mostrar si la línea está activa
    if (!isLineActive) return
    val baseFontSize = 20f

    val rawProgress =
        if (syllable.durationMs > 0) {
            ((currentPosition - syllable.timeMs).toFloat() / syllable.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    // Para sílabas sostenidas, clampear el progreso a 1.0 si ya llegó ahí
    val clampedProgress = if (syllable.isSustained && rawProgress >= 1f) 1f else rawProgress
    val targetProgress = if (currentPosition >= syllable.timeMs) clampedProgress else 0f
    val horizontalPadding = if (syllable.continuesWord) 0.dp else 3.dp

    Row(modifier = modifier.padding(end = horizontalPadding)) {
        ProgressiveFillSyllableText(
            text = syllable.text,
            progress = targetProgress,
            baseColor = baseColor,
            activeColor = activeColor,
            fontSizeSp = baseFontSize,
            lineHeightSp = baseFontSize + 8f,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ProgressiveFillSyllableText(
    text: String,
    progress: Float,
    baseColor: Color,
    activeColor: Color,
    fontSizeSp: Float,
    lineHeightSp: Float,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box {
        Text(
            text = text,
            color = baseColor,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = lineHeightSp.sp,
            softWrap = true,
        )
        Text(
            text = text,
            color = activeColor,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = lineHeightSp.sp,
            softWrap = true,
            modifier =
                Modifier.drawWithContent {
                    val clipRight = size.width * clampedProgress

                    clipRect(right = clipRight) {
                        this@drawWithContent.drawContent()
                    }
                },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
/*
 * Componente para mostrar una linea completa con palabras sincronizadas
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordLine(
    modifier: Modifier = Modifier,
    syllables: List<TtmlSyllable>,
    currentPosition: Long,
    isActive: Boolean,
    baseColor: Color = Color(0xFF707070),
    activeColor: Color = Color.White,
    horizontalAlignment: TtmlAlignment = TtmlAlignment.LEFT,
    maxWidthFraction: Float = 1f, // 1 = 100%, 0.666f = 2/3
) {
    // Separar sílabas principales de las de fondo (entre paréntesis)
    val mainSyllables = syllables.filter { !it.isBackground }
    val backgroundSyllables = syllables.filter { it.isBackground }

    // Determinar alineación horizontal para FlowRow
    val flowHorizontalArrangement =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> Arrangement.Start
            TtmlAlignment.RIGHT -> Arrangement.End
        }

    val transition = updateTransition(targetState = isActive, label = "lyricLine")

    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
            } else {
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
            }
        },
    ) { isActive -> if (isActive) 1.05f else 1f }

    val pivotX =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> 0f
            TtmlAlignment.RIGHT -> 1f
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 0.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(pivotFractionX = pivotX, pivotFractionY = 0.5f)
                },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment =
            when (horizontalAlignment) {
                TtmlAlignment.LEFT -> Alignment.Start
                TtmlAlignment.RIGHT -> Alignment.End
            },
    ) {
        // Agrupar sílabas que forman una palabra
        FlowRow(
            horizontalArrangement = flowHorizontalArrangement,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(maxWidthFraction),
        ) {
            var wordBuffer = mutableListOf<TtmlSyllable>()

            fun isJapanese(text: String) = text.any { it in '\u3040'..'\u9FAF' }

            @Composable
            fun flushWord() {
                if (wordBuffer.isNotEmpty()) {
                    // si la palabra es japonesa, NO usar Row (para que se pueda partir)
                    // si el latina, SÍ unar Row
                    val japanese = wordBuffer.any { isJapanese(it.text) }
                    if (japanese) {
                        wordBuffer.forEach { syllable ->
                            SyllableLyric(
                                syllable = syllable,
                                currentPosition = currentPosition,
                                isLineActive = isActive,
                                baseColor = baseColor,
                                activeColor = activeColor,
                            )
                        }
                    } else {
                        Row {
                            wordBuffer.forEach { syllable ->
                                SyllableLyric(
                                    syllable = syllable,
                                    currentPosition = currentPosition,
                                    isLineActive = isActive,
                                    baseColor = baseColor,
                                    activeColor = activeColor,
                                )
                            }
                        }
                    }
                    wordBuffer = mutableListOf()
                }
            }
            mainSyllables.forEach { syllable ->
                wordBuffer.add(syllable)
                if (!syllable.continuesWord) {
                    flushWord()
                    // Espacio entre palabras
                    if (wordBuffer.isNotEmpty() && !isJapanese(syllable.text)) {
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
            flushWord()
            /*var wordBuffer = mutableListOf<TtmlSyllable>()

            @Composable
            fun flushWord() {
                if (wordBuffer.isNotEmpty()) {
                    Row {
                        wordBuffer.forEach { syllable ->
                            SyllableLyric(
                                syllable = syllable,
                                currentPosition = currentPosition,
                                isLineActive = isActive,
                                baseColor = baseColor,
                                activeColor = activeColor,
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
            flushWord()*/
        }

        // Línea de fondo (más pequeña, debajo) - solo visible cuando la línea está activa
        if (backgroundSyllables.isNotEmpty() && isActive) {
            Spacer(modifier = Modifier.padding(top = 2.dp))
            FlowRow(
                horizontalArrangement = flowHorizontalArrangement,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(maxWidthFraction),
            ) {
                backgroundSyllables.forEach { syllable ->
                    BackgroundSyllableLyric(
                        syllable = syllable,
                        currentPosition = currentPosition,
                        isLineActive = isActive,
                        baseColor = baseColor,
                        activeColor = activeColor.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
/*
 * Animacion de tres puntos como Apple Music para pausas/instrumentales
 */
@Composable
fun LoadingDotsAnimation(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    durationMs: Long = 1000L, // Duración total del gap instrumental
    elapsedMs: Long = 0L, // Tiempo transcurrido actual
    brightColor: Color = MaterialTheme.extendedColors.brightColor,
    // brightColor: Color = Color.White,
) {
    if (!isVisible) return

    val dotCount = 3
    val colorSteps = MaterialTheme.extendedColors.dotsColors

    val minSize = 20f
    val maxSize = 25f
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
                val dotColor = if (i == dotCount - 1 && durationMs - elapsedMs <= lastDotWhiteThreshold) brightColor else baseColor

                // Tamaño animado: de min a max
                val dotSize = minSize + (maxSize - minSize) * dotProgress

                // Animación fluida
                val animatedSize by animateFloatAsState(
                    targetValue = dotSize,
                    animationSpec =
                        tween(durationMillis = 180),
                    label = "dotSize$i",
                )
                val animatedColor by androidx.compose.animation.animateColorAsState(
                    targetValue = dotColor,
                    animationSpec =
                        tween(durationMillis = 180),
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

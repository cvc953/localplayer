package com.cvc953.localplayer.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

/**
 * Extrae el color dominante de un Bitmap usando Palette
 * Retorna el color dominante o un color por defecto
 */

suspend fun Bitmap.getDominantColor(defaultColor: Color): Color =
    try {
        val palette = Palette.from(this@getDominantColor).maximumColorCount(8).generate()
        val vibrantColor = palette.vibrantSwatch
        val dominantColor = palette.dominantSwatch
        val mutedColor = palette.mutedSwatch

        val swatch =
            vibrantColor ?: dominantColor ?: mutedColor
        if (swatch != null) {
            Color(swatch.rgb)
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }

/**
 * Aplica una transparencia muy baja a un color para crear un efecto de blur
 * @param alphaPercent Porcentaje de opacidad (0.0 a 1.0), por defecto 0.1 (10%)
 */
fun Color.withAlpha(alphaPercent: Float = 0.1f): Color = this.copy(alpha = alphaPercent.coerceIn(0f, 1f))

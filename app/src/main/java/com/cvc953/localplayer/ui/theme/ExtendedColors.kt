package com.cvc953.localplayer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val textSecondary: Color,
    val textSecondarySoft: Color,
    val textSecondaryStrong: Color,
    val texMeta: Color,
    val surfaceSheet: Color,
    val dotsColors: List<Color>,
    val brightColor: Color,
)

val LocalExtendedColors =
    staticCompositionLocalOf<ExtendedColors> {
        error("ExtendedColors not provided")
    }

package com.cvc953.localplayer.util

import androidx.compose.ui.graphics.Color

fun Color.darken(factor: Float = 0.7f): Color =
    this.copy(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
    )

fun Color.luminance(factor: Float = 0.7f): Color =
    this.copy(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
    )

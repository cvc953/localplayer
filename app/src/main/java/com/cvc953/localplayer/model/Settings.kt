package com.cvc953.localplayer.model

import android.content.res.Resources
import androidx.compose.foundation.isSystemInDarkTheme

data class Settings(
    val theme: String = "sistema", // "light", "dark", "system"
    val equalizerEnabled: Boolean = false,
    val playbackQuality: String = "auto", // "auto", "high", "low"
    // Agrega aquí más campos según necesidades futuras
)

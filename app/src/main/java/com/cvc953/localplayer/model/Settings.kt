package com.cvc953.localplayer.model

data class Settings(
    val theme: String = "system", // "light", "dark", "system"
    val equalizerEnabled: Boolean = false,
    val playbackQuality: String = "auto", // "auto", "high", "low"
    // Agrega aquí más campos según necesidades futuras
)

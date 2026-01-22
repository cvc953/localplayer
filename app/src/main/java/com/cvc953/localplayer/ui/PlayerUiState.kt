package com.cvc953.localplayer.ui

import com.cvc953.localplayer.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false
)

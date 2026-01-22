package com.cvc953.localplayer.ui

import androidx.compose.material3.FabPosition
import com.cvc953.localplayer.model.Song
import kotlin.time.Duration

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val  position: Long = 0L,
    val  duration: Long = 0L
)

package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Song

class PlaybackController(private val context: Context) {
    private val playerController: PlayerController = PlayerController.getInstance(context)

    fun play(song: Song, onCompletion: (() -> Unit)? = null) {
        playerController.playSong(song)
        // PlayerController handles completion internally; onCompletion can be invoked by caller if needed
    }

    fun pause() {
        playerController.pause()
    }

    fun resume() {
        playerController.resume()
    }

    fun stop() {
        playerController.stop()
    }

    fun seekTo(position: Long) {
        playerController.seekTo(position)
    }

    fun isPlaying(): Boolean = playerController.isPlaying()

    fun getCurrentPosition(): Long = playerController.getCurrentPosition()

    fun getDuration(): Long = playerController.getDuration()
}

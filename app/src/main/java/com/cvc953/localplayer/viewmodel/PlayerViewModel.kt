package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.player.PlayerController
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val playerController =
        PlayerController(
            context = application,
            scope = viewModelScope,
        )

    val playerState: StateFlow<com.cvc953.localplayer.ui.PlayerState> = playerController.state

    fun playSong(song: Song) {
        playerController.playNow(listOf(song))
    }

    fun playAlbum(songs: List<Song>) {
        playerController.playNow(songs)
    }

    fun queueNext(songs: List<Song>) {
        playerController.queueNext(songs)
    }

    fun queueEnd(songs: List<Song>) {
        playerController.queueEnd(songs)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun seekTo(position: Long) {
        playerController.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}

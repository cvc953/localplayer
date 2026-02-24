package com.cvc953.localplayer.player

import android.content.Context
import android.media.MediaPlayer
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerState(
    val curretSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Int = 0,
)

class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var mediaPlayer: MediaPlayer? = null
    private val queue = mutableListOf<Song>()
    private var currentIndex = -1

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    fun playNow(
        songs: List<Song>,
        startIndex: Int = 0,
    ) {
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex
        playCurrent()
    }

    fun queueNext(songs: List<Song>) {
        val insertIndex = (currentIndex + 1).coerceAtMost(queue.size)
        queue.addAll(insertIndex, songs)
    }

    fun queueEnd(songs: List<Song>) {
        queue.addAll(songs)
    }

    private fun playCurrent() {
        if (currentIndex !in queue.indices) return
        play(queue[currentIndex])
    }

    private fun play(song: Song) {
        mediaPlayer?.release()
        mediaPlayer =
            MediaPlayer().apply {
                setDataSource(context, song.uri)
                prepare()
                start()
                setOnCompletionListener {
                    if (currentIndex + 1 < queue.size) {
                        currentIndex++
                        playCurrent()
                    } else {
                        _state.update { it.copy(isPlaying = false) }
                    }
                }
            }

        _state.value =
            PlayerState(
                currentSong = song,
                isPlaying = true,
                duration = mediaPlayer?.duration?.toLong() ?: 0,
            )

        startProgressUpdates()
    }

    private var progressJob: Job? = null

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob =
            scope.launch {
                while (mediaPlayer != null) {
                    _state.update {
                        it.copy(position = mediaPlayer!!.currentPosition.toLong())
                    }
                    delay(200)
                }
            }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
            _state.update { s -> s.copy(isPlaying = it.isPlaying, position = it.currentPosition.toLong()) }
        }
    }

    fun release() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _state.update { s -> s.copy(position = position) }
    }
}

package com.cvc953.localplayer.controller

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

class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope? = null,
) {
    private var mediaPlayer: MediaPlayer? = null
    private val queue = mutableListOf<Song>()
    private var currentIndex = -1

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var progressJob: Job? = null

    fun playNow(
        songs: List<Song>,
        startIndex: Int = 0,
    ) {
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex
        playCurrent()
    }

    fun playSong(song: Song) {
        playNow(listOf(song))
    }

    fun queueNext(songs: List<Song>) {
        val insertIndex = (currentIndex + 1).coerceAtMost(queue.size)
        queue.addAll(insertIndex, songs)
    }

    fun queueEnd(songs: List<Song>) {
        queue.addAll(songs)
    }

    private fun playCurrent() {
        if (currentIndex !in queue.indices) {
            stop()
            return
        }
        val song = queue[currentIndex]
        play(song)
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

        _state.value = PlayerState(currentSong = song, isPlaying = true, position = 0L, duration = mediaPlayer?.duration?.toLong() ?: 0L)

        startProgressUpdates()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        val s = scope ?: return
        progressJob =
            s.launch {
                while (mediaPlayer != null) {
                    _state.update { it.copy(position = mediaPlayer?.currentPosition?.toLong() ?: it.position) }
                    delay(200L)
                }
            }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
            _state.update { s -> s.copy(isPlaying = it.isPlaying, position = it.currentPosition.toLong()) }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        mediaPlayer?.start()
        _state.update { it.copy(isPlaying = true) }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = PlayerState()
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _state.update { s -> s.copy(position = position) }
    }

    fun next() {
        if (currentIndex + 1 < queue.size) {
            currentIndex++
            playCurrent()
        } else {
            stop()
        }
    }

    fun previous() {
        if (currentIndex - 1 >= 0) {
            currentIndex--
            playCurrent()
        } else {
            seekTo(0L)
        }
    }

    fun release() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    fun getDuration(): Long = mediaPlayer?.duration?.toLong() ?: 0L

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    companion object {
        @Suppress("ktlint:standard:property-naming")
        @Volatile
        private var INSTANCE: PlayerController? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope? = null,
        ): PlayerController =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerController(context.applicationContext, scope).also { INSTANCE = it }
            }
    }
}

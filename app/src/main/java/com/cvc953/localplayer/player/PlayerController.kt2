package com.cvc953.localplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.cvc953.localplayer.model.Song

class PlayerController(context: Context) {

    private val player = ExoPlayer.Builder(context).build()
    private var currentSong: Song? = null

    fun toggle(
        song: Song,
        onStateChanged: (Song?, Boolean) -> Unit
    ) {
        if (currentSong?.id == song.id) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        } else {
            play(song)
        }

        onStateChanged(currentSong, player.isPlaying)
    }

    private fun play(song: Song) {
        currentSong = song
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.play()
    }

    fun release() {
        player.release()
    }
}

package com.cvc953.localplayer.player

import com.cvc953.localplayer.model.Song

class MusicPlayer {

    private var playingSong: Song? = null
    private var playing = false

    fun toggle(song: Song) {
        if (playingSong == song) {
            playing = !playing
        } else {
            playingSong = song
            playing = true
        }
    }

    fun isPlaying(): Boolean = playing
    fun currentSong(): Song? = playingSong
}

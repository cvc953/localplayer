package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    application: Application,
) : AndroidViewModel(application) {

    // UI visibility state
    private val _isPlayerScreenVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlayerScreenVisible: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlayerScreenVisible

    private val _isSettingsVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSettingsVisible: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSettingsVisible

    private val _isAboutVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAboutVisible: kotlinx.coroutines.flow.StateFlow<Boolean> = _isAboutVisible

    private val _isEqualizerVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isEqualizerVisible: kotlinx.coroutines.flow.StateFlow<Boolean> = _isEqualizerVisible

    private val _showLyrics = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showLyrics: kotlinx.coroutines.flow.StateFlow<Boolean> = _showLyrics

    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
    }

    // Methods to update visibility
    fun showPlayerScreen(show: Boolean) {
        _isPlayerScreenVisible.value = show
    }

    fun showSettings(show: Boolean) {
        _isSettingsVisible.value = show
    }

    fun showAbout(show: Boolean) {
        _isAboutVisible.value = show
    }

    fun showEqualizer(show: Boolean) {
        _isEqualizerVisible.value = show
    }

    // Expose playback state (read-only) from the centralized PlayerController
    val playerState: StateFlow<PlayerState> = com.cvc953.localplayer.controller.PlayerController.getInstance(getApplication()).state

    fun openPlayerScreen() {
        showPlayerScreen(true)
    }

    fun closePlayerScreen() {
        showPlayerScreen(false)
    }

    fun closeSettingsScreen() {
        showSettings(false)
    }

    fun closeEqualizerScreen() {
        showEqualizer(false)
    }
}

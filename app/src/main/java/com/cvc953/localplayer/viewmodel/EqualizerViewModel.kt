package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.EqualizerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = EqualizerController(application)
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled
    private val _bands = MutableStateFlow<List<Int>>(emptyList())
    val bands: StateFlow<List<Int>> = _bands
    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets
    private val _selectedPreset = MutableStateFlow<String?>(null)
    val selectedPreset: StateFlow<String?> = _selectedPreset

    init {
        loadEqualizerState()
    }

    private fun loadEqualizerState() {
        viewModelScope.launch {
            _isEnabled.value = controller.isEnabled()
            _bands.value = controller.getBands()
            _presets.value = controller.getPresets()
            _selectedPreset.value = controller.getSelectedPreset()
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            controller.setEnabled(enabled)
            _isEnabled.value = enabled
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        viewModelScope.launch {
            controller.setBandLevel(band, level)
            _bands.value = controller.getBands()
        }
    }

    fun selectPreset(preset: String) {
        viewModelScope.launch {
            controller.selectPreset(preset)
            _selectedPreset.value = preset
        }
    }

    fun resetBandLevels() {
        viewModelScope.launch {
            controller.resetBandLevels()
            _bands.value = controller.getBands()
        }
    }
}

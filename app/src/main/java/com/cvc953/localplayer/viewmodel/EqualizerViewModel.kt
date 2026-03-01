package com.cvc953.localplayer.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EqualizerViewModel : ViewModel() {
    // Número de bandas (ejemplo: 5 bandas)
    private val _bandLevels = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandLevels: StateFlow<List<Int>> = _bandLevels

    private val _bandMinLevel = MutableStateFlow(-10)
    val bandMinLevel: StateFlow<Int> = _bandMinLevel
    private val _bandMaxLevel = MutableStateFlow(10)
    val bandMaxLevel: StateFlow<Int> = _bandMaxLevel

    private val _bandFrequencies = MutableStateFlow(listOf(60, 230, 910, 3600, 14000))
    val bandFrequencies: StateFlow<List<Int>> = _bandFrequencies

    private val _presets = MutableStateFlow(listOf("Normal", "Pop", "Rock", "Jazz", "Clásica"))
    val presets: StateFlow<List<String>> = _presets
    private val _selectedPreset = MutableStateFlow(0)
    val selectedPreset: StateFlow<Int> = _selectedPreset

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _isSupported = MutableStateFlow(true)
    val isSupported: StateFlow<Boolean> = _isSupported

    fun setBandLevel(band: Int, level: Int) {
        _bandLevels.value = _bandLevels.value.toMutableList().also { it[band] = level }
    }
    fun setSelectedPreset(index: Int) { _selectedPreset.value = index }
    fun setEnabled(enabled: Boolean) { _enabled.value = enabled }
    fun setSupported(supported: Boolean) { _isSupported.value = supported }
}

package com.cvc953.localplayer.controller

import android.app.Application

class EqualizerController(private val application: Application) {
    // Placeholder for actual equalizer implementation
    private var enabled = false
    private var bands = listOf(0, 0, 0, 0, 0)
    private var presets = listOf("Normal", "Pop", "Rock", "Jazz", "Classical")
    private var selectedPreset: String? = null

    fun isEnabled(): Boolean = enabled
    fun setEnabled(value: Boolean) { enabled = value }

    fun getBands(): List<Int> = bands
    fun setBandLevel(band: Int, level: Int) {
        if (band in bands.indices) {
            bands = bands.toMutableList().also { it[band] = level }
        }
    }

    fun resetBandLevels() {
        bands = List(bands.size) { 0 }
    }

    fun getPresets(): List<String> = presets
    fun selectPreset(preset: String) {
        if (preset in presets) selectedPreset = preset
    }
    fun getSelectedPreset(): String? = selectedPreset
}

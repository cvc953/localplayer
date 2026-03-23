package com.cvc953.localplayer.controller

import android.app.Application
import android.media.audiofx.Equalizer
import android.util.Log

class EqualizerController(private val application: Application) {
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = 0
    private var savedEnabledState: Boolean = false

    fun initializeWithAudioSession(sessionId: Int) {
        try {
            // Save the enabled state before reinitializing to prevent transient audio pops
            savedEnabledState = equalizer?.enabled ?: false
            
            // Release previous equalizer if exists
            equalizer?.release()
            
            audioSessionId = sessionId
            equalizer = Equalizer(0, sessionId).apply {
                enabled = false
            }
            Log.d("EqualizerController", "Equalizer initialized with session $sessionId, bands=${equalizer?.numberOfBands}, restoring enabled=$savedEnabledState")
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error initializing equalizer", e)
        }
    }

    fun restoreSavedEnabledState() {
        try {
            if (savedEnabledState && equalizer != null) {
                equalizer?.enabled = true
                Log.d("EqualizerController", "Restored equalizer enabled state: true")
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error restoring enabled state", e)
        }
    }

    fun release() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error releasing equalizer", e)
        }
    }

    fun isEnabled(): Boolean = equalizer?.enabled ?: false
    
    fun setEnabled(value: Boolean) {
        try {
            equalizer?.enabled = value
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error setting enabled", e)
        }
    }

    fun getBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 0

    fun getBands(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return try {
            (0 until eq.numberOfBands.toInt()).map { band ->
                eq.getBandLevel(band.toShort()).toInt()
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error getting bands", e)
            emptyList()
        }
    }

    fun getBandFreqs(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return try {
            (0 until eq.numberOfBands.toInt()).map { band ->
                eq.getCenterFreq(band.toShort()) / 1000 // Convert mHz to Hz
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error getting band freqs", e)
            emptyList()
        }
    }

    fun getBandLevelRange(): Pair<Int, Int> {
        val eq = equalizer ?: return Pair(0, 0)
        return try {
            val range = eq.bandLevelRange
            Pair(range[0].toInt(), range[1].toInt())
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error getting band level range", e)
            Pair(0, 0)
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error setting band level", e)
        }
    }

    fun resetBandLevels() {
        val eq = equalizer ?: return
        try {
            for (band in 0 until eq.numberOfBands.toInt()) {
                eq.setBandLevel(band.toShort(), 0)
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error resetting bands", e)
        }
    }

    fun getPresets(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try {
            (0 until eq.numberOfPresets.toInt()).map { preset ->
                eq.getPresetName(preset.toShort())
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error getting presets", e)
            emptyList()
        }
    }

    fun selectPreset(index: Int) {
        try {
            equalizer?.usePreset(index.toShort())
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error selecting preset", e)
        }
    }

    fun getCurrentPreset(): Short? {
        return try {
            equalizer?.currentPreset
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error getting current preset", e)
            null
        }
    }

    fun getSelectedPreset(): String? {
        val eq = equalizer ?: return null
        return try {
            val currentPreset = eq.currentPreset
            if (currentPreset >= 0) {
                eq.getPresetName(currentPreset)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error getting selected preset", e)
            null
        }
    }
}

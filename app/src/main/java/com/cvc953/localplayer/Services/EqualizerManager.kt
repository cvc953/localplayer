package com.cvc953.localplayer.services

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Small wrapper around android.media.audiofx.Equalizer to manage lifecycle and provide helpers.
 */
class EqualizerManager {
        private fun log(msg: String) {
            android.util.Log.d("EqualizerManager", msg)
        }
    private var eq: Equalizer? = null

    fun init(sessionId: Int): Boolean {
        release()
        return try {
            // priority 0 is fine for basic usage
            val e = Equalizer(0, sessionId)
            e.enabled = false
            eq = e
            android.util.Log.d("EqualizerManager", "Initialized Equalizer with sessionId=$sessionId; bands=${e.numberOfBands}")
            true
        } catch (t: Throwable) {
            Log.w("EqualizerManager", "Failed to init Equalizer", t)
            eq = null
            false
        }
    }

    fun release() {
        try {
            eq?.release()
        } catch (_: Exception) {
        }
        eq = null
    }

    fun isAvailable(): Boolean = eq != null

    fun setEnabled(enabled: Boolean) {
        try {
            eq?.enabled = enabled
        } catch (_: Exception) {
        }
    }

    fun getNumberOfBands(): Short = eq?.numberOfBands ?: 0

    fun getBandLevelRange(): ShortArray = eq?.bandLevelRange ?: shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE)

    fun getBandLevel(band: Short): Short = try { eq?.getBandLevel(band) ?: 0 } catch (_: Exception) { 0 }

    fun setBandLevel(band: Short, level: Short) {
        try {
            log("setBandLevel(band=$band, level=$level)")
            eq?.setBandLevel(band, level)
        } catch (e: Exception) {
            log("Error en setBandLevel: ${e.message}")
        }
    }

    fun getPresets(): List<String> {
        return try {
            val e = eq ?: return emptyList()
            val n = e.numberOfPresets
            val list = mutableListOf<String>()
            for (i in 0 until n) list.add(e.getPresetName(i.toShort()))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun usePreset(index: Int) {
        try {
            log("usePreset(index=$index)")
            val e = eq ?: return
            if (index >= 0 && index < e.numberOfPresets) e.usePreset(index.toShort())
        } catch (e: Exception) {
            log("Error en usePreset: ${e.message}")
        }
    }

    fun getBandCenterFreq(band: Short): Int = try { eq?.getCenterFreq(band) ?: 0 } catch (_: Exception) { 0 }
}

package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.services.EqualizerManager
import kotlinx.coroutines.Dispatchers
import com.cvc953.localplayer.controller.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {
        // Permite acceso a AppPrefs para presets de usuario
        private val appPrefs = com.cvc953.localplayer.preferences.AppPrefs(application.applicationContext)
        /** Aplica un preset de usuario por nombre: ajusta bandas, actualiza estado y guarda selección. */
        fun applyUserPreset(name: String) {
            viewModelScope.launch(Dispatchers.IO) {
                val preset = appPrefs.getUserPresets().firstOrNull { it.first == name } ?: return@launch
                val levels = preset.second
                val bc = manager.getNumberOfBands().toInt()
                for (i in 0 until minOf(bc, levels.size)) {
                    try {
                        manager.setBandLevel(i.toShort(), levels[i].toShort())
                    } catch (_: Exception) {}
                }
                // Actualiza estado de bandas y preset seleccionado
                val newLevels = mutableListOf<Int>()
                for (i in 0 until bc) newLevels.add(manager.getBandLevel(i.toShort()).toInt())
                _bands.value = newLevels
                _selectedPreset.value = name
                // Guarda selección y niveles personalizados
                appPrefs.setEqualizerPresetIndex(-1)
                appPrefs.setCustomBandLevels(newLevels)
            }
        }
    private val manager = EqualizerManager()
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try to initialize using the real audio session id from PlayerController.
                val pc = PlayerController.getInstance(getApplication(), null)
                val sessionId = try { pc.getAudioSessionId() } catch (_: Exception) { 0 }
                var ok = false
                if (sessionId != 0) ok = manager.init(sessionId)
                // fallback to global session (0) if session-specific init failed
                if (!ok) {
                    try { ok = manager.init(0) } catch (_: Exception) {}
                }
            } catch (_: Exception) {
            }
            _isEnabled.value = manager.isAvailable()
            val bc = manager.getNumberOfBands().toInt()
            val levels = mutableListOf<Int>()
            for (i in 0 until bc) levels.add(manager.getBandLevel(i.toShort()).toInt())
            _bands.value = levels
            // sanitize vendor preset names for UI readability
            val rawPresets = manager.getPresets()
            _presets.value = rawPresets.map { sanitizePresetName(it) }
            // we cannot reliably get selected preset name from manager; leave null
            _selectedPreset.value = null
        }
    }

    private fun sanitizePresetName(raw: String): String {
        try {
            var s = raw.lowercase().replace(Regex("[^a-z0-9]"), " ").trim()
            if (s.isEmpty()) return raw.trim()
            // separate camelCase like 'JazzHoperal' -> 'Jazz Hoperal'
            s = s.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            // collapse multiple spaces and capitalize words
            s = s.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            return s
        } catch (_: Exception) {
            return raw
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manager.setEnabled(enabled)
            } catch (_: Exception) {
            }
            _isEnabled.value = enabled
        }
    }

    /** Refresh/attempt reinitialization of equalizer state (useful for Retry button). */
    fun refresh() {
        loadEqualizerState()
    }

    fun setBandLevel(band: Int, level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manager.setBandLevel(band.toShort(), level.toShort())
            } catch (_: Exception) {
            }
            // refresh
            val bc = manager.getNumberOfBands().toInt()
            val levels = mutableListOf<Int>()
            for (i in 0 until bc) levels.add(manager.getBandLevel(i.toShort()).toInt())
            _bands.value = levels
            _selectedPreset.value = null
        }
    }

    fun selectPreset(preset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val idx = manager.getPresets().indexOf(preset)
                if (idx >= 0) manager.usePreset(idx)
            } catch (_: Exception) {
            }
            _selectedPreset.value = preset
            // update band levels
            val bc = manager.getNumberOfBands().toInt()
            val levels = mutableListOf<Int>()
            for (i in 0 until bc) levels.add(manager.getBandLevel(i.toShort()).toInt())
            _bands.value = levels
        }
    }

    fun resetBandLevels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bc = manager.getNumberOfBands().toInt()
                for (i in 0 until bc) manager.setBandLevel(i.toShort(), 0)
            } catch (_: Exception) {
            }
            val bc = manager.getNumberOfBands().toInt()
            val levels = mutableListOf<Int>()
            for (i in 0 until bc) levels.add(manager.getBandLevel(i.toShort()).toInt())
            _bands.value = levels
            _selectedPreset.value = null
        }
    }
}

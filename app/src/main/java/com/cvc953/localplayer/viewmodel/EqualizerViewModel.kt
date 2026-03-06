package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.EqualizerController
import com.cvc953.localplayer.controller.PlayerController
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EqualizerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appPrefs = AppPrefs(application)
    private val equalizerController = EqualizerController(application)

    // Track last initialized sessionId to avoid repeated re-initialization.
    private var lastEqSessionId: Int = 0

    private val _selectedPresetIndex = MutableStateFlow(0)
    val selectedPresetIndex: StateFlow<Int> = _selectedPresetIndex

    private val _bandCount = MutableStateFlow(0)
    val bandCount: StateFlow<Int> = _bandCount

    private val _bandFreqs = MutableStateFlow<List<Int>>(emptyList())
    val bandFreqs: StateFlow<List<Int>> = _bandFreqs

    private val _bandLevels = MutableStateFlow<List<Int>>(emptyList())
    val bandLevels: StateFlow<List<Int>> = _bandLevels

    private val _equalizerPresets = MutableStateFlow<List<String>>(emptyList())
    val equalizerPresets: StateFlow<List<String>> = _equalizerPresets

    private val _selectedPresetName = MutableStateFlow<String?>(null)
    val selectedPresetName: StateFlow<String?> = _selectedPresetName

    private val _isEqualizerVisible = MutableStateFlow(false)
    val isEqualizerVisible: StateFlow<Boolean> = _isEqualizerVisible

    private val _equalizerEnabled = MutableStateFlow(appPrefs.isEqualizerEnabled())
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled

    private val _userPresets = MutableStateFlow<List<Pair<String, List<Int>>>>(emptyList())
    val userPresets: StateFlow<List<Pair<String, List<Int>>>> = _userPresets

    private val _bandLevelRange = MutableStateFlow(Pair(-1500, 1500))
    val bandLevelRange: StateFlow<Pair<Int, Int>> = _bandLevelRange

    init {
        _userPresets.value = appPrefs.getUserPresets()
        try {
            val pc = PlayerController.getInstance(getApplication(), viewModelScope)

            pc.setOnAudioSessionIdChangedListener { sessionId ->
                if (sessionId != 0 && sessionId != lastEqSessionId) {
                    lastEqSessionId = sessionId
                    android.util.Log.d("EqualizerViewModel", "Audio session changed: $sessionId")
                    viewModelScope.launch {
                        safelyReinitializeEqualizer(sessionId)
                    }
                }
            }
            val currentSessionId = pc.getAudioSessionId()
            if (currentSessionId != 0 && currentSessionId != lastEqSessionId) {
                lastEqSessionId = currentSessionId
                android.util.Log.d("EqualizerViewModel", "Using current audio session: $currentSessionId")
                viewModelScope.launch {
                    safelyReinitializeEqualizer(currentSessionId)
                }
            }
        } catch (_: Exception) {
        }

        _bandCount.value = 0
        _bandFreqs.value = emptyList()
        _bandLevels.value = emptyList()
        _equalizerPresets.value = emptyList()
        _selectedPresetName.value = null
    }

    fun openEqualizerScreen() {
        _isEqualizerVisible.value = true
        try {
            val pc = PlayerController.getInstance(getApplication(), viewModelScope)
            val currentSessionId = pc.getAudioSessionId()
            if (currentSessionId != 0 && currentSessionId != lastEqSessionId) {
                lastEqSessionId = currentSessionId
                viewModelScope.launch {
                    safelyReinitializeEqualizer(currentSessionId)
                }
            } else {
                updateEqualizerStateFromDevice()
            }
        } catch (_: Exception) {
        }
    }

    fun closeEqualizerScreen() {
        _isEqualizerVisible.value = false
    }

    private suspend fun safelyReinitializeEqualizer(sessionId: Int) {
        try {
            equalizerController.setEnabled(false)
        } catch (_: Exception) {
        }

        kotlinx.coroutines.delay(80)

        equalizerController.initializeWithAudioSession(sessionId)
        updateEqualizerStateFromDevice()
    }

    private fun updateEqualizerStateFromDevice() {
        try {
            val bandCount = equalizerController.getBandCount()
            _bandCount.value = bandCount

            if (bandCount > 0) {
                _bandFreqs.value = equalizerController.getBandFreqs()
                _bandLevels.value = equalizerController.getBands()
                _equalizerPresets.value = sanitizePresetNames(equalizerController.getPresets())
                _selectedPresetName.value = equalizerController.getSelectedPreset()?.let { sanitizePresetName(it) }
                _bandLevelRange.value = equalizerController.getBandLevelRange()

                val savedLevels = appPrefs.getCustomBandLevels()
                if (savedLevels.size == bandCount) {
                    savedLevels.forEachIndexed { index, level ->
                        equalizerController.setBandLevel(index, level)
                    }
                    _bandLevels.value = savedLevels
                }

                val savedEnabled = appPrefs.isEqualizerEnabled()
                equalizerController.setEnabled(savedEnabled)
                _equalizerEnabled.value = savedEnabled
                equalizerController.restoreSavedEnabledState()

                android.util.Log.d("EqualizerViewModel", "Equalizer state updated: $bandCount bands, range=${_bandLevelRange.value}")
            }
        } catch (e: Exception) {
            android.util.Log.e("EqualizerViewModel", "Error updating equalizer state", e)
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        val current = _bandLevels.value.toMutableList()
        if (band in current.indices) {
            equalizerController.setBandLevel(band, level)
            current[band] = level
            _bandLevels.value = current
            _selectedPresetName.value = null
            _selectedPresetIndex.value = -1
            appPrefs.setCustomBandLevels(current)
        }
    }

    fun setEqualizerPreset(index: Int) {
        if (index in _equalizerPresets.value.indices) {
            equalizerController.selectPreset(index)
            _selectedPresetIndex.value = index
            _selectedPresetName.value = _equalizerPresets.value[index]
            appPrefs.setEqualizerPresetIndex(index)
            _bandLevels.value = equalizerController.getBands()
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        appPrefs.setEqualizerEnabled(enabled)
        _equalizerEnabled.value = enabled
        equalizerController.setEnabled(enabled)
    }

    fun resetBandLevels() {
        equalizerController.resetBandLevels()
        val zeroed = List(_bandLevels.value.size) { 0 }
        _bandLevels.value = zeroed
        appPrefs.setCustomBandLevels(zeroed)
        _selectedPresetName.value = null
        _selectedPresetIndex.value = -1
    }

    fun applyUserPreset(name: String) {
        val preset = _userPresets.value.find { it.first == name }
        if (preset != null) {
            preset.second.forEachIndexed { index, level ->
                equalizerController.setBandLevel(index, level)
            }
            _bandLevels.value = preset.second
            _selectedPresetName.value = name
            _selectedPresetIndex.value = -1
            appPrefs.setCustomBandLevels(preset.second)
        }
    }

    fun removeUserPreset(name: String) {
        appPrefs.removeUserPreset(name)
        _userPresets.value = appPrefs.getUserPresets()
        if (_selectedPresetName.value == name) {
            _selectedPresetName.value = null
        }
    }

    private fun sanitizePresetName(raw: String): String {
        val cleaned = raw.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (cleaned.isBlank()) {
            return raw.trim().replace(Regex("\\s+"), " ")
        }

        val canonical =
            listOf(
                "Normal" to listOf("normal"),
                "Flat" to listOf("flat"),
                "Classical" to listOf("classical", "classic", "class"),
                "Dance" to listOf("dance"),
                "Folk" to listOf("folk"),
                "Hip Hop" to listOf("hiphop"),
                "Jazz" to listOf("jazz"),
                "Pop" to listOf("pop"),
                "Rock" to listOf("rock"),
                "Metal" to listOf("metal"),
                "Electronic" to listOf("electronic", "electro"),
                "Vocal" to listOf("vocal", "voice"),
                "Speech" to listOf("speech"),
                "Bass" to listOf("bass"),
                "Treble" to listOf("treble"),
                "Latin" to listOf("latin"),
                "Blues" to listOf("blues"),
                "Acoustic" to listOf("acoustic"),
                "Reggae" to listOf("reggae"),
                "Soul" to listOf("soul"),
                "R&B" to listOf("rnb"),
            )

        val mapped = canonical.firstOrNull { (_, needles) -> needles.any { cleaned.contains(it) } }?.first
        if (mapped != null) {
            return mapped
        }

        return raw
            .trim()
            .replace(Regex("\\s+"), " ")
            .split(" ")
            .joinToString(" ") { token ->
                token.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) {
                        ch.titlecase()
                    } else {
                        ch.toString()
                    }
                }
            }
    }

    private fun sanitizePresetNames(input: List<String>): List<String> = input.map { sanitizePresetName(it) }
}

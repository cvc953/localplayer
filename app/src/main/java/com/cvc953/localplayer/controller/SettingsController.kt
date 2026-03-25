package com.cvc953.localplayer.controller

import com.cvc953.localplayer.model.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsController {
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    fun updateTheme(theme: String) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateOtherSetting(
        key: String,
        value: Any,
    ) {
        // lógica para actualizar otros ajustes
    }
}

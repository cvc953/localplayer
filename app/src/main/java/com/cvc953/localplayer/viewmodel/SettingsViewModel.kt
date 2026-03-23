package com.cvc953.localplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.SettingsController
import com.cvc953.localplayer.model.Settings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsController: SettingsController = SettingsController(),
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsController.settings

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            settingsController.updateTheme(theme)
        }
    }

    fun updateOtherSetting(
        key: String,
        value: Any,
    ) {
        viewModelScope.launch {
            settingsController.updateOtherSetting(key, value)
        }
    }

}

package com.cvc953.localplayer

import LocalPlayerTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.ui.MainMusicScreen
import com.cvc953.localplayer.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel by viewModels()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme =
                when (themeMode.lowercase()) {
                    "oscuro", "dark" -> true
                    "claro", "light" -> false
                    else -> isSystemInDarkTheme()
                }
            LocalPlayerTheme(darkTheme = darkTheme) {
                // Configuración global de barra de estado
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as? android.app.Activity)?.window
                    window?.statusBarColor = if (darkTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    androidx.core.view.WindowCompat.getInsetsController(window!!, view)?.isAppearanceLightStatusBars = !darkTheme
                }
                MainMusicScreen { }
            }
        }
    }
}

package com.cvc953.localplayer

import LocalPlayerTheme
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.screens.MainMusicScreenUpdated
import com.cvc953.localplayer.ui.theme.resolvePrimaryColor
import com.cvc953.localplayer.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply language preference by updating resources configuration
        applyLanguagePreference()

        setContent {
            val viewModel: MainViewModel by viewModels()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val languageChangeVersion by viewModel.languageChangeVersion.collectAsStateWithLifecycle()
            
            // Trigger recomposition when language changes
            val darkTheme =
                when (themeMode.lowercase()) {
                    "oscuro", "dark" -> true
                    "claro", "light" -> false
                    else -> isSystemInDarkTheme()
                }
            val primaryColorHex by viewModel.primaryColorHex.collectAsStateWithLifecycle()
            val primaryColor = resolvePrimaryColor(primaryColorHex).color
            LocalPlayerTheme(darkTheme = darkTheme, primaryColor = primaryColor) {
                // Configuración global de barra de estado
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as? android.app.Activity)?.window
                    window?.statusBarColor = if (darkTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    androidx.core.view.WindowCompat
                        .getInsetsController(window!!, view)
                        ?.isAppearanceLightStatusBars = !darkTheme
                }
                MainMusicScreenUpdated { }
            }
        }
    }

    private fun applyLanguagePreference() {
        val appPrefs = AppPrefs(this)
        val languageCode = appPrefs.getLanguage()
        android.util.Log.d("MainActivity", "applyLanguagePreference: languageCode = $languageCode")

        if (languageCode != "sistema") {
            val locale = when (languageCode) {
                "es" -> Locale("es")
                "en" -> Locale("en")
                "it" -> Locale("it")
                else -> Locale.getDefault()
            }
            android.util.Log.d("MainActivity", "Applying locale: $locale")

            // Apply locale to this activity's resources
            val config = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                config.setLocale(locale)
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            resources.updateConfiguration(config, resources.displayMetrics)
            android.util.Log.d("MainActivity", "Locale applied to resources")
        }
    }
}

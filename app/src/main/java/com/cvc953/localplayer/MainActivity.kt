package com.cvc953.localplayer

import LocalPlayerTheme
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.screens.MainMusicScreenUpdated
import com.cvc953.localplayer.ui.theme.resolvePrimaryColor
import com.cvc953.localplayer.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        // Apply language preference early in the context hierarchy
        if (newBase != null) {
            val appPrefs = AppPrefs(newBase)
            val languageCode = appPrefs.getLanguage()
            val localeList =
                when (languageCode) {
                    "es" -> LocaleListCompat.create(Locale("es"))
                    "en" -> LocaleListCompat.create(Locale("en"))
                    "it" -> LocaleListCompat.create(Locale("it"))
                    else -> LocaleListCompat.getEmptyLocaleList() // system default
                }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
        super.attachBaseContext(newBase)
    }

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
}

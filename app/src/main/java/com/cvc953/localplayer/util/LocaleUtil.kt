package com.cvc953.localplayer.util

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtil {
    fun setAppLanguage(
        context: Context,
        languageCode: String,
    ) {
        val localeList =
            when (languageCode) {
                "es" -> LocaleListCompat.create(Locale("es"))
                "en" -> LocaleListCompat.create(Locale("en"))
                "it" -> LocaleListCompat.create(Locale("it"))
                else -> LocaleListCompat.getEmptyLocaleList() // sistema (default)
            }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getLocaleName(languageCode: String): String =
        when (languageCode) {
            "es" -> "Español"
            "en" -> "English"
            "it" -> "Italiano"
            else -> "Sistema"
        }
}

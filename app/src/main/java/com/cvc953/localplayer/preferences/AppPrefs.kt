package com.cvc953.localplayer.preferences

import android.content.Context

class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(
        "localplayer_prefs",
        Context.MODE_PRIVATE
    )

    fun isFirstScanDone(): Boolean =
        prefs.getBoolean("first_scan_done", false)

    fun setFirstScanDone() {
        prefs.edit().putBoolean("first_scan_done", true).apply()
    }
}

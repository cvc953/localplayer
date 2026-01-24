package com.cvc953.localplayer

import android.app.Application
import com.cvc953.localplayer.util.createNotificationChannel

class LocalPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }
}

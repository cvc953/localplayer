package com.cvc953.localplayer

import android.app.Application
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.util.CrashReporter

class LocalPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Lo primero: cualquier crash posterior tiene que quedar registrado.
        CrashReporter.install(this)
        // Locale will be applied in MainActivity.onCreate()
        SongRepository.getInstance(this).startObserving()
    }
}

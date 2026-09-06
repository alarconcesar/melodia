package com.melodia.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MelodiaApp : Application() {
    lateinit var appContainer: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val ch = NotificationChannel("melodia_playback", "Reproducción", NotificationManager.IMPORTANCE_LOW)
                val nm = getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(ch)
            } catch (_: Exception) {}
        }
        appContainer = AppContainer(this)
    }
}

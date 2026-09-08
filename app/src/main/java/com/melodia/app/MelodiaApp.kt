package com.melodia.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MelodiaApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }

    override fun onCreate() {
        super.onCreate()
        // Atrapa cualquier crash y lo guarda para mostrarlo al reabrir
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = java.io.StringWriter()
                e.printStackTrace(java.io.PrintWriter(sw))
                openFileOutput(CRASH_FILE, MODE_PRIVATE).use {
                    it.write("Hilo: ${t.name}\n${sw}".toByteArray())
                }
            } catch (_: Exception) {}
            prev?.uncaughtException(t, e)
        }
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

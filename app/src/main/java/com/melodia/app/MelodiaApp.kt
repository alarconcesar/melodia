package com.melodia.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MelodiaApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    /** Detalle si el contenedor de datos no pudo prepararse (se muestra en pantalla). */
    var containerError: String? = null
        private set

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }

    override fun onCreate() {
        super.onCreate()
        // Atrapa cualquier crash y lo guarda para mostrarlo al reabrir
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val sw = java.io.StringWriter()
            try { e.printStackTrace(java.io.PrintWriter(sw)) } catch (_: Exception) {}
            val trace = "Hilo: ${'$'}{t.name}\n${'$'}sw"
            try {
                openFileOutput(CRASH_FILE, MODE_PRIVATE).use { it.write(trace.toByteArray()) }
            } catch (_: Exception) {}
            // Mejor esfuerzo: abrir la pantalla de error ya mismo, sin esperar a reabrir.
            // El guardia de tiempo evita un bucle de avisos si el fallo es del propio Application.
            try {
                val sp = getSharedPreferences("melodia_safe", MODE_PRIVATE)
                val ahora = System.currentTimeMillis()
                if (ahora - sp.getLong("last_crash_ui", 0L) > 15_000L) {
                    sp.edit().putLong("last_crash_ui", ahora).apply()
                    startActivity(
                        android.content.Intent(this, CrashActivity::class.java)
                            .putExtra("trace", trace)
                            .addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                    )
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
        // No reventar el arranque en silencio: si el contenedor falla, se reporta
        try {
            appContainer = AppContainer(this)
        } catch (t: Throwable) {
            containerError = "Fallo al preparar la app:\n${'$'}{t.stackTraceToString()}"
        }
    }
}

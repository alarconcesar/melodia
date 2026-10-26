package com.melodia.app.player

/**
 * Posición interpolada entre muestras del reproductor.
 *
 * ExoPlayer se consulta cada 500 ms; entre muestra y muestra la UI puede
 * avanzar el tiempo con `elapsedRealtime()` sin tocar el player (menos JNI y
 * seekbar fluida). Es lógica pura para poder fijarla con tests: un error acá
 * hace que la barra se adelante o se quede clavada al cambiar de velocidad.
 */
object PlaybackClock {

    fun positionMs(
        positionMs: Long,
        snapshotElapsedMs: Long,
        nowElapsedMs: Long,
        isPlaying: Boolean,
        durationMs: Long,
        speed: Float = 1f
    ): Long {
        val base = positionMs.coerceAtLeast(0L)
        if (!isPlaying) return clamp(base, durationMs)
        val elapsed = (nowElapsedMs - snapshotElapsedMs).coerceAtLeast(0L)
        val advanced = (elapsed * speed.coerceIn(0.25f, 2f)).toLong()
        return clamp(base + advanced, durationMs)
    }

    fun progress(positionMs: Long, durationMs: Long): Float =
        if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    private fun clamp(value: Long, durationMs: Long): Long =
        if (durationMs > 0) value.coerceIn(0L, durationMs) else value.coerceAtLeast(0L)
}

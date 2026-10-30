package com.melodia.app.perf

import android.view.Choreographer

/**
 * Medidor de fluidez sin dependencias: escucha el Choreographer, guarda los
 * últimos ~15 s de intervalos entre frames y los etiqueta con la pantalla
 * activa. Permite decir «Inicio va a 48 fps» sin adb ni Perfetto.
 *
 * Se arranca una sola vez desde MainActivity; no anima nada ni retiene Activity.
 */
object FrameMonitor {
    private const val CAP = 900
    private val intervals = LongArray(CAP)
    private val tags = arrayOfNulls<String>(CAP)
    private var count = 0
    private var index = 0
    private var lastNanos = 0L
    @Volatile private var running = false

    @Volatile
    private var current = "Inicio"
    @Volatile
    private var callback: Choreographer.FrameCallback? = null

    fun start() {
        if (running) return
        running = true
        val choreographer = Choreographer.getInstance()
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastNanos > 0L) {
                    val ms = (frameTimeNanos - lastNanos) / 1_000_000
                    // Descartar pausas (pantalla apagada, app en segundo plano).
                    if (ms in 1..500) {
                        intervals[index] = ms
                        tags[index] = current
                        index = (index + 1) % CAP
                        if (count < CAP) count++
                    }
                }
                lastNanos = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        }
        callback = cb
        choreographer.postFrameCallback(cb)
    }

    /** Detiene la medición (antes era imposible: el callback se re-registraba solo). */
    fun stop() {
        try {
            callback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        } catch (_: Exception) {}
        callback = null
        running = false
    }

    /** Marca la pantalla que se está usando (Inicio, Explorar, Reproductor…). */
    fun screen(name: String) {
        current = name
    }

    fun reset() {
        count = 0
        index = 0
        lastNanos = 0L
    }

    data class ScreenStats(
        val screen: String,
        val fps: Int,
        val slowPct: Int,
        val worstMs: Long,
        val frames: Int
    )

    /** Resumen global de la ventana. */
    fun overall(): ScreenStats {
        if (count == 0) return ScreenStats("Todo", 0, 0, 0, 0)
        var sum = 0L
        var slow = 0
        var worst = 0L
        for (i in 0 until count) {
            val v = intervals[i]
            sum += v
            if (v > 18) slow++
            if (v > worst) worst = v
        }
        val avg = sum.toFloat() / count
        return ScreenStats("Todo", fps(avg), slow * 100 / count, worst, count)
    }

    /** Una entrada por pantalla, la peor primero. */
    fun byScreen(): List<ScreenStats> {
        if (count == 0) return emptyList()
        val sum = HashMap<String, Long>()
        val slow = HashMap<String, Int>()
        val worst = HashMap<String, Long>()
        val frames = HashMap<String, Int>()
        for (i in 0 until count) {
            val k = tags[i] ?: "—"
            val v = intervals[i]
            sum[k] = (sum[k] ?: 0L) + v
            frames[k] = (frames[k] ?: 0) + 1
            if (v > 18) slow[k] = (slow[k] ?: 0) + 1
            if (v > (worst[k] ?: 0L)) worst[k] = v
        }
        return frames.map { (k, n) ->
            ScreenStats(k, fps(sum[k]!!.toFloat() / n), (slow[k] ?: 0) * 100 / n, worst[k] ?: 0L, n)
        }.filter { it.frames >= 20 }.sortedByDescending { it.slowPct }
    }

    private fun fps(avgMs: Float): Int =
        if (avgMs > 0f) (1000f / avgMs).toInt().coerceIn(0, 144) else 0
}

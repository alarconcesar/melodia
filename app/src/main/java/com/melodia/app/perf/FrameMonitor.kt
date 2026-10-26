package com.melodia.app.perf

import android.view.Choreographer

/**
 * Medidor de fluidez sin dependencias: escucha el Choreographer y guarda los
 * últimos ~15 s de intervalos entre frames. Sirve para saber si la app va a
 * 60 fps o si hay jank, sin adb ni Perfetto.
 *
 * Se arranca una sola vez desde MainActivity; no anima nada ni retiene Activity.
 */
object FrameMonitor {
    private const val CAP = 900
    private val intervals = LongArray(CAP)
    private var count = 0
    private var index = 0
    private var lastNanos = 0L
    private var running = false

    fun start() {
        if (running) return
        running = true
        val choreographer = Choreographer.getInstance()
        choreographer.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastNanos > 0L) {
                    val ms = (frameTimeNanos - lastNanos) / 1_000_000
                    // Descartar pausas (pantalla apagada, app en segundo plano) para no ensuciar la media.
                    if (ms in 1..500) {
                        intervals[index] = ms
                        index = (index + 1) % CAP
                        if (count < CAP) count++
                    }
                }
                lastNanos = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        })
    }

    data class Stats(val fps: Int, val slowPct: Int, val worstMs: Long, val samples: Int)

    fun stats(): Stats {
        if (count == 0) return Stats(0, 0, 0, 0)
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
        val fps = if (avg > 0f) (1000f / avg).toInt().coerceIn(0, 120) else 0
        return Stats(fps, slow * 100 / count, worst, count)
    }
}

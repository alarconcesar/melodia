package com.melodia.app

import com.melodia.app.player.PlaybackClock
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La barra de progreso se interpola entre muestras del player (cada 500 ms).
 * Si la aritmética falla, se adelanta, se queda clavada o pasa del final.
 */
class PlaybackClockTest {

    @Test
    fun enPausaLaPosicionNoAvanza() {
        assertEquals(30_000L, PlaybackClock.positionMs(30_000, 1_000, 9_000, isPlaying = false, 180_000))
    }

    @Test
    fun sonandoAvanzaConElReloj() {
        // 2 s desde la muestra a velocidad normal
        assertEquals(32_000L, PlaybackClock.positionMs(30_000, 1_000, 3_000, isPlaying = true, 180_000))
    }

    @Test
    fun laVelocidadMultiplicaElAvance() {
        // 2 s reales a 1.5x = 3 s de audio
        assertEquals(33_000L, PlaybackClock.positionMs(30_000, 1_000, 3_000, isPlaying = true, 180_000, speed = 1.5f))
    }

    @Test
    fun nuncaPasaDeLaDuracion() {
        assertEquals(180_000L, PlaybackClock.positionMs(179_000, 0, 60_000, isPlaying = true, 180_000))
    }

    @Test
    fun relojAtrasadoNoRetrocede() {
        assertEquals(30_000L, PlaybackClock.positionMs(30_000, 5_000, 1_000, isPlaying = true, 180_000))
    }

    @Test
    fun duracionDesconocidaNoRompe() {
        // Sin duración conocida no hay tope: sigue avanzando con el reloj
        assertEquals(43_000L, PlaybackClock.positionMs(42_000, 0, 1_000, isPlaying = true, 0))
        assertEquals(0f, PlaybackClock.progress(42_000, 0), 0.0001f)
    }

    @Test
    fun progresoAcotadoEntreCeroYUno() {
        assertEquals(0f, PlaybackClock.progress(0, 0), 0.0001f)
        assertEquals(0.5f, PlaybackClock.progress(90_000, 180_000), 0.0001f)
        assertEquals(1f, PlaybackClock.progress(200_000, 180_000), 0.0001f)
    }
}

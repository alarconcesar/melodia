package com.melodia.app

import com.melodia.app.data.online.StreamUrlExpiry
import com.melodia.app.data.online.YtErrors
import com.melodia.app.player.PlaybackClock
import com.melodia.app.player.QueueOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bordes de la lógica pura que la UI usa a diario: reloj de reproducción,
 * expiración de URLs, traducción de errores y aritmética de la cola.
 */
class LogicEdgesTest {

    // ── PlaybackClock ──
    @Test
    fun enPausaLaPosicionNoAvanza() {
        val pos = PlaybackClock.positionMs(
            positionMs = 30_000, snapshotElapsedMs = 0, nowElapsedMs = 10_000,
            isPlaying = false, durationMs = 200_000, speed = 1f
        )
        assertEquals(30_000L, pos)
    }

    @Test
    fun laVelocidadEscalaElAvance() {
        val pos = PlaybackClock.positionMs(
            positionMs = 0, snapshotElapsedMs = 0, nowElapsedMs = 2_000,
            isPlaying = true, durationMs = 200_000, speed = 2f
        )
        assertEquals(4_000L, pos)
    }

    @Test
    fun noPasaDeLaDuracion() {
        val pos = PlaybackClock.positionMs(
            positionMs = 199_000, snapshotElapsedMs = 0, nowElapsedMs = 60_000,
            isPlaying = true, durationMs = 200_000, speed = 1f
        )
        assertEquals(200_000L, pos)
    }

    @Test
    fun duracionCeroNoRompe() {
        val p = PlaybackClock.progress(5_000, 0)
        assertEquals(0f, p, 0.0001f)
    }

    @Test
    fun velocidadCeroSeTrataComoElMinimo() {
        // El reloj acota la velocidad a 0.25–2.0: una velocidad inválida no debe
        // congelar la barra ni adelantarla.
        val pos = PlaybackClock.positionMs(
            positionMs = 1_000, snapshotElapsedMs = 0, nowElapsedMs = 5_000,
            isPlaying = true, durationMs = 100_000, speed = 0f
        )
        assertEquals(2_250L, pos)
    }

    // ── StreamUrlExpiry ──
    @Test
    fun sinParametroExpireUsaElTechoPropio() {
        val ahora = 1_000_000L
        assertEquals(ahora + StreamUrlExpiry.DEFAULT_TTL_MS, StreamUrlExpiry.expiryEpochMs("https://x/y", ahora))
    }

    @Test
    fun respetaElExpireDeLaUrlSiEsMenor() {
        val ahora = 1_000L
        val url = "https://x/y?expire=1000&otro=1"
        assertEquals(1_000_000L, StreamUrlExpiry.expiryEpochMs(url, ahora))
    }

    @Test
    fun expireAbsurdoSeRecortaAlTecho() {
        val ahora = 1_000L
        val url = "https://x/y?expire=99999999999"
        assertEquals(ahora + StreamUrlExpiry.DEFAULT_TTL_MS, StreamUrlExpiry.expiryEpochMs(url, ahora))
    }

    @Test
    fun expireIlegibleNoRevienta() {
        val ahora = 5L
        assertEquals(ahora + StreamUrlExpiry.DEFAULT_TTL_MS, StreamUrlExpiry.expiryEpochMs("https://x?expire=abc", ahora))
    }

    @Test
    fun yaVencidaSeDetecta() {
        assertTrue(StreamUrlExpiry.isExpired("https://x?expire=10", cachedAtMs = 0, nowMs = 20_000))
        assertFalse(StreamUrlExpiry.isExpired("https://x?expire=100", cachedAtMs = 0, nowMs = 20_000))
    }

    // ── YtErrors ──
    @Test
    fun errorDesconocidoDaMensajeGenerico() {
        val msg = YtErrors.message(IllegalStateException("boom"), "No se pudo")
        assertTrue("no debe filtrar la clase: $msg", !msg.contains("IllegalState"))
        assertTrue(msg.isNotBlank())
    }

    // ── QueueOps ──
    @Test
    fun moverFueraDeRangoDevuelveNull() {
        val l = listOf(1L, 2L, 3L)
        assertEquals(null, QueueOps.move(l, 0, 9))
        assertEquals(null, QueueOps.move(l, 9, 0))
        assertEquals(null, QueueOps.move(emptyList<Long>(), 0, 1))
        assertEquals(null, QueueOps.move(l, 1, 1))
    }

    @Test
    fun extremosDeLaCola() {
        assertFalse(QueueOps.canMoveUp(0, 3))
        assertFalse(QueueOps.canMoveDown(2, 3))
        assertTrue(QueueOps.canMoveUp(2, 3))
        assertTrue(QueueOps.canMoveDown(0, 3))
        // índices inválidos no deben habilitar acciones
        assertFalse(QueueOps.canMoveUp(-1, 3))
        assertFalse(QueueOps.canMoveDown(5, 3))
    }

    @Test
    fun vaciarConservandoElActual() {
        assertEquals(listOf(2L), QueueOps.keepOnly(listOf(1L, 2L, 3L), 1))
        assertEquals(emptyList<Long>(), QueueOps.keepOnly(listOf(1L), 9))
    }
}

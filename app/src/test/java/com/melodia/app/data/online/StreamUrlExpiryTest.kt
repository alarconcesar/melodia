package com.melodia.app.data.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Antes se cacheaba la URL de audio 5 h fijas: si YouTube la vencía antes,
 * el play siguiente daba 403. Ahora manda el `expire` de la propia URL.
 */
class StreamUrlExpiryTest {

    private val now = 1_000_000_000_000L // epoch ms de referencia

    @Test
    fun sinParametroExpireUsaElTechoPropio() {
        val url = "https://rr1---sn.googlevideo.com/videoplayback?itag=140&id=abc"
        assertEquals(now + StreamUrlExpiry.DEFAULT_TTL_MS, StreamUrlExpiry.expiryEpochMs(url, now))
    }

    @Test
    fun respetaElExpireDeLaUrl() {
        val expira = (now / 1000) + 3600 // 1 h
        val url = "https://x/videoplayback?expire=$expira&itag=140"
        assertEquals(expira * 1000, StreamUrlExpiry.expiryEpochMs(url, now))
    }

    @Test
    fun expireMuyLejanoSeCapaAlTecho() {
        val expira = (now / 1000) + 48 * 3600
        val url = "https://x/videoplayback?expire=$expira&itag=140"
        assertEquals(now + StreamUrlExpiry.DEFAULT_TTL_MS, StreamUrlExpiry.expiryEpochMs(url, now))
    }

    @Test
    fun expireEnElPasadoQuedaVencido() {
        val expira = (now / 1000) - 10
        val url = "https://x/videoplayback?expire=$expira"
        assertTrue(StreamUrlExpiry.isExpired(url, cachedAtMs = now, nowMs = now))
        assertFalse(StreamUrlExpiry.isExpired(url, cachedAtMs = now, nowMs = now - 20_000))
    }

    @Test
    fun valorRaroNoRompe() {
        val url = "https://x/videoplayback?expire=abc&itag=140"
        assertEquals(now + StreamUrlExpiry.DEFAULT_TTL_MS, StreamUrlExpiry.expiryEpochMs(url, now))
    }
}

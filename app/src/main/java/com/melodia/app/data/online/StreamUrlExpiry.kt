package com.melodia.app.data.online

/**
 * Expiración real de las URLs de audio de YouTube.
 *
 * La URL trae `expire=<epoch segundos>`; antes se cacheaba 5 h fijas y una URL
 * vencida daba 403 al reanudar. Se toma el mínimo entre ese valor y un techo
 * propio (las URLs de googlevideo suelen valer ~6 h).
 */
object StreamUrlExpiry {

    const val DEFAULT_TTL_MS = 5 * 60 * 60 * 1000L
    private val EXPIRE = Regex("[?&]expire=(\\d+)")

    fun expiryEpochMs(url: String, nowMs: Long, maxTtlMs: Long = DEFAULT_TTL_MS): Long {
        val cap = nowMs + maxTtlMs
        val fromUrl = EXPIRE.find(url)?.groupValues?.get(1)?.toLongOrNull()?.times(1000L)
            ?: return cap
        return minOf(fromUrl, cap)
    }

    fun isExpired(url: String, cachedAtMs: Long, nowMs: Long): Boolean =
        nowMs >= expiryEpochMs(url, cachedAtMs)
}

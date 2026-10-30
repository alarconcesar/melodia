package com.melodia.app.data

import com.melodia.app.data.local.MelodiaDatabase
import com.melodia.app.data.local.StatEntity
import kotlinx.coroutines.flow.Flow

class StatsRepository(private val db: MelodiaDatabase) {
    fun observe(): Flow<List<StatEntity>> = db.statDao().observeAll()

    /**
     * Registra una escucha REAL: solo si sonó al menos 30 s (o la mitad en temas
     * cortos). Antes sumaba 1 play + la duración completa al primer segundo, y
     * saltar 10 temas de 5 min daba +10 plays y +50 min en 10 s.
     */
    suspend fun recordHeard(songId: Long, listenedMs: Long, durationMs: Long) {
        try {
            val umbral = if (durationMs > 0) minOf(30_000L, durationMs / 2) else 30_000L
            if (listenedMs < umbral) return
            // Tope: lo escuchado no pasa la duración (repeat-one cuenta por
            // vueltas, no acumulando horas en un solo play).
            val ms = if (durationMs > 0) listenedMs.coerceIn(0, durationMs) else listenedMs.coerceAtLeast(0)
            db.statDao().heard(songId, System.currentTimeMillis(), ms)
        } catch (_: Exception) {}
    }

    suspend fun clear() { try { db.statDao().clear() } catch (_: Exception) {} }
}

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
            val umbral = minOf(30_000L, (durationMs / 2).coerceAtLeast(0))
            if (listenedMs < umbral) return
            db.statDao().heard(songId, System.currentTimeMillis(), listenedMs.coerceAtLeast(0))
        } catch (_: Exception) {}
    }

    suspend fun clear() { try { db.statDao().clear() } catch (_: Exception) {} }
}

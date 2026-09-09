package com.melodia.app.data

import com.melodia.app.data.local.MelodiaDatabase
import com.melodia.app.data.local.StatEntity
import kotlinx.coroutines.flow.Flow

class StatsRepository(private val db: MelodiaDatabase) {
    fun observe(): Flow<List<StatEntity>> = db.statDao().observeAll()

    /** Registra el inicio de una reproducción (suma 1 play + duración aprox.). */
    suspend fun recordPlay(songId: Long, durationMs: Long) {
        try {
            val cur = db.statDao().get(songId)
            db.statDao().upsert(
                StatEntity(
                    songId = songId,
                    plays = (cur?.plays ?: 0) + 1,
                    lastPlayedAt = System.currentTimeMillis(),
                    listenMs = (cur?.listenMs ?: 0L) + durationMs.coerceAtLeast(0)
                )
            )
        } catch (_: Exception) {}
    }

    suspend fun clear() { try { db.statDao().clear() } catch (_: Exception) {} }
}

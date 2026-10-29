package com.melodia.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatDao {
    @Query("SELECT * FROM stats") fun observeAll(): Flow<List<StatEntity>>
    @Query("SELECT * FROM stats WHERE songId = :id") suspend fun get(id: Long): StatEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: StatEntity)
    /**
     * Suma atómica en una sola sentencia: el get+upsert por separado perdía
     * reproducciones al saltar temas rápido.
     */
    @Query("INSERT INTO stats(songId, plays, lastPlayedAt, listenMs) VALUES(:id, 1, :now, :ms) ON CONFLICT(songId) DO UPDATE SET plays = plays + 1, lastPlayedAt = :now, listenMs = listenMs + :ms")
    suspend fun heard(id: Long, now: Long, ms: Long)
    @Query("DELETE FROM stats") suspend fun clear()
}

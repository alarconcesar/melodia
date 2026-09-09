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
    @Query("DELETE FROM stats") suspend fun clear()
}

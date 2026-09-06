package com.melodia.app.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") fun observeAll(): Flow<List<PlaylistEntity>>
    @Insert suspend fun insert(p: PlaylistEntity): Long
    @Query("DELETE FROM playlists WHERE id=:id") suspend fun delete(id: Long)
    @Query("UPDATE playlists SET name=:name WHERE id=:id") suspend fun rename(id: Long, name: String)
}

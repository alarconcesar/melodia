package com.melodia.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY pinned DESC, createdAt DESC") fun observeAll(): Flow<List<PlaylistEntity>>
    @Query("SELECT * FROM playlists ORDER BY pinned DESC, createdAt DESC") suspend fun getAll(): List<PlaylistEntity>
    @Insert suspend fun insert(p: PlaylistEntity): Long
    @Query("DELETE FROM playlists WHERE id=:id") suspend fun delete(id: Long)
    @Query("UPDATE playlists SET name=:name WHERE id=:id") suspend fun rename(id: Long, name: String)
    @Query("UPDATE playlists SET coverAlbumId=:albumId WHERE id=:id") suspend fun setCover(id: Long, albumId: Long?)
    @Query("UPDATE playlists SET pinned=CASE WHEN pinned=1 THEN 0 ELSE 1 END WHERE id=:id") suspend fun togglePin(id: Long)
}

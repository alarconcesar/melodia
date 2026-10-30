package com.melodia.app.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface PlaylistSongDao {
    @Query("SELECT * FROM playlist_songs WHERE playlistId=:pid ORDER BY position ASC") fun observeSongs(pid: Long): Flow<List<PlaylistSongEntity>>
    @Query("SELECT * FROM playlist_songs WHERE playlistId=:pid ORDER BY position ASC") suspend fun getSongs(pid: Long): List<PlaylistSongEntity>
    @Query("SELECT * FROM playlist_songs") fun observeAllSongs(): Flow<List<PlaylistSongEntity>>
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId=:pid AND songId=:sid") suspend fun exists(pid: Long, sid: Long): Int
    @Query("UPDATE playlist_songs SET position=:pos WHERE playlistId=:pid AND songId=:sid") suspend fun updatePos(pid: Long, sid: Long, pos: Int)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(e: PlaylistSongEntity)
    @Query("DELETE FROM playlist_songs WHERE playlistId=:pid AND songId=:sid") suspend fun remove(pid: Long, sid: Long)
    @Query("DELETE FROM playlist_songs WHERE playlistId=:pid") suspend fun clear(pid: Long)
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId=:pid") suspend fun count(pid: Long): Int
    @Query("SELECT songId FROM playlist_songs WHERE playlistId=:pid") suspend fun idsIn(pid: Long): List<Long>
}

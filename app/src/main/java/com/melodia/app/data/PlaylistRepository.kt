package com.melodia.app.data
import com.melodia.app.data.local.*
import com.melodia.app.data.model.Song
import kotlinx.coroutines.flow.Flow
class PlaylistRepository(private val db: MelodiaDatabase) {
    fun playlists(): Flow<List<PlaylistEntity>> = db.playlistDao().observeAll()
    fun playlistSongs(pid: Long): Flow<List<PlaylistSongEntity>> = db.playlistSongDao().observeSongs(pid)
    fun favorites(): Flow<List<FavoriteEntity>> = db.favoriteDao().observeAll()
    fun isFav(id: Long): Flow<Boolean> = db.favoriteDao().isFav(id)
    suspend fun createPlaylist(name: String): Long = db.playlistDao().insert(PlaylistEntity(name = name.trim()))
    suspend fun deletePlaylist(id: Long) { db.playlistSongDao().clear(id); db.playlistDao().delete(id) }
    suspend fun rename(id: Long, name: String) = db.playlistDao().rename(id, name)
    suspend fun addToPlaylist(pid: Long, s: Song) {
        val pos = db.playlistSongDao().count(pid)
        db.playlistSongDao().add(PlaylistSongEntity(pid, s.id, pos, s.dataPath, s.title, s.artist, s.album, s.durationMs))
    }
    suspend fun removeFromPlaylist(pid: Long, sid: Long) = db.playlistSongDao().remove(pid, sid)
    suspend fun toggleFav(id: Long, isFav: Boolean) { if (isFav) db.favoriteDao().remove(id) else db.favoriteDao().add(FavoriteEntity(id)) }
}

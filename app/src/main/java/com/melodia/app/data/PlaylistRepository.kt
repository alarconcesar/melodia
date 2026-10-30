package com.melodia.app.data
import com.melodia.app.data.local.*
import com.melodia.app.data.model.Song
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
class PlaylistRepository(private val db: MelodiaDatabase) {
    fun playlists(): Flow<List<PlaylistEntity>> = db.playlistDao().observeAll()
    fun playlistSongs(pid: Long): Flow<List<PlaylistSongEntity>> = db.playlistSongDao().observeSongs(pid)
    fun favorites(): Flow<List<FavoriteEntity>> = db.favoriteDao().observeAll()
    fun isFav(id: Long): Flow<Boolean> = db.favoriteDao().isFav(id)
    suspend fun createPlaylist(name: String): Long = db.playlistDao().insert(PlaylistEntity(name = name.trim()))
    @Transaction
    suspend fun deletePlaylist(id: Long) { db.playlistSongDao().clear(id); db.playlistDao().delete(id) }
    suspend fun rename(id: Long, name: String) = db.playlistDao().rename(id, name)
    suspend fun setCover(id: Long, albumId: Long?) = db.playlistDao().setCover(id, albumId)
    suspend fun togglePin(id: Long) = db.playlistDao().togglePin(id)
    @Transaction
    suspend fun addToPlaylist(pid: Long, s: Song): Boolean {
        if (db.playlistSongDao().exists(pid, s.id) > 0) return false // sin duplicados
        val pos = try { db.playlistSongDao().maxPos(pid) + 1 } catch (_: Exception) { 0 }
        db.playlistSongDao().add(PlaylistSongEntity(pid, s.id, pos, s.dataPath, s.title, s.artist, s.album, s.durationMs))
        return true
    }
    /** Agrega en lote: una transacción, un conteo, sin exists por tema. */
    @Transaction
    suspend fun addAllToPlaylist(pid: Long, songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        val ya = try {
            db.playlistSongDao().idsIn(pid).toSet()
        } catch (_: Exception) { emptySet() }
        val base = try { db.playlistSongDao().maxPos(pid) + 1 } catch (_: Exception) { 0 }
        val nuevos = songs.filter { !ya.contains(it.id) }
        nuevos.forEachIndexed { i, s ->
            db.playlistSongDao().add(
                PlaylistSongEntity(pid, s.id, base + i, s.dataPath, s.title, s.artist, s.album, s.durationMs)
            )
        }
        return nuevos.size
    }

    /** Mueve un tema dentro de la playlist (up = hacia arriba). */
    @Transaction
    suspend fun moveSong(pid: Long, sid: Long, up: Boolean) {
        val dao = db.playlistSongDao()
        val list = dao.getSongs(pid)
        val i = list.indexOfFirst { it.songId == sid }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        val a = list[i]; val b = list[j]
        dao.updatePos(pid, a.songId, j)
        dao.updatePos(pid, b.songId, i)
    }
    fun allPlaylistSongs(): Flow<List<PlaylistSongEntity>> = db.playlistSongDao().observeAllSongs()
    suspend fun removeFromPlaylist(pid: Long, sid: Long) = db.playlistSongDao().remove(pid, sid)
    private val favMutex = Mutex()
    /** Toggle serializado con lectura fresca: el doble tap ya no se come uno. */
    suspend fun toggleFavFresh(id: Long) {
        favMutex.withLock {
            if (db.favoriteDao().existsNow(id)) db.favoriteDao().remove(id)
            else db.favoriteDao().add(FavoriteEntity(id))
        }
    }
    suspend fun toggleFav(id: Long, isFav: Boolean) { if (isFav) db.favoriteDao().remove(id) else db.favoriteDao().add(FavoriteEntity(id)) }

    /** Clave estable de un tema entre equipos (minúsculas, espacios colapsados). */
    fun favKey(artist: String, title: String): String {
        fun limpio(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")
        return limpio(artist) + "|" + limpio(title)
    }

    /** Exporta playlists + favoritos a JSON. */
    suspend fun exportJson(songs: List<Song> = emptyList()): String {
        val pls = db.playlistDao().getAll()
        val arr = org.json.JSONArray()
        pls.forEach { p ->
            val songs = db.playlistSongDao().getSongs(p.id)
            val sarr = org.json.JSONArray()
            songs.forEach { e ->
                sarr.put(
                    org.json.JSONObject()
                        .put("path", e.dataPath).put("title", e.title)
                        .put("artist", e.artist).put("album", e.album)
                        .put("dur", e.durationMs)
                )
            }
            arr.put(org.json.JSONObject().put("name", p.name).put("songs", sarr))
        }
        // Favoritos con clave estable (artista|título) Y ADEMÁS el id: el ID
        // cambia entre teléfonos y la clave falla si el artista se corrigió
        // después (p. ej. repairDownloadMeta); con ambos, uno rescata al otro.
        val byId = songs.associateBy { it.id }
        val favKeys = org.json.JSONArray()
        val favIds = org.json.JSONArray()
        db.favoriteDao().getAllIds().forEach { id ->
            favIds.put(id)
            byId[id]?.let { favKeys.put(favKey(it.artist, it.title)) }
        }
        return org.json.JSONObject().put("playlists", arr).put("favs", favKeys).put("favIds", favIds).toString()
    }
}

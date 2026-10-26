package com.melodia.app.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.melodia.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class MusicRepository(context: Context) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("melodia_lib", Context.MODE_PRIVATE)

    private val _minSecs = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("min_secs", 30))
    val minSecs: kotlinx.coroutines.flow.StateFlow<Int> = _minSecs
    private val _scanFolder = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("scan_folder", null))
    val scanFolder: kotlinx.coroutines.flow.StateFlow<String?> = _scanFolder

    fun setMinSecs(s: Int) {
        val v = s.coerceIn(0, 300)
        prefs.edit().putInt("min_secs", v).apply()
        _minSecs.value = v
    }

    fun setScanFolder(path: String?) {
        if (path == null) prefs.edit().remove("scan_folder").apply()
        else prefs.edit().putString("scan_folder", path).apply()
        _scanFolder.value = path
    }

    /** Edita título/artista/álbum en MediaStore. Devuelve true si se guardó. */
    fun updateMetadata(id: Long, title: String, artist: String, album: String): Boolean {
        return try {
            val uri = android.content.ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
            val cv = android.content.ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title.trim())
                put(MediaStore.Audio.Media.ARTIST, artist.trim().ifBlank { "<unknown>" })
                put(MediaStore.Audio.Media.ALBUM, album.trim())
            }
            resolver.update(uri, cv, null, null) > 0
        } catch (_: Exception) { false }
    }

    /**
     * Borra audios del dispositivo. En Android 10+ los archivos de otras apps
     * exigen confirmación del sistema: si hace falta, devuelve su IntentSender.
     */
    suspend fun deleteSongs(ids: List<Long>): IntentSender? = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext null
        val uris = ids.map { ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it) }
        try {
            uris.forEach { resolver.delete(it, null, null) }
            null
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= 30) {
                try { MediaStore.createDeleteRequest(resolver, uris).intentSender } catch (_: Exception) { null }
            } else null
        }
    }

    fun loadAllSongs(): Flow<List<Song>> = flow {
        val songs = withContext(Dispatchers.IO) { querySongs() }
        emit(songs)
    }

    private fun querySongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val minMs = _minSecs.value * 1000L
        val onlyFolder = _scanFolder.value
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK
        )
        // ALBUM_ARTIST existe desde Android 11; en versiones previas se usa el artista
        val hasAlbumArtist = android.os.Build.VERSION.SDK_INT >= 30
        if (hasAlbumArtist) cols.add(MediaStore.Audio.Media.ALBUM_ARTIST)
        try {
            resolver.query(uri, cols.toTypedArray(), MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " ASC")?.use { c ->
                val iId = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val iData = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val iAdded = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val iTrack = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val iAlbumArtist = if (hasAlbumArtist) c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1
                while (c.moveToNext()) {
                    val dur = if (iDur >= 0) c.getLong(iDur) else 0L
                    if (dur < minMs) continue
                    val path = if (iData >= 0) c.getString(iData) ?: "" else ""
                    if (onlyFolder != null && !path.startsWith(onlyFolder)) continue
                    var title = if (iTitle >= 0) c.getString(iTitle) ?: "" else ""
                    if (title.isBlank()) {
                        title = try { java.io.File(path).nameWithoutExtension.ifBlank { "Sin título" } } catch (_: Exception) { "Sin título" }
                    }
                    var artist = if (iArtist >= 0) c.getString(iArtist) ?: "" else ""
                    if (artist.isBlank() || artist == "<unknown>") artist = "Desconocido"
                    var album = if (iAlbum >= 0) c.getString(iAlbum) ?: "" else ""
                    if (album == "<unknown>") album = ""
                    // TRACK viene como disc*1000 + pista cuando hay varios discos
                    val rawTrack = if (iTrack >= 0) c.getInt(iTrack) else 0
                    val disc = if (rawTrack > 1000) rawTrack / 1000 else 1
                    val track = when {
                        rawTrack <= 0 -> 0
                        rawTrack > 1000 -> rawTrack % 1000
                        else -> rawTrack
                    }
                    val albumArtist = if (iAlbumArtist >= 0) {
                        (c.getString(iAlbumArtist) ?: "").takeUnless { it.isBlank() || it == "<unknown>" } ?: artist
                    } else artist
                    songs.add(
                        Song(
                            id = if (iId >= 0) c.getLong(iId) else 0L,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = if (iAlbumId >= 0) c.getLong(iAlbumId) else 0L,
                            durationMs = dur,
                            dataPath = if (iData >= 0) c.getString(iData) ?: "" else "",
                            dateAdded = if (iAdded >= 0) c.getLong(iAdded) else 0L,
                            trackNumber = track,
                            discNumber = if (rawTrack > 0) disc else 0,
                            albumArtist = albumArtist
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return songs
    }
}

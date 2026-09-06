package com.melodia.app.data

import android.content.Context
import android.provider.MediaStore
import com.melodia.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class MusicRepository(context: Context) {
    private val resolver = context.contentResolver

    fun loadAllSongs(): Flow<List<Song>> = flow {
        val songs = withContext(Dispatchers.IO) { querySongs() }
        emit(songs)
    }

    private fun querySongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        try {
            resolver.query(uri, cols, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " ASC")?.use { c ->
                val iId = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val iData = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val iAdded = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val dur = if (iDur >= 0) c.getLong(iDur) else 0L
                    if (dur < 30000) continue
                    val path = if (iData >= 0) c.getString(iData) ?: "" else ""
                    var title = if (iTitle >= 0) c.getString(iTitle) ?: "" else ""
                    if (title.isBlank()) {
                        title = try { java.io.File(path).nameWithoutExtension.ifBlank { "Sin título" } } catch (_: Exception) { "Sin título" }
                    }
                    var artist = if (iArtist >= 0) c.getString(iArtist) ?: "" else ""
                    if (artist.isBlank() || artist == "<unknown>") artist = "Desconocido"
                    var album = if (iAlbum >= 0) c.getString(iAlbum) ?: "" else ""
                    if (album == "<unknown>") album = ""
                    songs.add(
                        Song(
                            id = if (iId >= 0) c.getLong(iId) else 0L,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = if (iAlbumId >= 0) c.getLong(iAlbumId) else 0L,
                            durationMs = dur,
                            dataPath = if (iData >= 0) c.getString(iData) ?: "" else "",
                            dateAdded = if (iAdded >= 0) c.getLong(iAdded) else 0L
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return songs
    }
}

package com.melodia.app.data
import com.melodia.app.data.model.Song
import java.io.File
data class MusicFolder(val path: String, val name: String, val count: Int)
data class Album(val name: String, val artist: String, val count: Int, val artId: Long)
data class Artist(val name: String, val count: Int)
fun groupFolders(songs: List<Song>): List<MusicFolder> =
    songs.groupBy { try { File(it.dataPath).parent ?: "Desconocido" } catch (_: Exception) { "Desconocido" } }
        .map { (p, l) -> MusicFolder(p, try { File(p).name.ifBlank { p } } catch (_: Exception) { p }, l.size) }
        .sortedBy { it.name.lowercase() }
fun groupAlbums(songs: List<Song>): List<Album> =
    songs.groupBy { it.album.ifBlank { "Sin álbum" } }
        .map { (n, l) -> Album(n, l.firstOrNull()?.artist ?: "", l.size, l.firstOrNull()?.albumId ?: 0L) }
        .sortedBy { it.name.lowercase() }
fun groupArtists(songs: List<Song>): List<Artist> =
    songs.groupBy { it.artist.ifBlank { "Desconocido" } }
        .map { (n, l) -> Artist(n, l.size) }
        .sortedBy { it.name.lowercase() }

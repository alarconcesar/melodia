package com.melodia.app.data
import com.melodia.app.data.model.Song
import java.io.File
data class MusicFolder(val path: String, val name: String, val count: Int)
data class Album(val name: String, val artist: String, val count: Int, val artId: Long)
data class Artist(val name: String, val count: Int)

/**
 * Orden de álbum: disco → pista → título. Los temas sin número de pista van al
 * final del álbum, incluso si el álbum tiene varios discos (antes se colaban
 * entre el disco 1 y el 2 porque el disco desconocido se asumía 1).
 */
val AlbumTrackOrder: Comparator<Song> = compareBy<Song>(
    { if (it.trackNumber > 0) (if (it.discNumber > 0) it.discNumber else 1) else Int.MAX_VALUE },
    { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
    { it.title.lowercase() }
)

fun groupFolders(songs: List<Song>): List<MusicFolder> =
    songs.groupBy { try { File(it.dataPath).parent ?: "Desconocido" } catch (_: Exception) { "Desconocido" } }
        .map { (p, l) -> MusicFolder(p, try { File(p).name.ifBlank { p } } catch (_: Exception) { p }, l.size) }
        .sortedBy { it.name.lowercase() }
fun groupAlbums(songs: List<Song>): List<Album> =
    songs.groupBy { it.album.ifBlank { "Sin álbum" } }
        .map { (n, l) ->
            val first = l.firstOrNull()
            val artist = first?.albumArtist?.takeIf { it.isNotBlank() } ?: first?.artist ?: ""
            Album(n, artist, l.size, first?.albumId ?: 0L)
        }
        .sortedBy { it.name.lowercase() }
fun groupArtists(songs: List<Song>): List<Artist> =
    songs.groupBy { it.artist.ifBlank { "Desconocido" } }
        .map { (n, l) -> Artist(n, l.size) }
        .sortedBy { it.name.lowercase() }

/**
 * Filtros de biblioteca que no dependen de Android: rutas excluidas por el
 * usuario (p. ej. la carpeta de audios de WhatsApp).
 */
object LibraryFilters {
    /** true si [path] está dentro de alguna carpeta excluida. */
    fun isExcluded(path: String, excluded: Set<String>): Boolean {
        if (path.isBlank() || excluded.isEmpty()) return false
        val limpio = path.trim()
        return excluded.any { raw ->
            val ex = raw.trim().trimEnd('/')
            ex.isNotEmpty() && (limpio == ex || limpio.startsWith("$ex/"))
        }
    }
}

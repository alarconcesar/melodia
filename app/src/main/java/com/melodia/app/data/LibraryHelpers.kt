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
    // Clave nombre+artista: antes dos "Greatest Hits" de artistas distintos se
    // fusionaban en una mega-lista que se reproducía entera por error.
    songs.groupBy {
        val n = it.album.ifBlank { "Sin álbum" }
        val a = it.albumArtist.takeIf { a -> a.isNotBlank() } ?: it.artist.ifBlank { "Desconocido" }
        n.trim().lowercase() to a.trim().lowercase()
    }.map { (_, l) ->
            val first = l.firstOrNull()
            val artist = first?.albumArtist?.takeIf { it.isNotBlank() } ?: first?.artist ?: ""
            val artId = l.firstOrNull { it.albumId > 0 }?.albumId ?: 0L
            Album(first?.album?.ifBlank { "Sin álbum" } ?: "Sin álbum", artist, l.size, artId)
        }
        .sortedBy { it.name.lowercase() }
/** Una tarjeta de álbum mantiene nombre y artista al abrirse y reproducirse. */
data class AlbumIdentity(val name: String, val artist: String? = null)

fun songsForAlbum(songs: List<Song>, identity: AlbumIdentity): List<Song> = songs.filter { song ->
    song.album.trim().ifBlank { "Sin álbum" }.equals(identity.name.trim(), ignoreCase = true) &&
        (identity.artist == null || (song.albumArtist.takeIf { it.isNotBlank() }
            ?: song.artist.ifBlank { "Desconocido" }).trim().equals(identity.artist.trim(), ignoreCase = true))
}.sortedWith(AlbumTrackOrder)

fun songsForArtist(songs: List<Song>, name: String): List<Song> = songs.filter {
    it.artist.trim().ifBlank { "Desconocido" }.equals(name.trim(), ignoreCase = true)
}

fun groupArtists(songs: List<Song>): List<Artist> =
    songs.groupBy { it.artist.trim().ifBlank { "Desconocido" }.lowercase() }
        .map { (_, l) -> Artist(l.first().artist.trim().ifBlank { "Desconocido" }, l.size) }
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

/**
 * Título/artista listos para mostrar.
 *
 * - Quita el sufijo "[videoId]" que queda en el nombre de las descargas.
 * - Si el artista quedó en "Desconocido"/"YouTube" y el título trae
 *   "Tema - Artista", lo rescata (así llegan muchas descargas).
 * Solo estética de lectura: no reescribe el archivo.
 */
fun normalizeSongMeta(title: String, artist: String): Pair<String, String> {
    var t = title.replace(Regex("""\s*\[[A-Za-z0-9_-]{6,16}]\s*$"""), "").trim().ifBlank { title }
    var a = artist.trim()
    val basura = a.isBlank() || a.equals("Desconocido", true) ||
        a.equals("YouTube", true) || a == "<unknown>"
    if (basura) {
        val partes = t.split(" - ").map { it.trim() }.filter { it.isNotBlank() }
        if (partes.size >= 2) {
            val posible = partes.last()
            val ruido = listOf(
                "official video", "video oficial", "official audio", "official music video",
                "audio", "lyrics", "letra", "hd", "hq", "remastered", "remaster"
            )
            if (posible.lowercase() !in ruido && !posible.startsWith("http") && posible.length in 2..48) {
                a = posible
                t = partes.dropLast(1).joinToString(" - ").ifBlank { t }
            }
        }
    }
    if (a.isBlank()) a = "Desconocido"
    return t to a
}

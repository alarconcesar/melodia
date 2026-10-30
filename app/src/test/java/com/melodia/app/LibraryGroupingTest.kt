package com.melodia.app

import com.melodia.app.data.AlbumTrackOrder
import com.melodia.app.data.groupAlbums
import com.melodia.app.data.groupArtists
import com.melodia.app.data.groupFolders
import com.melodia.app.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Agrupaciones de biblioteca con datos sucios: artistas vacíos, "<unknown>",
 * álbumes en blanco, rutas sin carpeta y orden de pistas por disco.
 */
class LibraryGroupingTest {

    private fun song(
        id: Long,
        title: String = "Tema $id",
        artist: String = "Artista",
        album: String = "Álbum",
        albumArtist: String = "",
        albumId: Long = 10L,
        path: String = "/music/Album/$id.mp3",
        track: Int = 0,
        disc: Int = 0
    ) = Song(
        id = id, title = title, artist = artist, album = album, albumId = albumId,
        durationMs = 180_000, dataPath = path, dateAdded = 0,
        trackNumber = track, discNumber = disc, albumArtist = albumArtist
    )

    @Test
    fun artistasNoDuplicanPorMayusculas() {
        val lista = listOf(
            song(1, artist = "Muse"),
            song(2, artist = "muse"),
            song(3, artist = "Muse ")
        )
        val artistas = groupArtists(lista)
        assertEquals(1, artistas.size)
        assertEquals(3, artistas.first().count)
    }

    @Test
    fun artistasVaciosCaenEnDesconocido() {
        val lista = listOf(
            song(1, artist = ""),
            song(2, artist = "  "),
            song(3, artist = "Muse"),
            song(4, artist = "Muse")
        )
        val artistas = groupArtists(lista).associate { it.name to it.count }
        assertEquals(2, artistas["Desconocido"])
        assertEquals(2, artistas["Muse"])
    }

    @Test
    fun albumsVaciosSeAgrupanComoSinAlbum() {
        val lista = listOf(
            song(1, album = ""),
            song(2, album = ""),
            song(3, album = "Absolution")
        )
        val albums = groupAlbums(lista).associate { it.name to it.count }
        assertEquals(2, albums["Sin álbum"])
        assertEquals(1, albums["Absolution"])
    }

    @Test
    fun elAlbumUsaAlbumArtistCuandoExiste() {
        val lista = listOf(
            song(1, album = "Compilado", albumArtist = "Varios", artist = "Uno"),
            song(2, album = "Compilado", albumArtist = "Varios", artist = "Dos")
        )
        val a = groupAlbums(lista).first { it.name == "Compilado" }
        assertEquals("Varios", a.artist)
        assertEquals(2, a.count)
    }

    @Test
    fun carpetasSinRutaNoRevientan() {
        val lista = listOf(song(1, path = ""), song(2, path = "/a/b/c.mp3"))
        val carpetas = groupFolders(lista)
        assertTrue(carpetas.isNotEmpty())
        assertTrue(carpetas.any { it.name == "b" })
    }

    @Test
    fun ordenDeAlbumPorDiscoPistaYTitulo() {
        val lista = listOf(
            song(1, title = "B", track = 2, disc = 1),
            song(2, title = "A", track = 1, disc = 1),
            song(3, title = "C", track = 1, disc = 2),
            song(4, title = "Sin número", track = 0, disc = 0)
        )
        val ordenada = lista.sortedWith(AlbumTrackOrder).map { it.title }
        assertEquals(listOf("A", "B", "C", "Sin número"), ordenada)
    }

    @Test
    fun ordenDeAlbumEmpataPorTitulo() {
        val lista = listOf(
            song(1, title = "Zeta", track = 1),
            song(2, title = "alfa", track = 1)
        )
        assertEquals(listOf("alfa", "Zeta"), lista.sortedWith(AlbumTrackOrder).map { it.title })
    }
}

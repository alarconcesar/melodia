package com.melodia.app

import com.melodia.app.data.AlbumTrackOrder
import com.melodia.app.data.groupAlbums
import com.melodia.app.data.groupArtists
import com.melodia.app.data.groupFolders
import com.melodia.app.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Biblioteca grande (5.000 temas, como la de César): las agrupaciones y el orden
 * de álbum deben dar resultados correctos y no dispararse de tiempo. Antes el
 * filtrado corría en el hilo principal y se notaba en la UI.
 */
class LibraryScaleTest {

    private fun biblioteca(n: Int): List<Song> = (1..n).map { i ->
        Song(
            id = i.toLong(),
            title = "Tema $i",
            artist = "Artista ${i % 120}",
            album = "Album ${i % 400}",
            albumId = (i % 400).toLong(),
            durationMs = 180_000,
            dataPath = "/music/Artista ${i % 120}/Album ${i % 400}/$i.mp3",
            dateAdded = i.toLong(),
            trackNumber = (i % 12) + 1,
            discNumber = 1
        )
    }

    @Test
    fun cincoMilTemasSeAgrupanBienYRapido() {
        val songs = biblioteca(5_000)
        lateinit var artistas: List<com.melodia.app.data.Artist>
        lateinit var albums: List<com.melodia.app.data.Album>
        lateinit var carpetas: List<com.melodia.app.data.MusicFolder>
        val ms = measureTimeMillis {
            artistas = groupArtists(songs)
            albums = groupAlbums(songs)
            carpetas = groupFolders(songs)
        }
        assertEquals(120, artistas.size)
        // Clave nombre+artista: "Album X" de artistas distintos ya no se fusiona
        // en una mega-lista (400 nombres × combinaciones con 120 artistas).
        assertEquals(1200, albums.size)
        // Las carpetas son combinaciones artista/álbum presentes en la biblioteca
        assertTrue(carpetas.isNotEmpty())
        assertEquals(5_000, carpetas.sumOf { it.count })
        assertEquals(5_000, artistas.sumOf { it.count })
        assertEquals(5_000, albums.sumOf { it.count })
        assertTrue("agrupar 5k tardó ${ms}ms", ms < 3_000)
    }

    @Test
    fun ordenarUnAlbumGrandeNoPierdeTemas() {
        val album = biblioteca(600)
        val ordenado = album.shuffled().sortedWith(AlbumTrackOrder)
        assertEquals(600, ordenado.size)
        assertEquals(600, ordenado.map { it.id }.distinct().size)
        // Las pistas dentro de un mismo disco quedan ascendentes
        val pistas = ordenado.map { it.trackNumber }
        assertEquals(pistas.sorted(), pistas)
    }
}

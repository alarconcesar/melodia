package com.melodia.app

import com.melodia.app.data.AlbumTrackOrder
import com.melodia.app.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El orden de un álbum debe ser disco → pista → título, no alfabético.
 * Antes de 4.6 los álbumes se escuchaban en orden de título.
 */
class LibraryOrderTest {

    private fun song(id: Long, title: String, track: Int, disc: Int = 1) = Song(
        id = id, title = title, artist = "A", album = "Disco", albumId = 1,
        durationMs = 1000, dataPath = "/m/$id.mp3", dateAdded = id,
        trackNumber = track, discNumber = disc
    )

    @Test
    fun ordenaPorPistaNoPorTitulo() {
        val list = listOf(song(1, "Zeta", 1), song(2, "Alfa", 2), song(3, "Mono", 3))
        assertEquals(listOf(1L, 2L, 3L), list.sortedWith(AlbumTrackOrder).map { it.id })
    }

    @Test
    fun discosAntesQuePistas() {
        val list = listOf(song(1, "D2T1", 1, disc = 2), song(2, "D1T2", 2, disc = 1), song(3, "D1T1", 1, disc = 1))
        assertEquals(listOf(3L, 2L, 1L), list.sortedWith(AlbumTrackOrder).map { it.id })
    }

    @Test
    fun sinNumeroDePistaVaAlFinal() {
        val list = listOf(song(1, "Sin", 0), song(2, "Uno", 1), song(3, "Dos", 2))
        assertEquals(listOf(2L, 3L, 1L), list.sortedWith(AlbumTrackOrder).map { it.id })
    }

    @Test
    fun empateDePistaSeRompePorTitulo() {
        val list = listOf(song(1, "B", 1), song(2, "A", 1))
        assertEquals(listOf(2L, 1L), list.sortedWith(AlbumTrackOrder).map { it.id })
    }
}

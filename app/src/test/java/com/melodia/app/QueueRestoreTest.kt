package com.melodia.app

import com.melodia.app.data.model.Song
import com.melodia.app.player.QueueRestore
import com.melodia.app.player.QueueSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Restaurar la cola tiene que sobrevivir a que el usuario haya borrado temas.
 * Si el tema actual desapareció, se arranca en el primero y sin posición.
 */
class QueueRestoreTest {

    private fun song(id: Long) = Song(
        id = id, title = "Tema $id", artist = "A", album = "Al", albumId = 1,
        durationMs = 1000, dataPath = "/m/$id.mp3", dateAdded = id
    )

    private fun snapshot(ids: List<Long>, index: Int, pos: Long) = QueueSnapshot(
        songs = ids.map { song(it) }, index = index, positionMs = pos, shuffle = false, repeatMode = 0
    )

    @Test
    fun restauraColaCompletaEnElMismoTema() {
        val plan = QueueRestore.plan(snapshot(listOf(1, 2, 3), 1, 42_000), listOf(song(1), song(2), song(3)))!!
        assertEquals(listOf(1L, 2L, 3L), plan.songs.map { it.id })
        assertEquals(1, plan.index)
        assertEquals(42_000L, plan.positionMs)
    }

    @Test
    fun descartaTemasBorradosYRecolocaElIndice() {
        val plan = QueueRestore.plan(snapshot(listOf(1, 2, 3), 2, 30_000), listOf(song(2), song(3)))!!
        assertEquals(listOf(2L, 3L), plan.songs.map { it.id })
        assertEquals(1, plan.index)
        assertEquals(30_000L, plan.positionMs)
    }

    @Test
    fun siElTemaActualDesaparecioArrancaEnElPrimero() {
        val plan = QueueRestore.plan(snapshot(listOf(1, 2, 3), 1, 30_000), listOf(song(1), song(3)))!!
        assertEquals(0, plan.index)
        assertEquals(0L, plan.positionMs)
    }

    @Test
    fun sinBibliotecaNoSeRestaura() {
        assertNull(QueueRestore.plan(snapshot(listOf(1), 0, 0), emptyList()))
    }

    @Test
    fun sinCoincidenciasNoSeRestaura() {
        assertNull(QueueRestore.plan(snapshot(listOf(9, 8), 0, 0), listOf(song(1))))
    }

    @Test
    fun sinSnapshotNoSeRestaura() {
        assertNull(QueueRestore.plan(null, listOf(song(1))))
    }
}

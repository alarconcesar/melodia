package com.melodia.app

import com.melodia.app.player.QueueOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La cola que ve el usuario y la que reproduce ExoPlayer son dos listas
 * paralelas: si el cálculo del movimiento se equivoca, la app salta temas o
 * cierra al llegar al final. Estos tests fijan ese contrato.
 */
class QueueOpsTest {

    private val base = listOf("a", "b", "c", "d", "e")

    @Test
    fun subirUnPuesto() {
        assertEquals(listOf("b", "a", "c", "d", "e"), QueueOps.move(base, 1, 0))
    }

    @Test
    fun bajarUnPuesto() {
        assertEquals(listOf("a", "c", "d", "b", "e"), QueueOps.move(base, 1, 3))
    }

    @Test
    fun moverDelPrimeroAlUltimo() {
        assertEquals(listOf("b", "c", "d", "e", "a"), QueueOps.move(base, 0, 4))
    }

    @Test
    fun moverDelUltimoAlPrimero() {
        assertEquals(listOf("e", "a", "b", "c", "d"), QueueOps.move(base, 4, 0))
    }

    @Test
    fun moverAlMismoSitioNoCambiaNada() {
        assertNull(QueueOps.move(base, 2, 2))
    }

    @Test
    fun indicesFueraDeRangoNoAplican() {
        assertNull(QueueOps.move(base, -1, 2))
        assertNull(QueueOps.move(base, 2, 5))
        assertNull(QueueOps.move(emptyList<String>(), 0, 0))
    }

    @Test
    fun noSePierdeNiDuplicanElementos() {
        val out = QueueOps.move(base, 3, 1)!!
        assertEquals(base.size, out.size)
        assertEquals(base.toSet(), out.toSet())
    }

    @Test
    fun bordesDeLosBotones() {
        assertFalse(QueueOps.canMoveUp(0, 5))
        assertTrue(QueueOps.canMoveUp(1, 5))
        assertTrue(QueueOps.canMoveDown(3, 5))
        assertFalse(QueueOps.canMoveDown(4, 5))
        assertFalse(QueueOps.canMoveDown(0, 1))
        assertFalse(QueueOps.canMoveUp(0, 1))
    }

    @Test
    fun vaciarConservaSoloElActual() {
        assertEquals(listOf("c"), QueueOps.keepOnly(base, 2))
        assertTrue(QueueOps.keepOnly(base, 9).isEmpty())
    }
}

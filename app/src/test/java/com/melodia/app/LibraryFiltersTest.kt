package com.melodia.app

import com.melodia.app.data.LibraryFilters
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exclusión de carpetas: lo que está dentro de una carpeta excluida no debe
 * aparecer, pero una carpeta con nombre parecido sí (prefijo con separador).
 */
class LibraryFiltersTest {

    private val wa = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio"

    @Test
    fun unArchivoDentroDeLaCarpetaExcluidaSeExcluye() {
        assertTrue(LibraryFilters.isExcluded("$wa/AUD-20260116-WA0068.opus", setOf(wa)))
    }

    @Test
    fun laCarpetaExactaTambienCuenta() {
        assertTrue(LibraryFilters.isExcluded(wa, setOf(wa)))
    }

    @Test
    fun unaCarpetaConNombreParecidoNoSeExcluye() {
        assertFalse(
            LibraryFilters.isExcluded("$wa 2/otro.opus", setOf(wa))
        )
        assertFalse(
            LibraryFilters.isExcluded("/storage/emulated/0/Music/WhatsApp Audio/x.mp3", setOf(wa))
        )
    }

    @Test
    fun laBarraFinalEnLaExclusionNoRompeLaComparacion() {
        assertTrue(LibraryFilters.isExcluded("$wa/a.opus", setOf("$wa/")))
    }

    @Test
    fun sinExclusionesNoSeFiltraNada() {
        assertFalse(LibraryFilters.isExcluded("$wa/a.opus", emptySet()))
    }

    @Test
    fun unaRutaVaciaNoSeExcluye() {
        assertFalse(LibraryFilters.isExcluded("", setOf(wa)))
    }

    @Test
    fun variasCarpetasExcluidas() {
        val otra = "/storage/emulated/0/Recordings"
        val excl = setOf(wa, otra)
        assertTrue(LibraryFilters.isExcluded("$wa/x.opus", excl))
        assertTrue(LibraryFilters.isExcluded("$otra/y.m4a", excl))
        assertFalse(LibraryFilters.isExcluded("/storage/emulated/0/Music/z.mp3", excl))
    }
}

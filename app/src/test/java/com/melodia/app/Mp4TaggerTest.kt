package com.melodia.app

import com.melodia.app.data.online.Mp4Tagger
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El etiquetado del m4a corre dentro de la descarga y cualquier excepción se
 * traga, pero un bucle infinito o un desborde sí romperían la app. Se comprueba
 * que con basura no reviente y que con un archivo válido devuelva algo.
 */
class Mp4TaggerTest {

    @Test
    fun conBasuraNoRevienta() {
        val basura = ByteArray(512) { (it * 31 % 255).toByte() }
        val out = Mp4Tagger.tag(basura, "Titulo", "Artista", "Album", null, null, null)
        assertNotNull(out)
        assertTrue("sin moov debe devolver el mismo archivo", out.contentEquals(basura))
    }

    @Test
    fun conArregloVacioNoRevienta() {
        val out = Mp4Tagger.tag(ByteArray(0), "T", "A", null, null, null, null)
        assertNotNull(out)
    }

    @Test
    fun unFtypSoloNoRevienta() {
        // caja ftyp mínima: size=16, type=ftyp, major brand + versión
        val ftyp = byteArrayOf(
            0, 0, 0, 16, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'M'.code.toByte(), '4'.code.toByte(), 'A'.code.toByte(), ' '.code.toByte(),
            0, 0, 0, 0
        )
        val out = Mp4Tagger.tag(ftyp, "T", "A", "Al", null, null, null)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun etiquetarDosVecesNoRompeElArchivo() {
        val ftyp = byteArrayOf(
            0, 0, 0, 16, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'M'.code.toByte(), '4'.code.toByte(), 'A'.code.toByte(), ' '.code.toByte(),
            0, 0, 0, 0
        )
        val una = Mp4Tagger.tag(ftyp, "T", "A", "Al", "letra", null, null)
        val dos = Mp4Tagger.tag(una, "T2", "A2", "Al2", "letra2", null, null)
        assertTrue(dos.isNotEmpty())
        // El archivo no debe crecer sin límite al re-etiquetar
        assertTrue("creció de ${una.size} a ${dos.size}", dos.size < una.size * 3 + 1024)
    }
}

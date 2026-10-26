package com.melodia.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.model.Song
import com.melodia.app.ui.screens.QueueSheetContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Contrato de la hoja de cola: conteo, duración, extremos deshabilitados y
 * que los botones llamen con los índices correctos. Se compone el contenido
 * sin ViewModel para no depender de Room ni del servicio de audio.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class QueueSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private fun canciones(n: Int): List<Song> = (1L..n.toLong()).map { i ->
        Song(
            id = i, title = "Tema $i", artist = "Artista $i", album = "Album",
            albumId = 10L, durationMs = 180_000L, dataPath = "/sdcard/Music/t$i.mp3",
            dateAdded = i
        )
    }

    private fun componer(
        songs: List<Song>,
        currentId: Long? = 2L,
        onPlay: (Int) -> Unit = {},
        onMove: (Int, Int) -> Unit = { _, _ -> },
        onRemove: (Int) -> Unit = {},
        onClear: () -> Unit = {},
        onSave: () -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                QueueSheetContent(
                    queue = songs,
                    currentId = currentId,
                    shuffle = false,
                    repeatMode = 0,
                    onPlay = onPlay,
                    onMove = onMove,
                    onRemove = onRemove,
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onClear = onClear,
                    onSave = onSave,
                    onClose = {}
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun muestraConteoYDuracionTotal() {
        componer(canciones(5), currentId = 3L)
        rule.onNodeWithText("5 temas · 15 min").assertExists()
        rule.onNodeWithText("Tema 3").assertExists()
    }

    @Test
    fun losExtremosNoSePuedenMoverMasAlla() {
        componer(canciones(4))
        val subir = rule.onAllNodesWithContentDescription("Subir")
        val bajar = rule.onAllNodesWithContentDescription("Bajar")
        subir[0].assertIsNotEnabled()
        bajar[3].assertIsNotEnabled()
        subir[1].assertIsEnabled()
        bajar[2].assertIsEnabled()
    }

    @Test
    fun losBotonesLlamanConElIndiceCorrecto() {
        var movido: Pair<Int, Int>? = null
        var quitado: Int? = null
        var guardado = false
        componer(
            canciones(4),
            onMove = { from, to -> movido = from to to },
            onRemove = { quitado = it },
            onSave = { guardado = true }
        )
        rule.onAllNodesWithContentDescription("Subir")[1].performClick()
        assertEquals(1 to 0, movido)
        rule.onAllNodesWithContentDescription("Quitar")[2].performClick()
        assertEquals(2, quitado)
        rule.onNodeWithText("Guardar lista").performClick()
        assertEquals(true, guardado)
    }

    @Test
    fun laColaVaciaSeExplicaSola() {
        componer(emptyList(), currentId = null)
        rule.onNodeWithText("La cola está vacía").assertExists()
        rule.onNodeWithText("Sin temas").assertExists()
    }

    @Test
    fun laHojaEntraEnLaPantallaConColaLarga() {
        componer(canciones(12), currentId = 5L)
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val guardar = rule.onNodeWithText("Guardar lista").getUnclippedBoundsInRoot()
        assertTrue("el pie se sale por la derecha", guardar.right <= root.right)
        assertTrue("el pie se corta abajo", guardar.bottom <= root.bottom)
        val titulo = rule.onNodeWithText("Tema 5").getUnclippedBoundsInRoot()
        val anchoTitulo = titulo.right - titulo.left
        assertTrue("el título queda sin espacio: $anchoTitulo", anchoTitulo.value >= 110f)
    }
}

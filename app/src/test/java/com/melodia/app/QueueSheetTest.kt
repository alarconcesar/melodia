package com.melodia.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
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
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
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
        // Sin tema actual la lista muestra la cola completa (índices 0..3).
        componer(canciones(4), currentId = null)
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
            currentId = null,
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
    fun laColaLargaEntraEnLaPantalla() {
        componer(canciones(12), currentId = 5L)
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val guardar = rule.onNodeWithText("Guardar lista").getUnclippedBoundsInRoot()
        assertTrue("el pie se sale por la derecha", guardar.right <= root.right)
        assertTrue("el pie se corta abajo", guardar.bottom <= root.bottom)
        // La lista arranca después del tema que suena y solo muestra lo que viene.
        rule.onNodeWithText("A continuación · 7").assertExists()
        rule.onNodeWithText("SONANDO AHORA").assertExists()
    }

    @Test
    fun laListaEmpiezaDespuesDelTemaActual() {
        componer(canciones(5), currentId = 2L)
        rule.onNodeWithText("A continuación · 3").assertExists()
        rule.onNodeWithText("Tema 2").assertExists()   // solo en la tarjeta
        rule.onNodeWithText("Tema 3").assertExists()   // primer próximo
    }

    @Test
    fun sinColaAvisaQueNoHayNadaMas() {
        componer(canciones(2), currentId = 2L)
        rule.onNodeWithText("No hay más temas después de este.").assertExists()
    }
}

package com.melodia.app

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.lyrics.LyricLine
import com.melodia.app.data.model.Song
import com.melodia.app.ui.screens.LyricsContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Contenido puro a 320 dp / fuente 1.3: sin ViewModel, Room ni capturas. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w320dp-h720dp-mdpi", application = Application::class)
class LyricsPolishTest {
    @get:Rule val rule = createComposeRule()

    private val returnLabel = "Volver al verso actual"
    private val listTag = "lyrics-synced-list"
    private val slotTag = "lyrics-follow-slot"

    private fun song(id: Long = 1L) = Song(id, "Tema", "Artista", "Álbum", 0,
        100_000, "/music/tema.m4a", 0)

    private fun longLyrics() = MainViewModel.LyricsUi.Synced(
        List(60) { LyricLine(it * 1_000L, "Verso $it") }
    )

    private fun compose(content: @Composable () -> Unit) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, 1.3f)) {
                MelodiaTheme { Box(Modifier.width(320.dp).fillMaxHeight()) { content() } }
            }
        }
    }

    private fun layout(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(results)) }
        assertEquals(1, results.size)
        return results.single()
    }

    private fun scrollPosition(): Float {
        val axis = rule.onNodeWithTag(listTag).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        return rule.runOnIdle { axis.value() }
    }

    private fun manualSwipe() {
        rule.onNodeWithTag(listTag).performTouchInput { swipeUp(durationMillis = 600) }
        rule.onNodeWithText(returnLabel).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test fun versosDuplicadosConservanGeometriaYEstiloAlActivarse() {
        val repeated = "Y volvemos a cantar el mismo verso una vez más"
        val lyrics = MainViewModel.LyricsUi.Synced(listOf(
            LyricLine(0, repeated), LyricLine(0, repeated), LyricLine(10_000, "Final")
        ))
        var position by mutableStateOf(-1_000L)
        compose { LyricsContent(song(), lyrics, position, 100_000, {}, {}, {}) }
        val duplicates = rule.onAllNodesWithText(repeated)
        duplicates.assertCountEquals(2)
        val before = (0..1).map { layout(duplicates[it]) }
        val bounds = (0..1).map { duplicates[it].getUnclippedBoundsInRoot() }
        duplicates[0].assertIsNotSelected()
        duplicates[1].assertIsNotSelected()
        rule.runOnIdle { position = 0L }
        duplicates[0].assertIsNotSelected()
        duplicates[1].assertIsSelected()
        (0..1).forEach { index ->
            val after = layout(duplicates[index])
            assertEquals(1.3f, after.layoutInput.density.fontScale, 0f)
            assertEquals(19.sp, after.layoutInput.style.fontSize)
            assertEquals(29.sp, after.layoutInput.style.lineHeight)
            assertEquals(before[index].layoutInput.style.fontWeight, after.layoutInput.style.fontWeight)
            assertEquals(before[index].layoutInput.style.fontFamily, after.layoutInput.style.fontFamily)
            assertEquals(before[index].size, after.size)
            assertEquals(before[index].lineCount, after.lineCount)
            assertEquals(bounds[index], duplicates[index].getUnclippedBoundsInRoot())
            assertFalse(after.hasVisualOverflow)
            assertTrue(after.lineCount >= 1)
        }
        assertEquals(before[0].size, before[1].size)
    }

    @Test fun seekDeCadaVersoRespetaDuracionYLimites() {
        val seeks = mutableListOf<Float>()
        val lyrics = MainViewModel.LyricsUi.Synced(listOf(
            LyricLine(-5_000, "Antes"), LyricLine(25_000, "En el tiempo"), LyricLine(200_000, "Después")
        ))
        compose { LyricsContent(song(), lyrics, -10_000, 100_000, {}, {}, { seeks += it }) }
        listOf("Antes", "En el tiempo", "Después").forEach {
            rule.onNodeWithText(it).assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        }
        rule.runOnIdle { assertEquals(listOf(0f, 0.25f, 1f), seeks) }
    }

    @Test fun duracionDesconocidaAvisaSinBuscarYPermiteBuscarCuandoCarga() {
        val seeks = mutableListOf<Float>()
        var notices = 0
        var duration by mutableStateOf(0L)
        val lyrics = MainViewModel.LyricsUi.Synced(listOf(LyricLine(50_000, "Mitad")))
        compose { LyricsContent(song(), lyrics, 0, duration, {}, {}, { seeks += it }, { notices++ }) }
        rule.onNodeWithText("Mitad").performClick()
        rule.runOnIdle { duration = -1L }
        rule.onNodeWithText("Mitad").performClick()
        rule.runOnIdle { assertEquals(2, notices); assertTrue(seeks.isEmpty()); duration = 100_000 }
        rule.onNodeWithText("Mitad").performClick()
        rule.runOnIdle { assertEquals(listOf(0.5f), seeks); assertEquals(2, notices) }
    }

    @Test fun scrollProgramaticoSigueVersosSinConfundirseConGestoManual() {
        var position by mutableStateOf(0L)
        val lyrics = longLyrics()
        compose { LyricsContent(song(), lyrics, position, 100_000, {}, {}, {}) }
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
        val start = scrollPosition()
        rule.runOnIdle { position = 20_000L }
        rule.onNodeWithText("Verso 20").assertIsDisplayed().assertIsSelected()
        assertTrue(scrollPosition() > start)
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
        rule.runOnIdle { position = 35_000L }
        rule.onNodeWithText("Verso 35").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
    }

    @Test fun gestoManualPausaHastaVolverAlVersoActualSinReflow() {
        var position by mutableStateOf(20_000L)
        val lyrics = longLyrics()
        compose { LyricsContent(song(), lyrics, position, 100_000, {}, {}, {}) }
        rule.onNodeWithText("Verso 20").assertIsDisplayed()
        val listBounds = rule.onNodeWithTag(listTag).getUnclippedBoundsInRoot()
        val slotBounds = rule.onNodeWithTag(slotTag).getUnclippedBoundsInRoot()
        val beforeSwipe = scrollPosition()
        manualSwipe()
        val pausedPosition = scrollPosition()
        assertTrue(pausedPosition > beforeSwipe)
        assertFalse(layout(rule.onNodeWithText(returnLabel)).hasVisualOverflow)
        rule.runOnIdle { position = 45_000L }
        assertEquals(pausedPosition, scrollPosition(), 0f)
        rule.runOnIdle { position = 50_000L }
        assertEquals(pausedPosition, scrollPosition(), 0f)
        assertEquals(listBounds, rule.onNodeWithTag(listTag).getUnclippedBoundsInRoot())
        assertEquals(slotBounds, rule.onNodeWithTag(slotTag).getUnclippedBoundsInRoot())
        rule.onNodeWithText(returnLabel).performClick()
        rule.onNodeWithText("Verso 50").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
        assertEquals(listBounds, rule.onNodeWithTag(listTag).getUnclippedBoundsInRoot())
        assertEquals(slotBounds, rule.onNodeWithTag(slotTag).getUnclippedBoundsInRoot())
        // Reanudar no es un salto de una sola vez: el siguiente verso vuelve a acompañarse.
        rule.runOnIdle { position = 55_000L }
        rule.onNodeWithText("Verso 55").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
    }

    @Test fun cambiarDeTemaReseteaSeguimientoYPosicionManual() {
        var currentSong by mutableStateOf(song())
        val lyrics = longLyrics()
        compose { LyricsContent(currentSong, lyrics, 20_000, 100_000, {}, {}, {}) }
        manualSwipe()
        rule.runOnIdle { currentSong = song(2L) }
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
        rule.onNodeWithText("Verso 20").assertIsDisplayed().assertIsSelected()
    }

    @Test fun cambiarDeTemaOnlineReseteaAunqueCompartaId() {
        var currentSong by mutableStateOf(song(-100L).copy(dataPath = ""))
        val lyrics = longLyrics()
        compose { LyricsContent(currentSong, lyrics, 20_000, 100_000, {}, {}, {}) }
        manualSwipe()
        rule.runOnIdle { currentSong = currentSong.copy(title = "Otro tema online") }
        rule.onNodeWithText(returnLabel).assertDoesNotExist()
        rule.onNodeWithText("Verso 20").assertIsDisplayed().assertIsSelected()
    }
}

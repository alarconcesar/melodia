package com.melodia.app

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.DlJob
import com.melodia.app.data.online.DlStatus
import com.melodia.app.data.online.YtRepository
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.DownloadsScreenContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w320dp-h720dp-night-mdpi")
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class DownloadsPolishTest {
    @get:Rule val rule = createComposeRule()

    // Árbol combinado: medir la acción, no el Text o Icon decorativo de dentro.
    private fun textAction(text: String) = rule.onNode(hasText(text) and hasClickAction())
    private fun iconAction(description: String) =
        rule.onNode(hasContentDescription(description) and hasClickAction())

    private fun SemanticsNodeInteraction.assertTouchTarget() =
        assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)

    private fun assertNoOverflow(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
        assertEquals("Debe haber un único layout para «$text»", 1, layouts.size)
        assertFalse("Se recorta «$text»", layouts.single().hasVisualOverflow)
    }

    private fun job(id: String, status: DlStatus, error: String? = null) = DlJob(
        track = YtTrack(id, "Tema $id", "Artista", 180, null, "https://youtu.be/$id"),
        high = true, status = status, progress = 0.5f, downloaded = 524_288, total = 1_048_576,
        error = error
    )

    private fun compose(
        jobs: List<DlJob> = emptyList(),
        history: List<YtRepository.DlRecord> = emptyList(),
        onPauseAll: () -> Unit = {},
        onResumeAll: () -> Unit = {},
        onResume: (String) -> Unit = {},
        onPause: (String) -> Unit = {},
        onCancel: (String) -> Unit = {},
        onClear: () -> Unit = {},
        onPlay: (YtRepository.DlRecord) -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                MelodiaTheme {
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        DownloadsScreenContent(
                            jobs = jobs, history = history, onClose = onClose,
                            onPauseAll = onPauseAll, onResumeAll = onResumeAll,
                            onPause = onPause, onResume = onResume, onCancel = onCancel,
                            onClearHistory = onClear, onPlayRecord = onPlay
                        )
                    }
                }
            }
        }
    }

    @Test fun colaMixtaNoComprimeContadorNiAccionesGlobales() {
        var pauses = 0
        var resumes = 0
        compose(listOf(job("a", DlStatus.ACTIVE), job("b", DlStatus.PAUSED)),
            onPauseAll = { pauses++ }, onResumeAll = { resumes++ })
        rule.onNodeWithText("En curso · 2", ignoreCase = true).assertIsDisplayed()
        textAction("Pausar todo").assertIsDisplayed().assertTouchTarget().performClick()
        textAction("Reanudar todo").assertIsDisplayed().assertTouchTarget().performClick()
        assertNoOverflow("Pausar todo")
        assertNoOverflow("Reanudar todo")
        rule.runOnIdle { assertEquals(1, pauses); assertEquals(1, resumes) }
    }

    @Test fun errorCompletoConReintentoYQuitarAccesiblesEn320dp() {
        val message = "No se pudo guardar el archivo porque no queda espacio disponible en el dispositivo. Libera espacio y vuelve a intentarlo."
        var resumed: String? = null
        var removed: String? = null
        compose(listOf(job("fallido", DlStatus.ERROR, message)),
            onResume = { resumed = it }, onCancel = { removed = it })
        rule.onNodeWithText(message).performScrollTo().assertIsDisplayed()
        assertNoOverflow(message)
        rule.onNodeWithText("Pulsa Reanudar para volver a intentarlo.").assertExists()
        iconAction("Reanudar").performScrollTo().assertIsDisplayed().assertTouchTarget().performClick()
        iconAction("Quitar").performScrollTo().assertIsDisplayed().assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals("fallido", resumed); assertEquals("fallido", removed) }
    }

    @Test fun guardadaPermaneceVisibleSinContarComoEnCurso() {
        compose(listOf(job("lista", DlStatus.DONE)))
        rule.onAllNodesWithText("Guardada").assertCountEquals(1)
        rule.onNodeWithText("Tema lista").assertIsDisplayed()
        rule.onNodeWithText("En curso", substring = true, ignoreCase = true).assertDoesNotExist()
        rule.onNodeWithText("Sin descargas en curso").assertDoesNotExist()
        rule.onNodeWithText("Pausar todo").assertDoesNotExist()
        rule.onNodeWithText("Reanudar todo").assertDoesNotExist()
        iconAction("Quitar").assertTouchTarget()
    }

    @Test fun enColaConservaPausaYPorcentajeExacto() {
        var paused: String? = null
        compose(listOf(job("espera", DlStatus.QUEUED)), onPause = { paused = it })
        rule.onNodeWithText("En cola").assertIsDisplayed()
        rule.onNodeWithText("50%").assertIsDisplayed()
        iconAction("Pausar").assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals("espera", paused) }
    }

    @Test fun transferenciaSinTotalTieneEstadoExplicito() {
        compose(listOf(job("nuevo", DlStatus.ACTIVE).copy(total = 0, downloaded = 0, progress = 0f)))
        rule.onNodeWithText("Descargando").assertIsDisplayed()
        rule.onNodeWithText("0%").assertIsDisplayed()
        iconAction("Pausar").assertIsDisplayed().assertTouchTarget()
    }

    @Test fun historialRepetidoConservaFilasReproduccionYLimpieza() {
        val first = YtRepository.DlRecord(title = "Repetido", artist = "Artista", high = true, at = 1L,
            id = "primero", url = "https://youtu.be/primero")
        // Mismo título y fecha: la clave necesita el índice, y el callback debe
        // devolver el registro pulsado, no el primero que tenga ese título.
        val second = first.copy(artist = "Artista segundo", id = "segundo", url = "https://youtu.be/segundo")
        var played: YtRepository.DlRecord? = null
        var cleared = 0
        compose(history = listOf(first, second), onPlay = { played = it }, onClear = { cleared++ })
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Historial"))
        rule.onNodeWithText("Historial").assertIsDisplayed()
        val secondRecord = hasText("Repetido") and hasText("Artista segundo", substring = true) and hasClickAction()
        rule.onNode(hasScrollAction()).performScrollToNode(secondRecord)
        rule.onAllNodesWithText("Repetido").assertCountEquals(2)
        rule.onNode(secondRecord).assertIsDisplayed().assertTouchTarget().performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Borrar historial"))
        iconAction("Borrar historial").assertIsDisplayed().assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals(second, played); assertEquals(1, cleared) }
    }

    @Test
    @Config(qualifiers = "w320dp-h720dp-notnight-mdpi")
    fun pantallaVaciaEnClaroConUnaSolaCabeceraYSalidaComoda() {
        var closed = 0
        compose(onClose = { closed++ })
        rule.onAllNodesWithText("Descargas").assertCountEquals(1)
        rule.onNodeWithText("Sin descargas en curso").assertIsDisplayed()
        iconAction("Atrás").assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals(1, closed) }
    }
}

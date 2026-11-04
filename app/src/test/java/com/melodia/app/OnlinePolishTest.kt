package com.melodia.app

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.YtSearchItem
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.ImportPlaylistDialogContent
import com.melodia.app.ui.screens.OnlineTabContent
import com.melodia.app.ui.screens.QualityDialog
import com.melodia.app.ui.screens.YtCollectionPreviewDialog
import com.melodia.app.ui.screens.YtRowFlat
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Contratos de UI puros, sin ViewModel, red ni capturas nativas de Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w320dp-h720dp-night-mdpi")
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class OnlinePolishTest {
    @get:Rule val rule = createComposeRule()

    // Árbol combinado: medir la acción, no el Text o Icon decorativo de dentro.
    private fun textAction(text: String) = rule.onNode(hasText(text) and hasClickAction())
    private fun disabledTextAction(text: String) = rule.onNode(hasText(text) and isNotEnabled())
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

    private fun track(title: String = "Tema de prueba") =
        YtTrack("repetido", title, "Artista", 180, null, "https://youtu.be/repetido")

    private fun compact(content: @Composable () -> Unit) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                MelodiaTheme { Box(Modifier.width(320.dp).fillMaxHeight()) { content() } }
            }
        }
    }

    private fun online(
        items: List<YtSearchItem> = emptyList(),
        loading: Boolean = false,
        error: String? = null,
        onSearch: () -> Unit = {},
        onClear: () -> Unit = {}
    ) = compact {
        OnlineTabContent(
            query = "muse", items = items, loading = loading, error = error, errorRetry = true,
            dlProgress = emptyMap(), busyId = null, recent = emptyList(), history = emptyList(),
            hasMore = false, moreLoading = false, saver = false, highQ = true,
            onQueryChange = {}, onSearch = onSearch, onClearQuery = onClear,
            onPlay = {}, onDownload = {}, onDownloadAll = {}, onLoadMore = {},
            onOpenDownloads = {}, onOpenImport = {}, onToggleSaver = {}, onToggleQuality = {}
        )
    }

    @Test fun buscadorConImeYControlesDe48dpSinTituloDuplicado() {
        var searches = 0
        var clears = 0
        online(onSearch = { searches++ }, onClear = { clears++ })
        rule.onNode(hasSetTextAction()).performImeAction()
        iconAction("Buscar").assertTouchTarget().performClick()
        iconAction("Limpiar").assertTouchTarget().performClick()
        iconAction("Opciones").assertTouchTarget()
        rule.onNodeWithText("Online").assertDoesNotExist()
        rule.runOnIdle { assertEquals(2, searches); assertEquals(1, clears) }
    }

    @Test fun errorConResultadosNoQuedaOcultoYConservaReintento() {
        var retries = 0
        online(items = listOf(YtSearchItem.Track(track())), error = "Sin conexión", onSearch = { retries++ })
        rule.onNodeWithText("Sin conexión").assertIsDisplayed()
        rule.onNodeWithText("Tema de prueba").assertExists()
        textAction("Reintentar").assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun cargaExplicitaSinEstadoVacioPrematuro() {
        online(loading = true)
        rule.onNodeWithText("Buscando en YouTube…").assertIsDisplayed()
        rule.onNodeWithText("Busca música en YouTube").assertDoesNotExist()
    }

    @Test fun resultadosConIdRepetidoConservanAmbasFilas() {
        online(items = listOf(YtSearchItem.Track(track()), YtSearchItem.Track(track())))
        rule.onAllNodesWithText("Tema de prueba").assertCountEquals(2)
        textAction("Descargar todo").assertTouchTarget()
    }

    @Test fun filaDistinguePreparacionColaYProgresoExacto() {
        val busy = mutableStateOf(true)
        val progress = mutableStateOf<Float?>(null)
        compact { YtRowFlat(track(), progress.value, busy.value, onPlay = {}, onDownload = {}) }
        rule.onNodeWithText("Preparando audio…").assertIsDisplayed()
        rule.runOnIdle { busy.value = false; progress.value = 0f }
        rule.onNodeWithText("En cola").assertIsDisplayed()
        rule.runOnIdle { progress.value = 0.5f }
        rule.onNodeWithText("50%").assertIsDisplayed()
        rule.onNodeWithContentDescription("Descargar").assertDoesNotExist()
    }

    @Test fun importacionEnCursoBloqueaCampoYAccionesSinSegundoEnvio() {
        compact {
            ImportPlaylistDialogContent("https://youtube.com/playlist?list=PL1", "…", {}, {}, {})
        }
        rule.onNodeWithText("https://youtube.com/playlist?list=PL1").assertIsNotEnabled()
        disabledTextAction("Importando…").assertIsNotEnabled().assertTouchTarget()
        disabledTextAction("Cancelar").assertIsNotEnabled().assertTouchTarget()
    }

    @Test fun errorDeImportacionOfreceCorreccionYPermiteReintentar() {
        var imports = 0
        compact {
            ImportPlaylistDialogContent("enlace", "No se pudo leer la playlist", {}, { imports++ }, {})
        }
        rule.onNodeWithText("No se pudo leer la playlist").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Revisa el enlace y vuelve a pulsar Importar.").performScrollTo().assertIsDisplayed()
        textAction("Importar").assertIsEnabled().assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals(1, imports) }
    }

    @Test fun calidadConservaPreferenciaYOpcionesCompletasEnPantallaEstrecha() {
        val high = mutableStateOf(false)
        val picked = mutableListOf<Boolean>()
        var dismissed = 0
        compact {
            QualityDialog(track(), highQ = high.value,
                onPick = { high.value = it; picked += it }, onDismiss = { dismissed++ })
        }
        textAction("Estándar").performScrollTo().assertIsSelected()
            .assertHeightIsAtLeast(64.dp).assertWidthIsAtLeast(48.dp)
        assertNoOverflow("Liviana, ahorra espacio")
        textAction("Alta").performScrollTo().assertIsNotSelected().assertIsDisplayed()
            .assertHeightIsAtLeast(64.dp).performClick().assertIsSelected()
        assertNoOverflow("Mejor sonido, archivo más grande")
        textAction("Estándar").performScrollTo().assertIsNotSelected().performClick().assertIsSelected()
        textAction("Cancelar").assertIsDisplayed().assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals(listOf(true, false), picked); assertEquals(1, dismissed) }
    }

    @Test fun previaRecortadaConDuplicadosConservaEscuchaYDescarga() {
        var played: YtTrack? = null
        var downloads = 0
        compact {
            YtCollectionPreviewDialog(
                MainViewModel.YtPreview("Colección", "url", tracks = listOf(track("Uno"), track("Dos")), truncated = true),
                onClose = {}, onDownload = { downloads++ }, onPlay = { played = it }
            )
        }
        rule.onNodeWithText("2 temas (lista recortada)").assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Dos"))
        textAction("Dos").assertIsDisplayed().performClick()
        textAction("Descargar todo").assertIsEnabled().assertTouchTarget().performClick()
        assertNoOverflow("Descargar todo")
        rule.runOnIdle { assertEquals("Dos", played?.title); assertEquals(1, downloads) }
    }

    @Test fun errorDePreviaNoOfreceDescargarUnaListaVacia() {
        var closed = 0
        compact {
            YtCollectionPreviewDialog(MainViewModel.YtPreview("Colección", "url", error = "Sin conexión"),
                onClose = { closed++ }, onDownload = {}, onPlay = {})
        }
        rule.onNodeWithText("Sin conexión").assertIsDisplayed()
        rule.onNodeWithText("Cierra y vuelve a abrir la lista para reintentar.").performScrollTo().assertIsDisplayed()
        disabledTextAction("Descargar todo").assertIsNotEnabled().assertTouchTarget()
        textAction("Cerrar").assertTouchTarget().performClick()
        rule.runOnIdle { assertEquals(1, closed) }
    }

    @Test
    @Config(qualifiers = "w320dp-h720dp-notnight-mdpi")
    fun previaVaciaEnClaroMuestraUnaSalidaAccionable() {
        compact {
            YtCollectionPreviewDialog(MainViewModel.YtPreview("Colección", "url"), {}, {}, {})
        }
        rule.onNodeWithText("Sin temas visibles").assertExists()
        // El texto exacto puede decir "playlist" o "lista": la intención es que
        // la salida explique qué hacer, no fijar una palabra que el diseño renombra.
        rule.onNodeWithText("Prueba con otra", substring = true).assertExists()
        disabledTextAction("Descargar todo").assertIsNotEnabled().assertTouchTarget()
        rule.onNodeWithText("Cerrar").assertIsDisplayed()
    }
}

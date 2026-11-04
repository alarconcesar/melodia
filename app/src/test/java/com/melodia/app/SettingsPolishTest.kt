package com.melodia.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.perf.FrameMonitor
import com.melodia.app.ui.screens.ExcludedFoldersContent
import com.melodia.app.ui.screens.SettingsAboutContent
import com.melodia.app.ui.screens.SettingsAppearance
import com.melodia.app.ui.screens.SettingsPerformanceContent
import com.melodia.app.ui.screens.SettingsRow
import com.melodia.app.ui.screens.SettingsSwitch
import com.melodia.app.ui.screens.SleepTimerOptions
import com.melodia.app.ui.theme.Accents
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Semántica y disposición en 320 dp; sin ViewModel, Room ni capturas nativas. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w320dp-h640dp-xhdpi")
class SettingsPolishTest {
    @get:Rule
    val rule = createComposeRule()

    private fun role(value: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, value)

    private fun compose(
        fontScale: Float = 1f,
        scrolling: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MelodiaTheme {
                    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("area-ajustes")) {
                        if (scrolling) {
                            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { content() }
                        } else {
                            content()
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun temasYColoresTienenNombreSeleccionYCallbacks() {
        val themes = mutableListOf<Boolean?>()
        val accents = mutableListOf<String>()
        compose {
            var theme by remember { mutableStateOf<Boolean?>(null) }
            var accent by remember { mutableStateOf("Verde") }
            SettingsAppearance(theme, accent, dark = true,
                onThemeSelected = { theme = it; themes += it },
                onAccentSelected = { accent = it; accents += it })
        }
        rule.onNodeWithText("Sistema").assertIsSelected()
        listOf("Claro", "Oscuro", "Sistema").forEach { label ->
            rule.onNode(hasText(label) and role(Role.RadioButton)).performScrollTo().performClick().assertIsSelected()
        }
        Accents.forEach { accent ->
            rule.onNode(hasText(accent.name) and role(Role.RadioButton))
                .performScrollTo().performClick().assertIsSelected()
        }
        rule.runOnIdle {
            assertEquals(listOf(false, true, null), themes)
            assertEquals(Accents.map { it.name }, accents)
        }
    }

    @Test
    fun aparienciaRefluyeConLetraDobleSinPerderObjetivosDe48dp() {
        compose(fontScale = 2f) {
            SettingsAppearance(null, "Verde", dark = false, onThemeSelected = {}, onAccentSelected = {})
        }
        val area = rule.onNodeWithTag("area-ajustes").getUnclippedBoundsInRoot()
        (listOf("Claro", "Oscuro", "Sistema") + Accents.map { it.name }).forEach { label ->
            val choice = rule.onNode(hasText(label) and role(Role.RadioButton))
            choice.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            val bounds = choice.getUnclippedBoundsInRoot()
            assertTrue("$label sale por la izquierda", bounds.left >= area.left)
            assertTrue("$label sale por la derecha", bounds.right <= area.right)
        }
    }

    @Test
    fun interruptorEsUnaSolaAccionEtiquetadaYNoInvocaDosVeces() {
        val values = mutableListOf<Boolean>()
        compose(fontScale = 2f) {
            var checked by remember { mutableStateOf(false) }
            SettingsSwitch(Icons.Default.Download, "Descargas de alta calidad",
                "Mejor sonido, archivo más grande", checked) { checked = it; values += it }
        }
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        val toggle = rule.onNode(hasText("Descargas de alta calidad") and role(Role.Switch))
        toggle.assertIsOff().assertHeightIsAtLeast(48.dp).performClick().assertIsOn()
        toggle.performClick().assertIsOff()
        rule.runOnIdle { assertEquals(listOf(true, false), values) }
    }

    @Test
    fun filaDeNavegacionConservaElResumenCompletoConLetraGrande() {
        val detail = "Todos los temas de la carpeta seleccionada · duración mínima de 30 s"
        var clicks = 0
        compose(fontScale = 2f) {
            SettingsRow(Icons.Default.Folder, "Carpetas y escaneo", detail) { clicks++ }
        }
        rule.onNode(hasText("Carpetas y escaneo") and role(Role.Button))
            .assertHeightIsAtLeast(48.dp).performClick()
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(detail, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        rule.runOnIdle {
            assertEquals(1, clicks)
            assertTrue(layouts.isNotEmpty())
            assertFalse("El resumen se corta", layouts.single().hasVisualOverflow)
        }
    }

    @Test
    fun temporizadorConservaTodosLosPlazosYLaCancelacionEnPocoEspacio() {
        val minutes = mutableListOf<Int>()
        var cancels = 0
        compose(fontScale = 2f, scrolling = false) {
            SleepTimerOptions(10, onStart = { minutes += it }, onCancel = { cancels++ })
        }
        listOf(5, 10, 15, 30, 60).forEach { value ->
            rule.onNode(hasText("Pausar en $value minutos") and role(Role.Button))
                .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        }
        rule.onNodeWithText("Cancelar temporizador").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(listOf(5, 10, 15, 30, 60), minutes)
            assertEquals(1, cancels)
        }
    }

    @Test
    fun temporizadorInactivoExplicaQueNoHayNadaQueCancelar() {
        compose(scrolling = false) { SleepTimerOptions(null, onStart = {}, onCancel = {}) }
        rule.onNodeWithText("Sin temporizador activo").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun carpetaExcluidaSePuedeReincluirConUnaSolaAccion() {
        val path = "/storage/emulated/0/Grabaciones"
        val changed = mutableListOf<String>()
        compose(scrolling = false) {
            var excluded by remember { mutableStateOf(setOf(path)) }
            ExcludedFoldersContent(listOf(path), excluded) {
                changed += it
                excluded = if (it in excluded) excluded - it else excluded + it
            }
        }
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        rule.onNode(hasText("Grabaciones") and role(Role.Checkbox)).assertIsOn().performClick().assertIsOff()
        rule.runOnIdle { assertEquals(listOf(path), changed) }
    }

    @Test
    fun listaDeCarpetasLargaPermiteLlegarAlFinalConLetraDoble() {
        val paths = (1..25).map { "/storage/emulated/0/Música/Carpeta $it" }
        compose(fontScale = 2f, scrolling = false) { ExcludedFoldersContent(paths, emptySet(), onToggle = {}) }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Carpeta 25"))
        rule.onNode(hasText("Carpeta 25") and role(Role.Checkbox)).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun informacionDePrivacidadIncluyeServiciosYBusquedaAutomatica() {
        compose(fontScale = 2f, scrolling = false) { SettingsAboutContent("prueba") }
        rule.onNodeWithText("automáticamente.", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("iTunes (Apple): carátulas y metadatos").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Versión prueba").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rendimientoMuestraResultadosEnEspanolYPermiteReiniciar() {
        var resets = 0
        val stats = FrameMonitor.ScreenStats("Biblioteca", 59, 2, 36L, 100)
        compose(fontScale = 2f) { SettingsPerformanceContent(stats, listOf(stats)) { resets++ } }
        rule.onNodeWithText("59 fotogramas por segundo").assertExists()
        rule.onNodeWithText("Reiniciar medición").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, resets) }
    }
}

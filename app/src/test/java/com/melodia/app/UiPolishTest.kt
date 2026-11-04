package com.melodia.app

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.local.PlaylistSongEntity
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.SelectionToolbar
import com.melodia.app.ui.ds.MelodiaPillNav
import com.melodia.app.ui.screens.*
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Contratos de la UI real: ancho 320 dp y fuente al 130 %, sin Room ni capturas inventadas. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w320dp-h640dp-xhdpi", application = Application::class)
class UiPolishTest {
    @get:Rule val rule = createComposeRule()
    private fun song() = Song(1, "Un título largo que debe seguir siendo legible en una pantalla pequeña",
        "Artista", "Álbum", 0, 200_000, "/storage/emulated/0/Music/Melodia/tema.m4a", 0)

    private fun compose(content: @Composable () -> Unit) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, 1.3f)) {
                MelodiaTheme { Box(Modifier.fillMaxSize()) { content() } }
            }
        }
    }

    @Test fun seleccionTieneAccionesSeparadasYAccesibles() {
        var all = false; var deleted = false; var closed = false
        compose { SelectionToolbar(125, false, true, { all = true }, { deleted = true }, { closed = true }) }
        rule.onNodeWithText("125 seleccionados").assertIsDisplayed()
        rule.onNodeWithText("Todo").assertIsDisplayed().performClick()
        rule.onNodeWithText("Borrar").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Cancelar selección").performClick()
        assertEquals(true, all); assertEquals(true, deleted); assertEquals(true, closed)
    }

    @Test fun seleccionVaciaNoPermiteBorrar() {
        compose { SelectionToolbar(0, false, false, {}, {}, {}) }
        rule.onNodeWithText("Borrar").assertIsNotEnabled()
        rule.onNodeWithText("Todo").assertIsNotEnabled()
    }

    @Test fun todoMarcadoPermiteDesmarcarSinSalir() {
        var cleared = false
        compose { SelectionToolbar(5, true, true, { cleared = true }, {}, {}) }
        rule.onNodeWithText("Nada").performClick()
        assertEquals(true, cleared)
    }

    private fun info(onClose: () -> Unit = {}, onShare: () -> Unit = {}, onSelect: () -> Unit = {}) {
        compose {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                SongInfoContent(song(), false, "Melodia", "No disponible", "M4A", "5.2 MB",
                    {}, {}, {}, {}, {}, {}, onShare, onSelect, {}, {}, onClose)
            }
        }
    }

    @Test fun fichaNoPierdeAccionesConLetraGrande() {
        var shared = false; var selected = false
        info(onShare = { shared = true }, onSelect = { selected = true })
        rule.onNodeWithText("Compartir").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Elegir varios").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Excluir carpeta").performScrollTo().assertIsDisplayed()
        assertEquals(true, shared); assertEquals(true, selected)
    }

    @Test fun fichaTieneCierreVisibleYDatosReales() {
        var closed = false
        info(onClose = { closed = true })
        rule.onNodeWithText("Información del tema").assertIsDisplayed()
        rule.onNodeWithText("M4A").assertExists()
        rule.onNodeWithContentDescription("Cerrar información").performClick()
        assertEquals(true, closed)
    }

    @Test fun vacioConPermisoNoVuelveAPedirPermiso() {
        var reload = false
        compose { LibraryEmptyContent(true, {}, {}, { reload = true }, {}) }
        rule.onNodeWithText("Conceder permiso de audio").assertDoesNotExist()
        rule.onNodeWithText("Actualizar biblioteca").performScrollTo().performClick()
        assertEquals(true, reload)
    }

    @Test fun vacioSinPermisoTieneSalidaAAjustes() {
        var settings = false
        compose { LibraryEmptyContent(false, {}, { settings = true }, {}, {}) }
        rule.onNodeWithText("Abrir ajustes de la app").performScrollTo().performClick()
        assertEquals(true, settings)
    }

    @Test fun playlistComparteCabeceraYReproduceIdNoIndice() {
        var played = -1L
        val links = listOf(PlaylistSongEntity(1, 91, 0, "/a.mp3", "Primer tema", "Artista", "Álbum", 180_000))
        compose { PlaylistDetail("Mi lista", links, {}, { played = it }, {}, { _, _ -> }, {}) }
        rule.onNodeWithText("Reproducir").performClick()
        assertEquals(91L, played)
    }

    @Test fun letraVaciaReintentaYSale() {
        var retried = false; var closed = false
        compose { LyricsContent(song(), MainViewModel.LyricsUi.None, 0, 200_000,
            { closed = true }, { retried = true }, {}) }
        rule.onNodeWithText("Sin letra para este tema").assertExists()
        rule.onNodeWithText("Reintentar").performClick()
        rule.onNodeWithContentDescription("Volver").performClick()
        assertEquals(true, retried); assertEquals(true, closed)
    }

    @Test fun navegacionNombraTodasLasSecciones() {
        var tab = -1
        compose { MelodiaPillNav(0, { tab = it }) }
        listOf("Inicio", "Explorar", "Listas", "Favoritos", "Online").forEach {
            rule.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        rule.onNodeWithContentDescription("Favoritos").performClick()
        assertEquals(3, tab)
    }

    @Test fun colaConDuplicadosArrancaDespuesDeLaOcurrenciaActual() {
        val a = Song(1, "A", "Artista", "Álbum", 0, 100_000, "/a.mp3", 0)
        val b = Song(2, "B", "Artista", "Álbum", 0, 100_000, "/b.mp3", 0)
        val c = Song(3, "C", "Artista", "Álbum", 0, 100_000, "/c.mp3", 0)
        compose {
            QueueSheetContent(listOf(a, b, a, c), a.id, currentIndex = 2,
                shuffle = false, repeatMode = 0, onPlay = {}, onMove = { _, _ -> },
                onRemove = {}, onToggleShuffle = {}, onCycleRepeat = {},
                onClear = {}, onSave = {}, onClose = {})
        }
        rule.onNodeWithText("A continuación · 1").assertExists()
        rule.onNodeWithText("C").assertExists()
        rule.onAllNodesWithText("B").assertCountEquals(0)
    }

    @Test fun alcanceDeSeleccionSeRegistraConLoVisible() {
        var ids: List<Long> = emptyList()
        compose {
            CompositionLocalProvider(
                com.melodia.app.ui.ds.LocalSelection provides
                    com.melodia.app.ui.ds.SelectionState(reportScope = { ids = it })
            ) { com.melodia.app.ui.ds.SelectionScope(listOf(4L, 8L, 15L)) }
        }
        rule.waitForIdle()
        assertEquals(listOf(4L, 8L, 15L), ids)
    }

    @Test fun albumHomonimoConservaSuArtista() {
        val uno = Song(1, "Uno", "Ana", "En vivo", 0, 1000, "/1.mp3", 0, albumArtist = "Ana")
        val dos = Song(2, "Dos", "Beto", "En vivo", 0, 1000, "/2.mp3", 0, albumArtist = "Beto")
        val mine = com.melodia.app.data.songsForAlbum(listOf(uno, dos),
            com.melodia.app.data.AlbumIdentity("En vivo", "Beto"))
        assertEquals(listOf(2L), mine.map { it.id })
    }

    @Test fun todoAcentoClaroSeLeeSobreFondoYComoRelleno() {
        fun lum(c: androidx.compose.ui.graphics.Color): Double {
            fun f(v: Float): Double {
                val x = v.toDouble()
                return if (x <= 0.03928) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * f(c.red) + 0.7152 * f(c.green) + 0.0722 * f(c.blue)
        }
        fun ratio(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
            val x = lum(a); val y = lum(b)
            return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
        }
        val fondo = androidx.compose.ui.graphics.Color(0xFFF4F6F4)
        val blanco = androidx.compose.ui.graphics.Color.White
        com.melodia.app.ui.theme.Accents.forEach { a ->
            assertTrue("${a.name}: texto sobre fondo", ratio(a.light, fondo) >= 4.5)
            assertTrue("${a.name}: blanco sobre relleno", ratio(blanco, a.light) >= 4.5)
        }
    }
}

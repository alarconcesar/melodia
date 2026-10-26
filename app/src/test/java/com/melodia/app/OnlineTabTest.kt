package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.OnlineTabContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * La pestaña Online se rediseñó en 4.8: buscador único, menú ⋮ y resultados en
 * filas planas. Se compone el contenido puro (sin ViewModel) para verificar
 * estados y acciones sin Room ni servicio de audio.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class OnlineTabTest {

    @get:Rule
    val rule = createComposeRule()

    private fun tracks(n: Int): List<YtTrack> = (1..n).map { i ->
        YtTrack(
            id = "v$i", title = "Tema $i", artist = "Artista $i",
            durationSec = 180L + i, thumbUrl = null, url = "https://youtu.be/v$i"
        )
    }

    private fun componer(
        query: String = "",
        results: List<YtTrack> = emptyList(),
        loading: Boolean = false,
        error: String? = null,
        errorRetry: Boolean = true,
        recent: List<String> = emptyList(),
        history: List<YtTrack> = emptyList(),
        onPlay: (YtTrack) -> Unit = {},
        onOpenDownloads: () -> Unit = {},
        onOpenImport: () -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                OnlineTabContent(
                    query = query,
                    results = results,
                    loading = loading,
                    error = error,
                    errorRetry = errorRetry,
                    dlProgress = emptyMap(),
                    busyId = null,
                    recent = recent,
                    history = history,
                    hasMore = false,
                    moreLoading = false,
                    saver = false,
                    highQ = true,
                    onQueryChange = {},
                    onSearch = {},
                    onClearQuery = {},
                    onPlay = onPlay,
                    onDownload = {},
                    onDownloadAll = {},
                    onLoadMore = {},
                    onOpenDownloads = onOpenDownloads,
                    onOpenImport = onOpenImport,
                    onToggleSaver = {},
                    onToggleQuality = {}
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun muestraLosResultadosEnFilas() {
        componer(query = "prueba", results = tracks(3))
        rule.onNodeWithText("3 resultados").assertExists()
        rule.onNodeWithText("Tema 1").assertExists()
        rule.onNodeWithText("Artista 2 · 3:02").assertExists()
    }

    @Test
    fun elEstadoVacioInvitaABuscar() {
        componer()
        rule.onNodeWithText("Busca música en YouTube").assertExists()
    }

    @Test
    fun elMenuConcentraDescargasEImportacion() {
        componer()
        rule.onNodeWithContentDescription("Opciones").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Descargas").assertExists()
        rule.onNodeWithText("Importar playlist").assertExists()
        rule.onNodeWithText("Calidad alta").assertExists()
    }

    @Test
    fun tocarUnaFilaLaReproduce() {
        var tocado: YtTrack? = null
        componer(query = "x", results = tracks(2), onPlay = { tocado = it })
        rule.onNodeWithText("Tema 2").performClick()
        rule.waitForIdle()
        assert(tocado?.id == "v2") { "se esperaba v2, llegó ${tocado?.id}" }
    }
}

package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.YtSearchItem
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.OnlineTabContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Pestaña Online: mezcla de temas y playlists/álbumes en la misma lista, menú
 * de opciones y descarga de una colección entera. Se compone el contenido puro
 * (sin ViewModel) para verificar estados y acciones sin Room ni audio.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class OnlineTabTest {

    @get:Rule
    val rule = createComposeRule()

    private fun tracks(n: Int): List<YtSearchItem.Track> = (1..n).map { i ->
        YtSearchItem.Track(
            YtTrack(
                id = "v$i", title = "Tema $i", artist = "Artista $i",
                durationSec = 180L + i, thumbUrl = null, url = "https://youtu.be/v$i"
            )
        )
    }

    private fun collection() = YtSearchItem.Collection(
        id = "PL123", title = "Queen Greatest Hits", uploader = "Queen Official",
        count = 17, thumbUrl = null, url = "https://youtube.com/playlist?list=PL123"
    )

    private fun componer(
        items: List<YtSearchItem>? = null,
        query: String = "",
        loading: Boolean = false,
        error: String? = null,
        errorRetry: Boolean = true,
        recent: List<String> = emptyList(),
        history: List<YtTrack> = emptyList(),
        notice: String? = null,
        onPlay: (YtTrack) -> Unit = {},
        onDownloadCollection: (YtSearchItem.Collection) -> Unit = {},
        onOpenDownloads: () -> Unit = {},
        onOpenImport: () -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                OnlineTabContent(
                    query = query,
                    items = items ?: emptyList(),
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
                    onToggleQuality = {},
                    notice = notice,
                    onDownloadCollection = onDownloadCollection
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun muestraLosResultadosEnFilas() {
        componer(items = tracks(3), query = "prueba")
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
        componer(items = tracks(2), query = "x", onPlay = { tocado = it })
        rule.onNodeWithText("Tema 2").performClick()
        rule.waitForIdle()
        assert(tocado?.id == "v2") { "se esperaba v2, llegó ${tocado?.id}" }
    }

    @Test
    fun lasPlaylistsSeMezclanYSeDescarganEnteras() {
        var pedida: YtSearchItem.Collection? = null
        componer(
            items = tracks(2) + collection(),
            query = "queen",
            onDownloadCollection = { pedida = it }
        )
        rule.onNodeWithText("3 resultados").assertExists()
        rule.onNodeWithText("Queen Greatest Hits").assertExists()
        rule.onNodeWithText("Queen Official · 17 temas · Playlist o álbum").assertExists()
        rule.onNodeWithText("Queen Greatest Hits").performClick()
        rule.waitForIdle()
        assert(pedida?.id == "PL123") { "no se pidió la colección: ${pedida?.id}" }
    }

    @Test
    fun elAvisoDePlaylistEnColaSeMuestra() {
        componer(notice = "«Queen»: 17 temas a la cola de descargas")
        rule.onNodeWithText("«Queen»: 17 temas a la cola de descargas").assertExists()
    }
}

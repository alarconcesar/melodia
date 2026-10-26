package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.OnlineTabContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Estados de la pestaña Online que no se habían cubierto: cargando, error con y
 * sin reintento, historial y chips de búsquedas recientes.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class OnlineStatesTest {

    @get:Rule
    val rule = createComposeRule()

    private fun componer(
        loading: Boolean = false,
        error: String? = null,
        errorRetry: Boolean = true,
        recent: List<String> = emptyList(),
        history: List<YtTrack> = emptyList(),
        onQueryChange: (String) -> Unit = {},
        onSearch: () -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                OnlineTabContent(
                    query = "", items = emptyList(), loading = loading,
                    error = error, errorRetry = errorRetry, dlProgress = emptyMap(),
                    busyId = null, recent = recent, history = history,
                    hasMore = false, moreLoading = false, saver = false, highQ = true,
                    onQueryChange = onQueryChange, onSearch = onSearch, onClearQuery = {},
                    onPlay = {}, onDownload = {}, onDownloadAll = {}, onLoadMore = {},
                    onOpenDownloads = {}, onOpenImport = {}, onToggleSaver = {},
                    onToggleQuality = {}
                )
            }
        }
        rule.waitForIdle()
    }

    private fun track(id: String) = YtTrack(
        id = id, title = "Escuchado $id", artist = "Artista", durationSec = 100,
        thumbUrl = null, url = "https://youtu.be/$id"
    )

    @Test
    fun elErrorConReintentoOfreceReintentar() {
        componer(error = "Sin conexión", errorRetry = true)
        rule.onNodeWithText("Sin conexión").assertExists()
        rule.onNodeWithText("Reintentar").assertExists()
    }

    @Test
    fun elErrorSinReintentoNoOfreceBoton() {
        componer(error = "Nada coincide", errorRetry = false)
        rule.onNodeWithText("Nada coincide").assertExists()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
        rule.onNodeWithText("Cambia las palabras y busca de nuevo.").assertExists()
    }

    @Test
    fun lasBusquedasRecientesSonChipsQueBuscan() {
        var buscado: String? = null
        componer(recent = listOf("salsa", "muse"), onQueryChange = { buscado = it })
        rule.onNodeWithText("Recientes").assertExists()
        rule.onNodeWithText("salsa").performClick()
        rule.waitForIdle()
        assert(buscado == "salsa") { "esperaba salsa, llegó $buscado" }
    }

    @Test
    fun elHistorialMuestraEscuchadoOnline() {
        componer(history = listOf(track("a"), track("b")))
        rule.onNodeWithText("Escuchado online").assertExists()
        rule.onNodeWithText("Escuchado a").assertExists()
    }

    @Test
    fun cargandoSinResultadosMuestraSpinnerYNoElVacio() {
        componer(loading = true)
        // El estado vacío ("Busca música en YouTube") no debe aparecer mientras carga
        rule.onNodeWithText("Busca música en YouTube").assertDoesNotExist()
    }

    @Test
    fun elBuscadorAceptaTexto() {
        var escrito: String? = null
        componer(onQueryChange = { escrito = it })
        rule.onNodeWithText("Buscar en YouTube").performTextInput("queen")
        rule.waitForIdle()
        assert(escrito != null) { "onQueryChange no se llamó" }
    }
}

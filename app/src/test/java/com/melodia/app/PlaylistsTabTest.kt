package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.local.PlaylistEntity
import com.melodia.app.data.local.PlaylistSongEntity
import com.melodia.app.data.model.Song
import com.melodia.app.ui.screens.PlaylistsTab
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Listas: estado vacío, tarjeta de crear, conteos y toque para abrir. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PlaylistsTabTest {

    @get:Rule
    val rule = createComposeRule()

    private fun song(id: Long) = Song(
        id = id, title = "Tema $id", artist = "Artista", album = "Álbum",
        albumId = 1L, durationMs = 200_000, dataPath = "/m/$id.mp3", dateAdded = 0
    )

    private fun componer(
        playlists: List<PlaylistEntity> = emptyList(),
        links: List<PlaylistSongEntity> = emptyList(),
        onOpen: (Long) -> Unit = {},
        onCreate: () -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                PlaylistsTab(
                    playlists = playlists,
                    links = links,
                    songs = listOf(song(1), song(2)),
                    onOpen = onOpen,
                    onCreate = onCreate,
                    onDelete = {}, onRename = {}, onPlay = {}, onPin = {}, onCover = { _, _ -> }
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun sinListasMuestraElVacioYLaTarjetaDeCrear() {
        componer()
        rule.onNodeWithText("Nueva playlist").assertExists()
        rule.onNodeWithText("Sin listas todavía").assertExists()
    }

    @Test
    fun laTarjetaDeCrearDisparaElCallback() {
        var creada = false
        componer(onCreate = { creada = true })
        rule.onNodeWithText("Nueva playlist").performClick()
        rule.waitForIdle()
        assert(creada) { "no se pidió crear la playlist" }
    }

    @Test
    fun unaListaMuestraSuConteoYSeAbreAlTocar() {
        var abierta = -1L
        val pl = PlaylistEntity(id = 7, name = "Favoritas de siempre")
        val links = listOf(
            PlaylistSongEntity(playlistId = 7, songId = 1),
            PlaylistSongEntity(playlistId = 7, songId = 2)
        )
        componer(playlists = listOf(pl), links = links, onOpen = { abierta = it })
        rule.onNodeWithText("Favoritas de siempre").assertExists()
        rule.onNodeWithText("2 temas", substring = true).assertExists()
        rule.onNodeWithText("Favoritas de siempre").performClick()
        rule.waitForIdle()
        assert(abierta == 7L) { "esperaba abrir 7, llegó $abierta" }
    }

    @Test
    fun unaListaVaciaSeIndicaComoTal() {
        val pl = PlaylistEntity(id = 9, name = "Recién creada")
        componer(playlists = listOf(pl))
        rule.onNodeWithText("Recién creada").assertExists()
        rule.onNodeWithText("Vacía").assertExists()
    }
}

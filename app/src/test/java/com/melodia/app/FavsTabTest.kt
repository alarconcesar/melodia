package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.LocalSelection
import com.melodia.app.ui.ds.SelectionState
import com.melodia.app.ui.screens.FavsTab
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Favoritos: el corazón de la fila tiene que quitar el tema (antes era un icono
 * decorativo y solo se podía deslizando) y tocar el tema lo reproduce.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class FavsTabTest {

    @get:Rule
    val rule = createComposeRule()

    private fun song(id: Long, title: String) = Song(
        id = id, title = title, artist = "Artista", album = "Álbum",
        albumId = 0L, durationMs = 200_000L, dataPath = "/x/$id.mp3",
        dateAdded = 0L
    )

    private fun componer(
        onFav: (Long) -> Unit = {},
        onPlayAt: (Int) -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                FavsTab(
                    favSongs = listOf(song(1, "Primera"), song(2, "Segunda")),
                    currentId = null,
                    playing = false,
                    favs = setOf(1L, 2L),
                    onPlayAt = onPlayAt,
                    onFav = onFav,
                    onAdd = {}
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun elCorazonQuitaDeFavoritos() {
        var quitado: Long? = null
        componer(onFav = { quitado = it })
        rule.onAllNodesWithContentDescription("Quitar de favoritos")[0].performClick()
        rule.waitForIdle()
        assertTrue("no se llamó onFav", quitado != null)
    }

    @Test
    fun enModoSeleccionElTapNoReproduce() {
        var indice = -1
        rule.setContent {
            MelodiaTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalSelection provides SelectionState(selecting = true, selected = setOf(1L), toggle = {})
                ) {
                    FavsTab(
                        favSongs = listOf(song(1, "Primera"), song(2, "Segunda")),
                        currentId = null, playing = false, favs = setOf(1L, 2L),
                        onPlayAt = { indice = it }, onFav = {}, onAdd = {}
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Segunda").performClick()
        rule.waitForIdle()
        assert(indice == -1) { "en selección no debe reproducir, índice=$indice" }
    }

    @Test
    fun tocarLaFilaReproduce() {
        var indice = -1
        componer(onPlayAt = { indice = it })
        rule.onNodeWithText("Segunda").performClick()
        rule.waitForIdle()
        assertEquals(1, indice)
    }
}

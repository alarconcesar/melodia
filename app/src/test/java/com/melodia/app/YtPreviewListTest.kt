package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.YtPreviewList
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regresión del crash "Key ... was already used": las playlists de YouTube
 * pueden repetir el mismo video y la lista debe aceptar duplicados.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class YtPreviewListTest {

    @get:Rule
    val rule = createComposeRule()

    private fun track(id: String, title: String) = YtTrack(
        id = id, title = title, artist = "Artista", durationSec = 200,
        thumbUrl = null, url = "https://youtu.be/$id"
    )

    @Test
    fun aceptaTemasRepetidos() {
        var tocado: YtTrack? = null
        val lista = listOf(
            track("abc", "Uno"),
            track("abc", "Uno (repetido)"),
            track("xyz", "Dos")
        )
        rule.setContent {
            MelodiaTheme {
                YtPreviewList(tracks = lista, onPlay = { tocado = it })
            }
        }
        rule.waitForIdle()
        // Ambas filas del mismo video deben existir (antes explotaba al medir).
        rule.onAllNodesWithText("Uno").assertCountEquals(1)
        rule.onAllNodesWithText("Uno (repetido)").assertCountEquals(1)
        rule.onAllNodesWithText("Dos")[0].performClick()
        rule.waitForIdle()
        assert(tocado?.id == "xyz") { "esperaba xyz, llegó ${tocado?.id}" }
    }
}

package com.melodia.app

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.model.Song
import com.melodia.app.ui.screens.MelodiaScreen
import com.melodia.app.ui.theme.MelodiaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Compone Inicio CON biblioteca (lo que ve César en su celular): filas con gestos,
 * carrusel de álbumes, miniplayer. Es el camino que el estado vacío no toca.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HomeConDatosTest {

    @get:Rule
    val rule = createComposeRule()

    private fun fake(vm: MainViewModel, field: String, value: Any) {
        val f = MainViewModel::class.java.getDeclaredField(field)
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any>).value = value
    }

    @Test
    fun inicioConCanciones() {
        val app = ApplicationProvider.getApplicationContext<Context>() as MelodiaApp
        val vm = app.appContainer.makeMainViewModel()

        val songs = (1L..6L).map { i ->
            Song(
                id = i, title = "Tema $i", artist = if (i % 2 == 0L) "Artista A" else "Artista B",
                album = if (i <= 3) "Album Uno" else "Album Dos", albumId = if (i <= 3) 10L else 20L,
                durationMs = 180_000L + i * 1000, dataPath = "/sdcard/Music/t$i.mp3", dateAdded = i
            )
        }
        fake(vm, "_songs", songs)
        fake(vm, "_loaded", true)
        fake(vm, "_favIds", setOf(2L, 4L))
        fake(vm, "_currentId", 3L)

        rule.setContent { MelodiaTheme { MelodiaScreen(vm) } }
        rule.waitForIdle()
    }
}

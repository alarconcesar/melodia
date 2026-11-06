package com.melodia.app

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.LocalSelection
import com.melodia.app.ui.ds.SelectionState
import com.melodia.app.ui.ds.TrackRow
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Gestos sobre la fila real, inyectados desde un padre inmóvil; no certifica el dibujo en Android físico. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w320dp-h640dp-mdpi", application = Application::class)
class TrackRowSwipeTest {
    @get:Rule val rule = createComposeRule()
    private var taps = 0
    private var favorites = 0
    private var queued = 0
    private var options = 0
    private val song = Song(901, "Tema de prueba", "Artista", "Álbum", 0, 180_000, "/test.m4a", 0)

    @Before fun resetSafeMode() {
        SafeMode.apagarTodo(ApplicationProvider.getApplicationContext())
    }

    private fun composeRow(selection: SelectionState = SelectionState()) {
        rule.setContent {
            MelodiaTheme {
                CompositionLocalProvider(LocalSelection provides selection) {
                    Box(Modifier.width(320.dp).testTag("swipe-host")) {
                        TrackRow(song, false, false, { taps++ }, { favorites++ }, { options++ }, { queued++ })
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun row() = rule.onNode(hasText(song.title) and hasClickAction())
    private fun host() = rule.onNodeWithTag("swipe-host")
    private fun rowLeft() = row().getUnclippedBoundsInRoot().left.value

    private fun assertFingerTracking(direction: Float) {
        composeRow()
        val initial = rowLeft()
        val start = if (direction > 0) 96f else 224f
        rule.mainClock.autoAdvance = false
        host().performTouchInput { down(Offset(start, center.y)) }
        rule.mainClock.advanceTimeByFrame()
        try {
            for (distance in listOf(44f, 60f, 76f, 92f, 108f, 124f)) {
                host().performTouchInput { moveTo(Offset(start + direction * distance, center.y), delayMillis = 32) }
                rule.mainClock.advanceTimeByFrame()
                val expected = initial + direction * distance
                val actual = rowLeft()
                assertEquals("La fila debe seguir al dedo sin realimentar su desplazamiento: distancia=$distance", expected, actual, 3f)
            }
        } finally {
            host().performTouchInput { up() }
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()
        assertEquals(initial, rowLeft(), .1f)
        assertEquals(0, taps)
        assertEquals(if (direction > 0) 1 else 0, queued)
        assertEquals(if (direction < 0) 1 else 0, favorites)
    }

    @Test fun derechaSigueDedoSinRetroceder() = assertFingerTracking(1f)
    @Test fun izquierdaSigueDedoSinRetroceder() = assertFingerTracking(-1f)

    @Test fun unSoloDeslizamientoEjecutaUnaSolaAccionYNoReproduce() {
        composeRow()
        rule.mainClock.autoAdvance = false
        host().performTouchInput { down(Offset(80f, center.y)) }
        rule.mainClock.advanceTimeByFrame()
        try {
            host().performTouchInput { moveTo(Offset(210f, center.y), delayMillis = 40) }
            rule.mainClock.advanceTimeByFrame()
        } finally {
            host().performTouchInput { up() }
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()
        assertEquals(1, queued)
        assertEquals(0, favorites)
        assertEquals(0, taps)
    }

    @Test fun toqueSinDeslizamientoNoMueveNiEncola() {
        composeRow()
        val initial = rowLeft()
        host().performTouchInput { click(Offset(120f, center.y)) }
        rule.waitForIdle()
        assertEquals(initial, rowLeft(), .1f)
        assertEquals(0, queued)
        assertEquals(0, favorites)
    }

    @Test fun modoSeleccionDesactivaElDeslizamiento() {
        composeRow(SelectionState(selecting = true, selected = setOf(song.id)))
        val initial = rowLeft()
        host().performTouchInput { swipeRight(startX = 96f, endX = 220f, durationMillis = 180) }
        rule.waitForIdle()
        assertEquals(initial, rowLeft(), .1f)
        assertEquals(0, queued)
        assertEquals(0, favorites)
    }
}

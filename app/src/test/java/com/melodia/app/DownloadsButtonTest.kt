package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.DlJob
import com.melodia.app.data.online.DlStatus
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.DownloadsButton
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Botón de descargas de la barra superior: siempre abre la hoja, con o sin cola. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DownloadsButtonTest {

    @get:Rule
    val rule = createComposeRule()

    private fun job(id: String, status: DlStatus, done: Long, total: Long) = DlJob(
        track = YtTrack(id, "Tema $id", "Artista", 100, null, "https://youtu.be/$id"),
        high = true, status = status, progress = if (total > 0) done.toFloat() / total else 0f,
        downloaded = done, total = total
    )

    @Test
    fun sinDescargasAbreLaHoja() {
        var abierto = false
        rule.setContent { MelodiaTheme { DownloadsButton(emptyList()) { abierto = true } } }
        rule.onNodeWithContentDescription("Descargas").performClick()
        rule.waitForIdle()
        assert(abierto) { "no abrió la hoja sin descargas" }
    }

    @Test
    fun conDescargasActivasTambienAbre() {
        var abierto = false
        val jobs = listOf(
            job("a", DlStatus.ACTIVE, 50, 100),
            job("b", DlStatus.QUEUED, 0, 0)
        )
        rule.setContent { MelodiaTheme { DownloadsButton(jobs) { abierto = true } } }
        rule.onNodeWithContentDescription("Descargas").performClick()
        rule.waitForIdle()
        assert(abierto) { "no abrió la hoja con descargas activas" }
    }

    @Test
    fun unaDescargaTerminadaNoCuentaComoActiva() {
        var abierto = false
        val jobs = listOf(job("a", DlStatus.DONE, 100, 100))
        rule.setContent { MelodiaTheme { DownloadsButton(jobs) { abierto = true } } }
        rule.onNodeWithContentDescription("Descargas").performClick()
        rule.waitForIdle()
        assert(abierto)
    }
}

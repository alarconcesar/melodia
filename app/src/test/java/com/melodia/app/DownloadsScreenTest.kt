package com.melodia.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.online.DlJob
import com.melodia.app.data.online.DlStatus
import com.melodia.app.data.online.YtRepository
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.screens.DownloadsScreenContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Pantalla de descargas: acciones globales, estados y historial. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DownloadsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun job(id: String, status: DlStatus, done: Long = 0, total: Long = 0) = DlJob(
        track = YtTrack(id, "Tema $id", "Artista", 200, null, "https://youtu.be/$id"),
        high = true, status = status,
        progress = if (total > 0) done.toFloat() / total else 0f,
        downloaded = done, total = total
    )

    private fun componer(
        jobs: List<DlJob> = emptyList(),
        history: List<YtRepository.DlRecord> = emptyList(),
        onResumeAll: () -> Unit = {},
        onPauseAll: () -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        rule.setContent {
            MelodiaTheme {
                DownloadsScreenContent(
                    jobs = jobs, history = history, onClose = onClose,
                    onResumeAll = onResumeAll, onPauseAll = onPauseAll,
                    onPause = {}, onResume = {}, onCancel = {}, onClearHistory = {}
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun sinDescargasMuestraElVacio() {
        componer()
        rule.onNodeWithText("Sin descargas en curso").assertExists()
    }

    @Test
    fun conVariasPausadasApareceReanudarTodo() {
        var reanudado = false
        componer(
            jobs = listOf(
                job("a", DlStatus.PAUSED, 10, 100),
                job("b", DlStatus.ERROR)
            ),
            onResumeAll = { reanudado = true }
        )
        rule.onNodeWithText("Reanudar todo").performClick()
        rule.waitForIdle()
        assert(reanudado) { "no se reanudó todo" }
    }

    @Test
    fun conDescargasActivasSePuedePausarTodo() {
        var pausado = false
        componer(
            jobs = listOf(job("a", DlStatus.ACTIVE, 50, 100), job("b", DlStatus.QUEUED)),
            onPauseAll = { pausado = true }
        )
        rule.onNodeWithText("Pausar todo").performClick()
        rule.waitForIdle()
        assert(pausado) { "no se pausó todo" }
    }

    @Test
    fun unaDescargaTerminadaNoSeListaEnCurso() {
        componer(jobs = listOf(job("a", DlStatus.DONE, 100, 100)))
        rule.onNodeWithText("Sin descargas en curso").assertExists()
    }

    @Test
    fun elHistorialSeMuestra() {
        componer(
            history = listOf(
                YtRepository.DlRecord(
                    title = "Tema viejo", artist = "Muse", high = true, at = 1_700_000_000_000L
                )
            )
        )
        rule.onNodeWithText("Historial").assertExists()
        rule.onNodeWithText("Tema viejo").assertExists()
    }

    @Test
    fun atrasCierraLaPantalla() {
        var cerrada = false
        componer(onClose = { cerrada = true })
        rule.onNodeWithContentDescription("Atrás").performClick()
        rule.waitForIdle()
        assert(cerrada) { "no cerró" }
    }
}

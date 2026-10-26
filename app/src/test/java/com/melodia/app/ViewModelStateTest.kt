package com.melodia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.model.Song
import com.melodia.app.player.YT_STREAM_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/**
 * Flujos del ViewModel que la UI da por hechos: selección múltiple, borrado sin
 * selección y reproducción de ids inexistentes.
 *
 * Nota: favoritos y playlists NO se prueban acá porque Room necesita SQLite
 * nativo y este VPS es aarch64 (`Architecture 'aarch64' is not supported`).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class ViewModelStateTest {

    private lateinit var vm: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val app = ApplicationProvider.getApplicationContext<Context>() as MelodiaApp
        vm = app.appContainer.makeMainViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Espera activa: Room escribe en IO y el estado llega después. */
    private fun esperar(desc: String, cond: () -> Boolean) {
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < 4_000) {
            // Room avisa de los cambios por el looper principal: sin idle no
            // llegan las emisiones nuevas.
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (cond()) return
            Thread.sleep(20)
        }
        assertTrue("timeout esperando: $desc", cond())
    }

    @Test
    fun seleccionMultipleNoIncluyeElStream() {
        vm.startSelection(5L)
        assertTrue(vm.selecting.value)
        assertEquals(setOf(5L), vm.selected.value)

        vm.toggleSelected(6L)
        assertEquals(setOf(5L, 6L), vm.selected.value)

        vm.toggleSelected(5L)
        assertEquals(setOf(6L), vm.selected.value)

        // El id reservado de los streams online nunca entra a la selección
        vm.toggleSelected(YT_STREAM_ID)
        assertEquals(setOf(6L), vm.selected.value)

        vm.selectAll(listOf(1L, 2L, YT_STREAM_ID))
        assertEquals(setOf(1L, 2L), vm.selected.value)

        vm.clearSelection()
        assertFalse(vm.selecting.value)
        assertTrue(vm.selected.value.isEmpty())
    }

    @Test
    fun borrarSinSeleccionNoHaceNada() {
        var pidioConfirmacion = false
        vm.deleteSelected { pidioConfirmacion = true }
        Thread.sleep(120)
        assertFalse(pidioConfirmacion)
        assertFalse(vm.selecting.value)
    }

    @Test
    fun reproducirUnIdInexistenteNoRevienta() {
        vm.playSong(999_999L)
        vm.playById(999_999L)
        Thread.sleep(80)
        // Sigue vivo y sin cola: el id no existe en la biblioteca
        assertTrue(vm.queueSongs.value.isEmpty())
    }
}

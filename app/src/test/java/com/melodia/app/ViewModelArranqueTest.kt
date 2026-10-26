package com.melodia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.util.Collections

/**
 * Regresión del crash de arranque: en el celular los coroutines del `init` del
 * ViewModel corren DE INMEDIATO (Dispatchers.Main.immediate). Si tocan un estado
 * declarado más abajo en la clase, el constructor revienta con NPE y la app no
 * abre. `Unconfined` reproduce esa inmediatez en la JVM.
 *
 * La excepción no revienta el constructor: se va al handler global de hilos, así
 * que aquí se intercepta ese handler — sin esto el test pasaría igual con el bug.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class ViewModelArranqueTest {

    private val errores: MutableList<Throwable> = Collections.synchronizedList(mutableListOf())
    private var previo: Thread.UncaughtExceptionHandler? = null

    @Before fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        previo = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> errores.add(e) }
    }

    @After fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previo)
        Dispatchers.resetMain()
    }

    @Test
    fun elArranqueNoLanzaExcepciones() {
        val app = ApplicationProvider.getApplicationContext<Context>() as MelodiaApp
        val vm = app.appContainer.makeMainViewModel()
        // Los estados que alimenta el init deben estar listos y sin reventar
        vm.dlProgress.value
        vm.loaded.value
        vm.dlHistory.value
        assertTrue(
            "El arranque lanzó excepciones: " + errores.joinToString("\n"),
            errores.isEmpty()
        )
    }
}

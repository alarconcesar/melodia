package com.melodia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.DownloadMetaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** La ficha de descarga debe sobrevivir y no inventar datos si no existe. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DownloadMetaStoreTest {

    @Test
    fun guardaYRecuperaLaFicha() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = DownloadMetaStore(ctx)
        store.put(
            "Jorge Drexler - Toco madera.m4a",
            DownloadMetaStore.Meta(
                title = "Toco madera",
                artist = "Jorge Drexler",
                album = "Toco madera",
                artPath = "file:///tmp/portada.jpg"
            )
        )
        val m = store.get("Jorge Drexler - Toco madera.m4a")
        assertEquals("Toco madera", m?.title)
        assertEquals("Jorge Drexler", m?.artist)
        assertEquals("file:///tmp/portada.jpg", m?.artPath)
    }

    @Test
    fun sinFichaDevuelveNull() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertNull(DownloadMetaStore(ctx).get("no-existe.m4a"))
    }

    @Test
    fun sinCaratulaElCampoQuedaVacio() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = DownloadMetaStore(ctx)
        store.put("x.m4a", DownloadMetaStore.Meta("T", "A", "Al", null))
        assertNull(store.get("x.m4a")?.artPath)
    }
}

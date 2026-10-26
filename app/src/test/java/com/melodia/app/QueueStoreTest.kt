package com.melodia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.model.Song
import com.melodia.app.player.QueueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * La cola persistida es lo que permite reabrir la app y seguir donde ibas.
 * Si el JSON se rompe o el índice queda fuera de rango, la app no debe caerse.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class QueueStoreTest {

    private fun ctx(): Context = ApplicationProvider.getApplicationContext()

    private fun song(id: Long, title: String, track: Int = 0) = Song(
        id = id, title = title, artist = "Artista", album = "Album", albumId = 7,
        durationMs = 180_000, dataPath = "/m/$id.mp3", dateAdded = id,
        trackNumber = track, discNumber = 1, albumArtist = "Artista del álbum"
    )

    @Test
    fun guardaYRecuperaLaColaCompleta() {
        val store = QueueStore(ctx()).apply { clear() }
        val songs = listOf(song(1, "Uno", 1), song(2, "Dos", 2))
        store.save(songs, index = 1, positionMs = 42_000, shuffle = true, repeatMode = 2)

        val snap = store.load()!!
        assertEquals(listOf(1L, 2L), snap.songs.map { it.id })
        assertEquals(1, snap.index)
        assertEquals(42_000L, snap.positionMs)
        assertTrue(snap.shuffle)
        assertEquals(2, snap.repeatMode)
        assertEquals(2, snap.songs[1].trackNumber)
        assertEquals("Artista del álbum", snap.songs[0].albumArtist)
    }

    @Test
    fun laPosicionSolaNoPierdeLaCola() {
        val store = QueueStore(ctx()).apply { clear() }
        store.save(listOf(song(1, "Uno")), 0, 1_000, false, 0)
        store.savePosition(99_000)
        val snap = store.load()!!
        assertEquals(99_000L, snap.positionMs)
        assertEquals(1, snap.songs.size)
    }

    @Test
    fun indiceFueraDeRangoSeCorrige() {
        val store = QueueStore(ctx()).apply { clear() }
        store.save(listOf(song(1, "Uno"), song(2, "Dos")), index = 99, positionMs = 0, shuffle = false, repeatMode = 0)
        assertEquals(1, store.load()!!.index)
    }

    @Test
    fun sinColaGuardadaDevuelveNull() {
        val store = QueueStore(ctx()).apply { clear() }
        assertNull(store.load())
    }

    @Test
    fun noGuardaColasVacias() {
        val store = QueueStore(ctx()).apply { clear() }
        store.save(emptyList(), 0, 0, false, 0)
        assertNull(store.load())
    }

    @Test
    fun jsonCorruptoDevuelveNullSinReventar() {
        val prefs = ctx().getSharedPreferences("melodia_queue", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("songs", "{esto no es json").apply()
        assertNull(QueueStore(ctx()).load())
    }

    @Test
    fun entradasIncompletasSeSaltanYElRestoSobrevive() {
        val prefs = ctx().getSharedPreferences("melodia_queue", android.content.Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        arr.put(org.json.JSONObject().put("id", 1L).put("t", "Uno").put("p", "/a/1.mp3"))
        arr.put(org.json.JSONObject().put("t", "Sin id")) // inválida
        arr.put(org.json.JSONObject().put("id", 2L).put("t", "Dos").put("p", "/a/2.mp3"))
        prefs.edit().putString("songs", arr.toString()).apply()
        val snap = QueueStore(ctx()).load()
        assertNotNull(snap)
        assertEquals(listOf(1L, 2L), snap!!.songs.map { it.id })
    }

    @Test
    fun unSoloTemaInvalidoDevuelveNull() {
        val prefs = ctx().getSharedPreferences("melodia_queue", android.content.Context.MODE_PRIVATE)
        val arr = org.json.JSONArray().put(org.json.JSONObject().put("t", "Sin id"))
        prefs.edit().putString("songs", arr.toString()).apply()
        assertNull(QueueStore(ctx()).load())
    }
}

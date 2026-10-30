package com.melodia.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.data.groupAlbums
import com.melodia.app.data.art.normTitle
import com.melodia.app.data.art.titlesMatch
import com.melodia.app.data.model.Song
import com.melodia.app.data.online.DlUrlExpiredException
import com.melodia.app.data.online.YtErrors
import com.melodia.app.data.online.YtRepository
import com.melodia.app.data.online.YtTrack
import com.melodia.app.player.QueueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Contratos de los arreglos de la auditoría 7.9: cola persistente que no
 * resucita, historial de descargas sin duplicados, nombres únicos por descarga,
 * álbumes homónimos separados y mensaje ante enlace vencido.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AuditFixesTest {

    private fun ctx(): Context = ApplicationProvider.getApplicationContext()

    private fun song(id: Long, artist: String = "A", album: String = "Al") = Song(
        id = id, title = "Tema $id", artist = artist, album = album, albumId = id,
        durationMs = 180_000, dataPath = "/music/$id.mp3", dateAdded = id
    )

    // ── QueueStore ────────────────────────────────────────────────
    @Test
    fun guardarColaVaciaNoResucitaLaVieja() {
        val store = QueueStore(ctx())
        store.saveSongs(listOf(song(1), song(2)))
        store.saveSongs(emptyList())
        // El JSON viejo se pisa con []: load() ya no devuelve la cola anterior.
        assertNull(kotlinx.coroutines.runBlocking { store.load() })
    }

    @Test
    fun repeatModeCorruptoNoMataLaRestauracion() {
        val store = QueueStore(ctx())
        store.saveSongs(listOf(song(7)))
        ctx().getSharedPreferences("melodia_queue", Context.MODE_PRIVATE)
            .edit().putInt("rep", 99).apply()
        val snap = kotlinx.coroutines.runBlocking { store.load() }
        assertEquals(1, snap?.songs?.size)
        assertEquals(0, snap?.repeatMode)
    }

    // ── Historial de descargas ────────────────────────────────────
    @Test
    fun reDescargarNoDuplicaElHistorial() {
        val yt = YtRepository(ctx())
        val t = YtTrack("abc", "Tema", "Artista", 200, null, "https://youtu.be/abc")
        yt.pushDlHistory(t, true)
        yt.pushDlHistory(t, true)
        val hist = yt.getDlHistory()
        assertEquals(1, hist.filter { it.id == "abc" }.size)
    }

    @Test
    fun descargasConTituloIgualNoColisionan() {
        val yt = YtRepository(ctx())
        val a = YtTrack("id-uno-1234567890", "Tema!", "Artista?", 200, null, "u1")
        val b = YtTrack("id-dos-0987654321", "Tema?", "Artista!", 200, null, "u2")
        // sanitize() las dejaba idénticas ("Tema - Artista"): el id las separa.
        assertTrue(yt.fileNamePara(a) != yt.fileNamePara(b))
    }

    // ── Álbumes homónimos ─────────────────────────────────────────
    @Test
    fun homonimosDeDistintoArtistaNoSeFusionan() {
        val songs = listOf(
            song(1, artist = "Muse", album = "Grandes éxitos"),
            song(2, artist = "Queen", album = "Grandes éxitos")
        )
        val groups = groupAlbums(songs)
        assertEquals(2, groups.size)
        assertEquals(2, groups.sumOf { it.count })
    }

    @Test
    fun mismoAlbumMismoArtistaSigueJunto() {
        val songs = listOf(song(1, artist = "Muse"), song(2, artist = "Muse"))
        assertEquals(1, groupAlbums(songs).size)
    }

    // ── Normalización de títulos (iTunes) ──────────────────────────
    @Test
    fun coletillasNoRompeneElMatch() {
        assertEquals("toco madera", normTitle("Toco madera (Official Video)"))
        assertEquals("tema", normTitle("[Lyric] Tema - HD"))
    }

    @Test
    fun elMismoTemaMatcheaAunqueVengaSucio() {
        assertTrue(titlesMatch("Toco madera", "Toco Madera"))
        assertTrue(titlesMatch("Amor Eterno", "amor eterno (official audio)"))
    }

    @Test
    fun temasDistintosNoMatchean() {
        assertTrue(!titlesMatch("Amor", "Amor - Remix"))
        assertTrue(!titlesMatch("Sol", "Luna"))
        assertTrue(!titlesMatch("", "Algo"))
    }

    @Test
    fun claveDeFavsIgnoraEspaciosYCase() {
        val repo = com.melodia.app.data.PlaylistRepository(
            androidx.room.Room.inMemoryDatabaseBuilder(
                ctx(), com.melodia.app.data.local.MelodiaDatabase::class.java
            ).allowMainThreadQueries().build()
        )
        assertEquals(repo.favKey("  Bad   Bunny", "DtMF"), repo.favKey("bad bunny", "dtmf"))
    }

    @Test
    fun artistaVacioEsYouTube() {
        val yt = YtRepository(ctx())
        assertEquals("YouTube", yt.artistFor(YtTrack("x", "T", "", 1, null, "u")))
        assertEquals("Muse", yt.artistFor(YtTrack("x", "T", "Muse", 1, null, "u")))
    }

    @Test
    fun estimadoDeColeccionEsProporcionalALaDuracion() {
        val tracks = (1..100).map {
            YtTrack("id$it", "T$it", "A", 240, null, "u$it")
        }
        // 100 temas de 4 min ≈ 800 MB a 2 MB/min.
        assertEquals(800, com.melodia.app.ui.screens.estimadoColMb(tracks))
        assertEquals(1, com.melodia.app.ui.screens.estimadoColMb(emptyList()))
    }

    // ── Errores ───────────────────────────────────────────────────
    @Test
    fun enlaceVencidoTieneMensajePropio() {
        val msg = YtErrors.message(DlUrlExpiredException("CDN devolvió 403"), "genérico")
        assertTrue(msg.contains("venció") || msg.contains("Venció"))
    }
}

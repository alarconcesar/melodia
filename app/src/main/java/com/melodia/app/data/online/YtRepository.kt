package com.melodia.app.data.online

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem.InfoType
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class YtTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbUrl: String?,
    val url: String
)

data class YtAudio(val url: String, val ext: String, val mime: String)

/** Búsqueda, audio y descarga de YouTube vía NewPipeExtractor (sin API key). */
class YtRepository(private val context: Context) {

    @Volatile private var ready = false
    private fun ensure() {
        if (ready) return
        synchronized(this) {
            if (!ready) {
                NewPipe.init(YtDownloader())
                ready = true
            }
        }
    }

    private fun yt() = NewPipe.getService(0)

    // ── Paginación: se guarda el extractor y la página siguiente ──
    private var pager: SearchExtractor? = null
    private var next: Page? = null
    private data class CachedAudio(val audio: YtAudio, val expiresAt: Long)
    private val audioCache = mutableMapOf<String, CachedAudio>()

    /**
     * Búsqueda mixta: YouTube devuelve videos y playlists/álbumes en la misma
     * página. Se conserva el orden y se deduplica por tipo.
     */
    suspend fun search(query: String): List<YtSearchItem> = withContext(Dispatchers.IO) {
        ensure()
        // Índice general primero (mejores resultados para videos). Si YouTube
        // no devuelve nada —filtra búsquedas que considera "inapropiadas" con
        // NothingFoundException— se busca en el catálogo de música, que sí las
        // tiene. Sin esto, "sexy magic" terminaba en un error técnico.
        val general = try {
            fetchPage(query, null)
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            if (t !is SearchExtractor.NothingFoundException) throw t
            emptyList()
        }
        if (general.isNotEmpty()) general
        else fetchPage(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS))
    }

    /** Ejecuta la búsqueda y deja listo el paginador. filters=null → índice general. */
    private fun fetchPage(query: String, filters: List<String>?): List<YtSearchItem> {
        val (ex, items) = extract(query, filters)
        pager = ex
        next = try { ex.initialPage.nextPage } catch (_: Exception) { null }
        return items
    }

    /**
     * Búsqueda de una sola página, sin tocar el paginador: la usa la barra de
     * Inicio para no romper el "Ver más" de la pestaña Online.
     */
    suspend fun searchFirstPage(query: String): List<YtSearchItem> = withContext(Dispatchers.IO) {
        ensure()
        val general = try {
            extract(query, null).second
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            if (t !is SearchExtractor.NothingFoundException) throw t
            emptyList()
        }
        if (general.isNotEmpty()) general
        else extract(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS)).second
    }

    private fun extract(
        query: String,
        filters: List<String>?
    ): Pair<SearchExtractor, List<YtSearchItem>> {
        val service = yt()
        val ex = if (filters == null) service.getSearchExtractor(query)
        else service.getSearchExtractor(service.searchQHFactory.fromQuery(query, filters, ""))
        ex.fetchPage()
        return ex to mapItems(ex.initialPage.items)
    }

    /** Devuelve true si hay más páginas por cargar. */
    fun hasMore(): Boolean = pager != null && next != null

    suspend fun more(): List<YtSearchItem> = withContext(Dispatchers.IO) {
        val ex = pager ?: return@withContext emptyList()
        val np = next ?: return@withContext emptyList()
        ensure()
        val page = ex.getPage(np)
        next = try { page.nextPage } catch (_: Exception) { null }
        mapItems(page.items)
    }

    private fun mapItems(items: List<org.schabi.newpipe.extractor.InfoItem>): List<YtSearchItem> {
        return items.mapNotNull { item ->
            try {
                when (item.infoType) {
                    InfoType.STREAM -> {
                        val s = item as StreamInfoItem
                        if (s.streamType == StreamType.LIVE_STREAM ||
                            s.streamType == StreamType.AUDIO_LIVE_STREAM
                        ) return@mapNotNull null
                        val url = s.url ?: return@mapNotNull null
                        val id = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.getOrNull(1)
                            ?: url.substringAfterLast("/").substringBefore("?").ifBlank { return@mapNotNull null }
                        val thumbs = try { s.thumbnails } catch (_: Exception) { emptyList() }
                        YtSearchItem.Track(
                            YtTrack(
                                id = id,
                                title = (try { s.name } catch (_: Exception) { "Sin título" }) ?: "Sin título",
                                artist = ((try { s.uploaderName } catch (_: Exception) { "" }) ?: "")
                                    .replace(" - Topic", "").replace("VEVO", "").trim(),
                                durationSec = try { s.duration } catch (_: Exception) { -1L },
                                thumbUrl = try { thumbs.maxByOrNull { it.height * it.width }?.url } catch (_: Exception) { null },
                                url = url
                            )
                        )
                    }

                    InfoType.PLAYLIST -> {
                        val pl = item as org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
                        val url = pl.url ?: return@mapNotNull null
                        // list=RD… es un "mix" automático (radio infinita): no es
                        // una playlist descargable de verdad.
                        if (url.contains("list=RD")) return@mapNotNull null
                        val id = Regex("[?&]list=([^&]+)").find(url)?.groupValues?.getOrNull(1)
                            ?: url.substringAfterLast("/").ifBlank { return@mapNotNull null }
                        val thumbs = try { pl.thumbnails } catch (_: Exception) { emptyList() }
                        YtSearchItem.Collection(
                            id = id,
                            title = (try { pl.name } catch (_: Exception) { "Playlist" }) ?: "Playlist",
                            uploader = ((try { pl.uploaderName } catch (_: Exception) { "" }) ?: "")
                                .replace(" - Topic", "").trim(),
                            count = try { pl.streamCount } catch (_: Exception) { 0L }.toInt(),
                            thumbUrl = try { thumbs.maxByOrNull { it.height * it.width }?.url } catch (_: Exception) { null },
                            url = url
                        )
                    }

                    else -> null
                }
            } catch (_: Exception) { null }
        }
    }

    /** high=true: mejor bitrate (alta). false: bitrate modesto (estándar / ahorro de datos). */
    suspend fun bestAudio(videoUrl: String, high: Boolean = true, force: Boolean = false): YtAudio =
        withContext(Dispatchers.IO) {
            ensure()
            val cacheKey = "$videoUrl|${if (high) "h" else "s"}"
            val now = System.currentTimeMillis()
            if (!force) {
                synchronized(audioCache) {
                    val hit = audioCache[cacheKey]
                    // Expiración real de la URL (expire=...), no un TTL a ciegas
                    if (hit != null && now < hit.expiresAt) return@withContext hit.audio
                    if (hit != null) audioCache.remove(cacheKey)
                }
            }
            val ex = yt().getStreamExtractor(videoUrl)
            ex.fetchPage()
            val audios: List<AudioStream> = ex.audioStreams
            require(audios.isNotEmpty()) { "Sin audio disponible" }
            val ranked = audios.sortedByDescending { try { it.averageBitrate } catch (_: Exception) { 0 } }
            val pool = if (high) ranked else ranked.filter {
                try { it.averageBitrate <= 96000 } catch (_: Exception) { true }
            }.ifEmpty { ranked.takeLast(2) }
            val pick = pool.firstOrNull { (try { it.codec } catch (_: Exception) { "" } ?: "").contains("mp4a", true) }
                ?: pool.firstOrNull { (try { it.codec } catch (_: Exception) { "" } ?: "").contains("opus", true) }
                ?: pool.first()
            val isMp4 = (try { pick.codec } catch (_: Exception) { "" } ?: "").contains("mp4a", true)
            YtAudio(pick.content, if (isMp4) "m4a" else "webm", if (isMp4) "audio/mp4" else "audio/webm").also {
                synchronized(audioCache) {
                    audioCache[cacheKey] = CachedAudio(
                        audio = it,
                        expiresAt = StreamUrlExpiry.expiryEpochMs(it.url, System.currentTimeMillis())
                    )
                    while (audioCache.size > 30) audioCache.remove(audioCache.keys.first())
                }
            }
        }

    /** Resuelve el audio en segundo plano para que el play sea instantáneo. */
    suspend fun prefetchAudio(videoUrl: String, high: Boolean = true) {
        try { bestAudio(videoUrl, high) } catch (_: Exception) {}
    }

    private fun sanitize(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().take(80).ifBlank { "audio" }

    // ── Historial online (persistido, máx. 20) ──
    fun getHistory(): List<YtTrack> {
        return try {
            val arr = org.json.JSONArray(prefs().getString("history", "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val o = arr.getJSONObject(i)
                    YtTrack(
                        o.getString("id"), o.getString("title"), o.optString("artist", ""),
                        o.optLong("dur", -1), o.optString("thumb", "").ifBlank { null }, o.getString("url")
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun pushHistory(t: YtTrack) {
        try {
            val list = (listOf(t) + getHistory()).distinctBy { it.id }.take(20)
            val arr = org.json.JSONArray()
            list.forEach {
                arr.put(
                    org.json.JSONObject()
                        .put("id", it.id).put("title", it.title).put("artist", it.artist)
                        .put("dur", it.durationSec).put("thumb", it.thumbUrl ?: "").put("url", it.url)
                )
            }
            prefs().edit().putString("history", arr.toString()).apply()
        } catch (_: Exception) {}
    }
    /** Historial de descargas (persistido, máx. 50): título, artista, calidad y fecha. */
    data class DlRecord(val title: String, val artist: String, val high: Boolean, val at: Long)

    fun getDlHistory(): List<DlRecord> {
        return try {
            val arr = org.json.JSONArray(prefs().getString("dlhist", "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val o = arr.getJSONObject(i)
                    DlRecord(o.getString("t"), o.optString("a", ""), o.optBoolean("h", true), o.optLong("at", 0))
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun pushDlHistory(track: YtTrack, high: Boolean) {
        try {
            val list = (listOf(DlRecord(track.title, track.artist, high, System.currentTimeMillis())) + getDlHistory()).take(50)
            val arr = org.json.JSONArray()
            list.forEach { arr.put(org.json.JSONObject().put("t", it.title).put("a", it.artist).put("h", it.high).put("at", it.at)) }
            prefs().edit().putString("dlhist", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearDlHistory() { try { prefs().edit().remove("dlhist").apply() } catch (_: Exception) {} }

    // ── Búsquedas recientes (persistidas, más recientes primero) ──
    private fun prefs() = context.getSharedPreferences("melodia_yt", Context.MODE_PRIVATE)
    fun getRecent(): List<String> = try {
        prefs().getString("recent", "")!!.split("\n").filter { it.isNotBlank() }.take(6)
    } catch (_: Exception) { emptyList() }

    fun pushRecent(q: String) {
        try {
            val clean = q.trim()
            if (clean.isBlank()) return
            val list = (listOf(clean) + getRecent()).distinct().take(6)
            prefs().edit().putString("recent", list.joinToString("\n")).apply()
        } catch (_: Exception) {}
    }

    private companion object {
        const val UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }

    /** Descarga el audio a Música/Melodia y devuelve su Uri de MediaStore. */
    suspend fun download(track: YtTrack, onProgress: (Float) -> Unit, high: Boolean = true): Uri =
        withContext(Dispatchers.IO) {
            val audio = bestAudio(track.url, high)
            val fileName = sanitize("${track.title} - ${track.artist.ifBlank { "YouTube" }}") + "." + audio.ext
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT < 29) {
                try {
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Melodia").mkdirs()
                } catch (_: Exception) {}
            }
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.TITLE, track.title)
                put(MediaStore.Audio.Media.ARTIST, track.artist.ifBlank { "YouTube" })
                // Álbum propio por descarga: si queda vacío, MediaStore agrupa
                // todos los temas en "<unknown>" y comparten la misma portada.
                put(MediaStore.Audio.Media.ALBUM, track.title)
                if (Build.VERSION.SDK_INT >= 30) {
                    put(MediaStore.Audio.Media.ALBUM_ARTIST, track.artist.ifBlank { "YouTube" })
                }
                put(MediaStore.Audio.Media.MIME_TYPE, audio.mime)
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Melodia")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear el archivo")
            val tmp = File(context.cacheDir, "melodia-${System.nanoTime()}.part")
            try {
                fetchToFile(audio.url, tmp, onProgress)
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(tmp).use { inp -> inp.copyTo(out, 64 * 1024) }
                } ?: error("Sin salida")
                if (Build.VERSION.SDK_INT >= 29) {
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                onProgress(1f)
                uri
            } catch (e: Exception) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                throw e
            } finally {
                try { tmp.delete() } catch (_: Exception) {}
            }
        }

    private fun openConn(url: String, range: String? = null): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        if (range != null) c.setRequestProperty("Range", range)
        c.connectTimeout = 20_000
        c.readTimeout = 30_000
        return c
    }

    /** Tamaño total del archivo, leído del Content-Range de una petición de 1 byte. */
    @androidx.annotation.VisibleForTesting
    internal fun contentLength(url: String): Long = try {
        val c = openConn(url, "bytes=0-0")
        c.connect()
        val total = c.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: -1L
        c.disconnect()
        total
    } catch (_: Exception) { -1L }

    /**
     * Baja el archivo a disco en trozos paralelos con `Range`.
     *
     * YouTube limita cada conexión: medido desde el VPS, una sola conexión daba
     * 32 KB/s para un m4a de 3,3 MB, mientras la misma descarga en 4 trozos daba
     * ~5,8 MB/s. El cuello de botella no era la red del equipo.
     */
    @androidx.annotation.VisibleForTesting
    internal suspend fun fetchToFile(url: String, dest: File, onProgress: (Float) -> Unit) {
        val total = contentLength(url)
        if (total <= 0L) {
            streamToFile(url, dest, -1L, onProgress)
            return
        }
        try {
            val parts = if (total > 1_500_000L) 4 else 2
            val chunk = total / parts
            RandomAccessFile(dest, "rw").use { raf ->
                raf.setLength(total)
                val ch = raf.channel
                val done = AtomicLong(0)
                val lastEmit = AtomicLong(0)
                val onBytes: (Long) -> Unit = { n ->
                    val d = done.addAndGet(n)
                    val now = System.nanoTime()
                    if (now - lastEmit.get() > 120_000_000L) {
                        lastEmit.set(now)
                        onProgress((d.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
                coroutineScope {
                    (0 until parts).map { i ->
                        val from = i * chunk
                        val to = if (i == parts - 1) total - 1 else from + chunk - 1
                        async { downloadRange(url, ch, from, to, onBytes) }
                    }.awaitAll()
                }
                onProgress(1f)
            }
        } catch (e: Exception) {
            // El servidor no respetó Range o falló un trozo: una sola conexión.
            try { dest.delete() } catch (_: Exception) {}
            streamToFile(url, dest, total, onProgress)
        }
    }

    private suspend fun downloadRange(
        url: String,
        ch: FileChannel,
        from: Long,
        to: Long,
        onBytes: (Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            try {
                val c = openConn(url, "bytes=$from-$to")
                if (c.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    c.disconnect()
                    error("El servidor no aceptó Range")
                }
                c.inputStream.use { inp ->
                    val buf = ByteArray(64 * 1024)
                    var pos = from
                    while (pos <= to) {
                        val want = minOf(buf.size.toLong(), to - pos + 1).toInt()
                        val n = inp.read(buf, 0, want)
                        if (n <= 0) break
                        ch.write(ByteBuffer.wrap(buf, 0, n), pos)
                        pos += n
                        onBytes(n.toLong())
                    }
                }
                c.disconnect()
                return@withContext
            } catch (e: Exception) {
                attempt++
                if (attempt >= 3) throw e
                delay(400L * attempt)
            }
        }
    }

    private suspend fun streamToFile(url: String, dest: File, total: Long, onProgress: (Float) -> Unit) =
        withContext(Dispatchers.IO) {
            val c = openConn(url)
            c.connect()
            val len = if (total > 0) total else c.contentLengthLong
            var read = 0L
            var lastEmit = 0L
            dest.outputStream().use { out ->
                c.inputStream.use { inp ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        val now = System.nanoTime()
                        if (len > 0 && now - lastEmit > 120_000_000L) {
                            lastEmit = now
                            onProgress((read.toFloat() / len).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            c.disconnect()
            if (len > 0) onProgress(1f)
        }

    /** Trae los temas de una playlist de YouTube (título + hasta 100 temas, varias páginas). */
    suspend fun playlistTracks(url: String): Pair<String, List<YtTrack>> = withContext(Dispatchers.IO) {
        ensure()
        val ex = yt().getPlaylistExtractor(url)
        ex.fetchPage()
        val title = (try { ex.name } catch (_: Exception) { "Playlist" }) ?: "Playlist"
        val out = mutableListOf<YtTrack>()
        var page = ex.initialPage
        var rounds = 0
        while (true) {
            out += mapItems(page.items).filterIsInstance<YtSearchItem.Track>().map { it.track }
            if (out.size >= 100 || !page.hasNextPage() || rounds++ >= 4) break
            page = ex.getPage(page.nextPage)
        }
        title to out.take(100)
    }

    /** Descarga bytes crudos (miniatura para incrustar como carátula). */
    suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.connect()
            if (conn.responseCode !in 200..299) return@withContext null
            conn.inputStream.use { it.readBytes() }.takeIf { it.size in 1..4_000_000 }
        } catch (_: Exception) { null }
    }

    /**
     * Guarda un archivo ya descargado en Música/Melodia y, si es M4A,
     * le incrusta título, artista, letra y carátula en el propio archivo.
     * Si el etiquetado falla, la descarga se conserva igual.
     */
    suspend fun storeDownload(
        track: YtTrack, high: Boolean, audio: YtAudio, part: File,
        lyrics: String? = null, artBytes: ByteArray? = null, artMime: String? = null
    ): Uri = withContext(Dispatchers.IO) {
        val fileName = sanitize("${track.title} - ${track.artist.ifBlank { "YouTube" }}") + "." + audio.ext
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT < 29) {
            try {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Melodia").mkdirs()
            } catch (_: Exception) {}
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, track.artist.ifBlank { "YouTube" })
            put(MediaStore.Audio.Media.ALBUM, track.title)
            if (Build.VERSION.SDK_INT >= 30) {
                put(MediaStore.Audio.Media.ALBUM_ARTIST, track.artist.ifBlank { "YouTube" })
            }
            put(MediaStore.Audio.Media.MIME_TYPE, audio.mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Melodia")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                part.inputStream().use { inp -> inp.copyTo(out) }
            } ?: error("Sin salida")
            // Etiquetado M4A dentro del archivo (título, artista, letra, carátula)
            if (audio.ext == "m4a") {
                try {
                    val raw = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?.takeIf { it.size in 16..60_000_000 }
                        ?: error("sin lectura")
                    val tagged = Mp4Tagger.tag(
                        raw, track.title, track.artist.ifBlank { "YouTube" },
                        track.title, lyrics, artBytes, artMime
                    )
                    resolver.openOutputStream(uri)?.use { it.write(tagged) }
                        ?: error("sin escritura")
                } catch (_: Exception) { /* se conserva sin etiquetar */ }
            }
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            throw e
        }
    }
}

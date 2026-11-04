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
import kotlinx.coroutines.ensureActive
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

/**
 * El CDN rechazó con 403: la URL firmada murió antes de lo previsto.
 * No es un corte de red: reintentar sobre la misma URL es inútil, hay que
 * re-resolver con `bestAudio(force = true)`.
 */
class DlUrlExpiredException(m: String) : java.io.IOException(m)

/** Disco lleno: no es un fallo de red y reintentar no sirve. */
class DlNoSpaceException(m: String) : java.io.IOException(m)

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
    private val pagerLock = Any()
    /** Generación de búsqueda: una búsqueda lenta no debe pisar a una rápida. */
    private var searchGen = 0
    private data class CachedAudio(val audio: YtAudio, val expiresAt: Long)
    // LRU: al acertar se reencola (FIFO expulsaba URLs frescas en uso).
    private val audioCache = object : LinkedHashMap<String, CachedAudio>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAudio>?): Boolean = size > 30
    }

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
        val gen = synchronized(pagerLock) { ++searchGen }
        val (ex, items) = extract(query, filters)
        // Si otra búsqueda empezó mientras extraíamos, sus resultados mandan.
        synchronized(pagerLock) { if (gen == searchGen) {
            pager = ex
            next = try { ex.initialPage.nextPage } catch (_: Exception) { null }
        } }
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
    fun hasMore(): Boolean = synchronized(pagerLock) { pager != null && next != null }

    suspend fun more(): List<YtSearchItem> = withContext(Dispatchers.IO) {
        val ex = synchronized(pagerLock) { pager } ?: return@withContext emptyList()
        val np = synchronized(pagerLock) { next } ?: return@withContext emptyList()
        ensure()
        val page = ex.getPage(np)
        synchronized(pagerLock) { next = try { page.nextPage } catch (_: Exception) { null } }
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
                            title = (try { pl.name } catch (_: Exception) { "Lista" }) ?: "Lista",
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
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val audios: List<AudioStream> = ex.audioStreams
            require(audios.isNotEmpty()) { "Sin audio disponible" }
            val ranked = audios.sortedByDescending { try { it.averageBitrate } catch (_: Exception) { 0 } }
            // En estándar se prefieren bitrates modestos, pero SIN recortar la
            // lista antes de elegir formato: takeLast(2) se comía el m4a y el
            // archivo quedaba webm sin etiquetas (storeDownload solo taggea m4a).
            val modest = ranked.filter {
                try { it.averageBitrate <= 96000 } catch (_: Exception) { true }
            }
            val pool = if (high) ranked else modest.ifEmpty { ranked.filter { isMp4a(it) }.ifEmpty { ranked } }
            val pick = pool.firstOrNull { isMp4a(it) }
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

    private fun isMp4a(s: AudioStream): Boolean =
        (try { s.codec } catch (_: Exception) { "" } ?: "").contains("mp4a", true)

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
    /** Historial de descargas (persistido, máx. 50): con url para re-escuchar. */
    data class DlRecord(
        val title: String, val artist: String, val high: Boolean, val at: Long,
        val id: String = "", val url: String = ""
    )

    fun getDlHistory(): List<DlRecord> {
        return try {
            val arr = org.json.JSONArray(prefs().getString("dlhist", "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val o = arr.getJSONObject(i)
                    DlRecord(o.getString("t"), o.optString("a", ""), o.optBoolean("h", true), o.optLong("at", 0), o.optString("id", ""), o.optString("u", ""))
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun pushDlHistory(track: YtTrack, high: Boolean) {
        try {
            // Deduplica por tema (antes cada re-descarga duplicaba la fila).
            val list = (listOf(DlRecord(track.title, track.artist, high, System.currentTimeMillis(), track.id, track.url)) + getDlHistory())
                .distinctBy { if (it.id.isNotBlank()) "id:${it.id}" else "t:${it.title}|${it.artist}|${it.high}" }
                .take(50)
            val arr = org.json.JSONArray()
            list.forEach { arr.put(org.json.JSONObject().put("t", it.title).put("a", it.artist).put("h", it.high).put("at", it.at).put("id", it.id).put("u", it.url)) }
            prefs().edit().putString("dlhist", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearDlHistory() { try { prefs().edit().remove("dlhist").apply() } catch (_: Exception) {} }

    // ── Búsquedas recientes (persistidas, más recientes primero) ──
    private fun prefs() = context.getSharedPreferences("melodia_yt", Context.MODE_PRIVATE)

    var altaCalidad: Boolean
        get() = try { prefs().getBoolean("alta_calidad", true) } catch (_: Exception) { true }
        set(v) { try { prefs().edit().putBoolean("alta_calidad", v).apply() } catch (_: Exception) {} }
    var ahorroDatos: Boolean
        get() = try { prefs().getBoolean("ahorro_datos", false) } catch (_: Exception) { false }
        set(v) { try { prefs().edit().putBoolean("ahorro_datos", v).apply() } catch (_: Exception) {} }
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

    companion object {
        /** Cuántos temas trae una colección como máximo (YouTube pagina de a ~20-50). */
        const val MAX_COLLECTION_TRACKS = 100
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }

    /** Descarga el audio a Música/Melodia y devuelve su Uri de MediaStore. */
    suspend fun download(track: YtTrack, onProgress: (Float) -> Unit, high: Boolean = true): Uri =
        withContext(Dispatchers.IO) {
            val audio = bestAudio(track.url, high)
            val fileName = fileNamePara(track) + "." + audio.ext
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
        c.readTimeout = 45_000
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
        downloadToFile(url, dest, 0L) { done, total ->
            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
        }
    }

    /** Cantidad de conexiones según el tamaño: más trozos = más ancho de banda. */
    private fun partesPara(restante: Long): Int {
        val ideal = when {
            restante >= 8L * 1024 * 1024 -> 6
            restante >= 4L * 1024 * 1024 -> 4
            restante >= 1_500_000L -> 3
            else -> 2
        }
        // Cada trozo de al menos 256 KB: más conexiones que datos solo agregan overhead.
        val maxPorTamano = (restante / 262_144L).toInt().coerceAtLeast(1)
        return ideal.coerceAtMost(maxPorTamano)
    }

    /**
     * Baja [url] a [dest] en trozos paralelos, reanudando desde [startAt] bytes
     * que ya estén en el archivo. Devuelve el total descargado.
     *
     * Si algo falla, el archivo queda truncado en [startAt] para que el próximo
     * intento retome desde un prefijo íntegro (nunca desde un hueco).
     */
    suspend fun downloadToFile(
        url: String,
        dest: File,
        startAt: Long = 0L,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val total = contentLength(url)
        if (total <= 0L) {
            val final = streamToFileResume(url, dest, startAt, onProgress)
            return@withContext final
        }
        var desde = startAt.coerceIn(0L, total)
        if (desde >= total && startAt > 0) {
            // El .part es más largo que el total fresco: otra variante.
            // Truncar y reintentar desde cero en vez de DONE corrupto.
            try { dest.delete() } catch (_: Exception) {}
            desde = 0L
        }
        if (desde >= total) {
            onProgress(total, total)
            return@withContext total
        }
        // ENOSPC al pre-alocar: el use cierra solo; se traduce el error para
        // no mostrar "sin conexión" con WiFi perfecto.
        RandomAccessFile(dest, "rw").use { raf ->
            try {
                raf.setLength(total)
            } catch (e: java.io.IOException) {
                if ((e.message ?: "").contains("space", true)) throw DlNoSpaceException("Sin espacio en el dispositivo")
                else throw e
            }
            val ch = raf.channel
            val partes = partesPara(total - desde)
            val chunk = (total - desde) / partes
            val done = AtomicLong(desde)
            val lastEmit = AtomicLong(0L)
            val onBytes: (Long) -> Unit = { n ->
                val d = done.addAndGet(n)
                val now = System.nanoTime()
                if (now - lastEmit.get() > 150_000_000L) {
                    lastEmit.set(now)
                    onProgress(d, total)
                }
            }
            try {
                coroutineScope {
                    (0 until partes).map { i ->
                        val from = desde + i * chunk
                        val to = if (i == partes - 1) total - 1 else from + chunk - 1
                        async { downloadRange(url, ch, from, to, onBytes) }
                    }.awaitAll()
                }
            } catch (e: Exception) {
                try { raf.setLength(desde) } catch (_: Exception) {}
                throw e
            }
        }
        onProgress(total, total)
        total
    }

    /**
     * Servidor sin tamaño conocido: intenta reanudar con Range y, si el
     * servidor no lo acepta, reescribe desde cero (antes siempre se tiraba
     * lo bajado y la barra retrocedía al pausar/retomar).
     */
    private suspend fun streamToFileResume(
        url: String,
        dest: File,
        startAt: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val c = if (startAt > 0) openConn(url, "bytes=$startAt-") else openConn(url)
        c.connect()
        // Un 403 aquí es HTML de error, no audio: antes se guardaba como tema válido.
        val code = try { c.responseCode } catch (_: Exception) { -1 }
        if (code == 403 || code == 404 || code == 410) {
            try { c.disconnect() } catch (_: Exception) {}
            throw DlUrlExpiredException("CDN devolvió $code")
        }
        // 206 = el servidor aceptó reanudar: se agrega al final.
        val reanuda = code == HttpURLConnection.HTTP_PARTIAL && startAt > 0 && dest.exists()
        val len = c.contentLengthLong.takeIf { it > 0 } ?: -1L
        var read = if (reanuda) dest.length() else 0L
        var lastEmit = 0L
        java.io.FileOutputStream(dest, reanuda).use { out ->
            c.inputStream.use { inp ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val n = inp.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    read += n
                    val now = System.nanoTime()
                    if (now - lastEmit > 150_000_000L) {
                        lastEmit = now
                        onProgress(read, len)
                    }
                }
            }
        }
        c.disconnect()
        onProgress(read, if (len > 0) len else read)
        read
    }

    /**
     * Baja un trozo. Si la conexión se corta (típico "software caused connection
     * abort" en LTE), reintenta y, si el rango es grande, lo parte en mitades
     * más chicas: pedazos pequeños sobreviven mejor a los cortes del CDN.
     */
    private suspend fun downloadRange(
        url: String,
        ch: FileChannel,
        from: Long,
        to: Long,
        onBytes: (Long) -> Unit,
        depth: Int = 0
    ): Unit = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            try {
                val c = openConn(url, "bytes=$from-$to")
                val rc = try { c.responseCode } catch (ce: kotlinx.coroutines.CancellationException) { throw ce } catch (_: Exception) { -1 }
                // 403 = URL firmada muerta: reintentar el mismo rango es quemar
                // requests. Se propaga para re-resolver el enlace una sola vez.
                if (rc == 403 || rc == 404 || rc == 410) {
                    c.disconnect()
                    throw DlUrlExpiredException("rango rechazado con $rc")
                }
                if (rc == 429) {
                    c.disconnect()
                    throw org.schabi.newpipe.extractor.exceptions.ReCaptchaException("rate-limited", url)
                }
                if (rc != HttpURLConnection.HTTP_PARTIAL) {
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
                    // Corte limpio del CDN a mitad de trozo: sin esto el hueco
                    // quedaba en ceros y se marcaba DONE. Se reintenta.
                    if (pos <= to) error("trozo incompleto ($pos/$to)")
                }
                c.disconnect()
                // 429 = rate-limit: mensaje honesto en vez de "no aceptó Range".
                // (Se detecta aquí porque el código ya se leyó arriba.)
                return@withContext
            } catch (e: Exception) {
                // Pausar/cancelar en pleno trozo tardaba hasta 5 reintentos en
                // sentirse: la cancelación siempre pasa de largo.
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Disco lleno: reintentar no sirve y el mensaje genérico de red
                // confunde ("sin conexión" con WiFi perfecto).
                if (e is java.io.IOException && (e.message ?: "").contains("space", true)) {
                    throw DlNoSpaceException("Sin espacio en el dispositivo")
                }
                attempt++
                if (attempt >= 3 && depth < 3 && (to - from) > 512_000L) {
                    // Reintentar el resto en dos mitades más pequeñas
                    val mid = (from + to) / 2
                    downloadRange(url, ch, from, mid, onBytes, depth + 1)
                    downloadRange(url, ch, mid + 1, to, onBytes, depth + 1)
                    return@withContext
                }
                if (attempt >= 5) throw e
                delay(500L * attempt + kotlin.random.Random.nextLong(0L, 250L))
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

    /**
     * Artista de una descarga: el canal del video o, si vino vacío, "YouTube".
     *
     * A propósito NO se deduce del título: "Amor - Remix" daría artista "Amor"
     * y eso ensucia la biblioteca. Mejor un dato faltante y honesto.
     */
    private fun artistaDe(track: YtTrack): String =
        track.artist.trim().ifBlank { "YouTube" }

    /** Versión pública (sin red) para historial y UI. */
    fun artistFor(track: YtTrack): String = artistaDe(track)

    /**
     * Artista con resolución real: si el item no trae canal, se consulta
     * iTunes Search por título (dato canónico, no deducción del título).
     * Solo se acepta con título coincidente; si no, "YouTube".
     */
    suspend fun resolveArtist(track: YtTrack, artRepo: com.melodia.app.data.art.ArtworkRepository): String =
        withContext(Dispatchers.IO) {
            val directo = track.artist.trim()
            if (directo.isNotBlank()) return@withContext directo
            try {
                val r = kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                    artRepo.fetchArtist(track.title, "")
                }
                r?.trim()?.takeIf { it.isNotBlank() } ?: "YouTube"
            } catch (_: Exception) { "YouTube" }
        }

    /** Guarda la carátula en disco y devuelve su file:// (independiente de MediaStore). */
    private fun guardarPortada(bytes: ByteArray?, clave: String, trim: Boolean = true): String? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val lado = 640
            val escala = minOf(1f, lado.toFloat() / maxOf(bmp.width, bmp.height).coerceAtLeast(1))
            val out = if (escala < 1f) android.graphics.Bitmap.createScaledBitmap(
                bmp, (bmp.width * escala).toInt().coerceAtLeast(1),
                (bmp.height * escala).toInt().coerceAtLeast(1), true
            ) else bmp
            val dir = File(context.filesDir, "dlart").apply { mkdirs() }
            if (trim) trimDir(dir, 200)
            val f = File(dir, "${clave.hashCode()}.jpg")
            f.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            if (out !== bmp) bmp.recycle()
            "file://${f.absolutePath}"
        } catch (_: Exception) { null }
    }

    /** Deja la copia propia de metadatos que manda al leer la biblioteca. */
    private fun guardarMeta(
        fileName: String, track: YtTrack, artista: String, titulo: String,
        artBytes: ByteArray? = null, artPath: String? = null
    ) {
        try {
            val meta = com.melodia.app.data.DownloadMetaStore(context)
            meta.put(
                fileName,
                com.melodia.app.data.DownloadMetaStore.Meta(
                    title = titulo,
                    artist = artista,
                    album = titulo,
                    artPath = artPath ?: guardarPortada(artBytes, fileName)
                )
            )
        } catch (_: Exception) {}
    }

    /** Poda un directorio de caché dejando los [keep] archivos más recientes. */
    private fun trimDir(dir: File, keep: Int) {
        try {
            val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
            files.drop(keep).forEach { try { it.delete() } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    /**
     * Limpia descargas marcadas IS_PENDING que quedaron colgadas (la app murió
     * entre insert y update): son invisibles para siempre si nadie las borra.
     */
    suspend fun cleanupStalePending(): Int = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 29) return@withContext 0
        var n = 0
        try {
            val resolver = context.contentResolver
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.IS_PENDING}=1 AND (${MediaStore.Audio.Media.RELATIVE_PATH} = ? OR ${MediaStore.Audio.Media.DATA} LIKE ?)",
                arrayOf("Music/Melodia/", "%/Melodia/%"),
                null
            )?.use { c ->
                val ids = mutableListOf<Long>()
                val ci = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (c.moveToNext()) ids += c.getLong(ci)
                ids.forEach { id ->
                    try {
                        if (resolver.delete(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            "${MediaStore.Audio.Media._ID}=?",
                            arrayOf(id.toString())
                        ) > 0) n++
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        n
    }

    /** Artista/título incrustados en el propio archivo (sin adivinar nada). */
    private fun embeddedArtist(uri: Uri): String? {
        var r: android.media.MediaMetadataRetriever? = null
        return try {
            r = android.media.MediaMetadataRetriever()
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                    r.setDataSource(fd.fileDescriptor)
                }
            } catch (_: Exception) { return null }
            r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotBlank() && it != "<unknown>" }
        } catch (_: Exception) { null } finally {
            try { r?.release() } catch (_: Exception) {}
        }
    }

    /** Cursor persistido del repair: cada pasada arranca donde terminó la anterior. */
    private var repairCursor: Int
        get() = try { prefs().getInt("repair_idx", 0) } catch (_: Exception) { 0 }
        set(v) { try { prefs().edit().putInt("repair_idx", v).apply() } catch (_: Exception) {} }

    private fun esArtistaBasura(a: String): Boolean {
        val t = a.trim()
        return t.isBlank() || t == "<unknown>" || t.equals("Desconocido", true) || t.equals("YouTube", true)
    }

    /**
     * Repara fichas de descargas con artista vacío o "YouTube": primero lo que
     * dice la fila de MediaStore, luego lo incrustado en el archivo, y solo al
     * final iTunes (máx. 25 consultas de red por pasada). Idempotente: lo ya
     * arreglado se salta, así converge en varios arranques sin castigar.
     *
     * Devuelve cuántas fichas escribió.
     */
    suspend fun repairDownloadMeta(
        artRepo: com.melodia.app.data.art.ArtworkRepository,
        meta: com.melodia.app.data.DownloadMetaStore,
        maxRed: Int = 25
    ): Int = withContext(Dispatchers.IO) {
        var escritas = 0
        var redUsada = 0
        // Se juntan las filas y se rota: las primeras irresolubles no bloquean
        // para siempre a las de atrás.
        data class FilaRep(val id: Long, val name: String, val title: String, val artist: String, val album: String)
        val filas = mutableListOf<FilaRep>()
        try {
            val resolver = context.contentResolver
            val sel: String
            val args: Array<String>?
            // Algunos OEM dejan RELATIVE_PATH nulo en la SD: se cubre con DATA
            // también (en <29 solo existe DATA).
            if (Build.VERSION.SDK_INT >= 29) {
                sel = "(${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Audio.Media.DATA} LIKE ?)"
                args = arrayOf("%Music/Melodia%", "%/Melodia/%")
            } else {
                sel = "${MediaStore.Audio.Media.DATA} LIKE ?"
                args = arrayOf("%/Melodia/%")
            }
            val cols = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM
            )
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, sel, args,
                "${MediaStore.Audio.Media._ID} ASC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                while (c.moveToNext()) {
                    try {
                        val name = try { c.getString(iName) ?: "" } catch (_: Exception) { "" }
                        if (name.isBlank()) continue
                        filas += FilaRep(
                            try { c.getLong(iId) } catch (_: Exception) { -1L },
                            name,
                            (try { c.getString(iTitle) ?: "" } catch (_: Exception) { "" }).ifBlank {
                                name.substringBeforeLast('.')
                            },
                            try { c.getString(iArtist) ?: "" } catch (_: Exception) { "" },
                            try { c.getString(iAlbum) ?: "" } catch (_: Exception) { "" }
                        )
                    } catch (_: Exception) {}
                }
            }
            if (filas.isEmpty()) return@withContext 0
            val ordered = if (repairCursor in filas.indices) filas.drop(repairCursor) + filas.take(repairCursor) else filas
            var vistas = 0
            for (f in ordered) {
                vistas++
                try {
                    val existente = try { meta.get(f.name) } catch (_: Exception) { null }
                    // Ficha sana (con artista real): nada que hacer.
                    if (existente != null && !esArtistaBasura(existente.artist)) continue
                    var artista: String? = null
                    if (!esArtistaBasura(f.artist)) artista = f.artist.trim()
                    if (artista == null) {
                        val uri = android.content.ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, f.id
                        )
                        artista = embeddedArtist(uri)
                    }
                    if (artista == null && redUsada < maxRed) {
                        redUsada++
                        artista = try {
                            kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                                artRepo.fetchArtist(f.title, "")
                            }?.trim()?.takeIf { it.isNotBlank() }
                        } catch (_: Exception) { null }
                        // Si la red falló, la próxima pasada lo reintenta.
                        if (artista == null) continue
                    }
                    if (artista == null) continue
                    try {
                        meta.put(
                            f.name,
                            com.melodia.app.data.DownloadMetaStore.Meta(
                                title = existente?.title?.ifBlank { f.title } ?: f.title,
                                artist = artista,
                                album = existente?.album?.ifBlank {
                                    f.album.ifBlank { f.title }
                                } ?: f.album.ifBlank { f.title },
                                artPath = existente?.artPath
                            )
                        )
                        escritas++
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
            // Avanza por lo resuelto en ESTA pasada (antes sumaba vistas==size
            // y el cursor nunca rotaba: la cola sin red quedaba sin turno).
            repairCursor = (repairCursor + escritas + redUsada) % filas.size
        } catch (_: Exception) {}
        escritas
    }

    /** Trae los temas de una playlist (título, temas y si se recortó a 100). */
    suspend fun playlistTracks(url: String): Triple<String, List<YtTrack>, Boolean> = withContext(Dispatchers.IO) {
        ensure()
        val ex = yt().getPlaylistExtractor(url)
        ex.fetchPage()
        val title = (try { ex.name } catch (_: Exception) { "Lista" }) ?: "Lista"
        val out = mutableListOf<YtTrack>()
        var page = ex.initialPage
        var rounds = 0
        var parcial = false
        while (true) {
            out += mapItems(page.items).filterIsInstance<YtSearchItem.Track>().map { it.track }
            if (out.size >= MAX_COLLECTION_TRACKS || !page.hasNextPage() || rounds++ >= 4) break
            // Si la siguiente página falla, lo juntado no se pierde.
            page = try { ex.getPage(page.nextPage) } catch (_: Exception) { parcial = true; break }
        }
        val recortado = parcial || page.hasNextPage() || out.size > MAX_COLLECTION_TRACKS
        // Lo que llega de una playlist suele venir sin artista y con el canal
        // pegado al título ("Tema - Canal Oficial"). Se limpia aquí, antes de
        // descargar, para que el archivo y la biblioteca queden bien.
        val owner = (try { ex.uploaderName } catch (_: Exception) { "" } ?: "")
            .replace(" - Topic", "").replace("VEVO", "").trim()
        val portada = try {
            ex.thumbnails.maxByOrNull { it.height * it.width }?.url
        } catch (_: Exception) { null }
        val limpio = out.take(100).map { t ->
            var titulo = t.title
            if (owner.isNotBlank() && titulo.endsWith(" - $owner", ignoreCase = true)) {
                titulo = titulo.dropLast(owner.length + 3).trim()
            }
            t.copy(
                title = titulo.ifBlank { t.title },
                artist = t.artist.ifBlank { owner },
                thumbUrl = t.thumbUrl ?: portada
            )
        }
        Triple(title, limpio, recortado)
    }

    /** Descarga bytes crudos (miniatura para incrustar como carátula). */
    suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            // Cortos: quien llama pone withTimeoutOrNull(4 s) y el hilo IO
            // no debe seguir bloqueado 15+20 s cuando ya se abandonó.
            conn.connectTimeout = 4_000
            conn.readTimeout = 6_000
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
    /** Nombre único por descarga: lleva el id para no colisionar ni con MediaStore. */
    fun fileNamePara(track: YtTrack): String {
        val base = sanitize("${track.title} - ${track.artist.ifBlank { "YouTube" }}") +
            " [" + track.id.take(16).replace(Regex("[^A-Za-z0-9_-]"), "") + "]"
        // El límite real es 255 BYTES (CJK = 3 B, emoji = 4 B por letra).
        if (base.toByteArray(Charsets.UTF_8).size <= 200) return base
        var end = base.length
        while (end > 16 && base.substring(0, end).toByteArray(Charsets.UTF_8).size > 200) end--
        return base.substring(0, end).trim()
    }

    suspend fun storeDownload(
        track: YtTrack, high: Boolean, audio: YtAudio, part: File,
        lyrics: String? = null, artBytes: ByteArray? = null, artMime: String? = null,
        artistOverride: String? = null
    ): Uri = withContext(Dispatchers.IO) {
        val fileName = fileNamePara(track) + "." + audio.ext
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT < 29) {
            try {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Melodia").mkdirs()
            } catch (_: Exception) {}
        }
        val ov = artistOverride?.trim()
        val artista = if (ov.isNullOrBlank()) artistaDe(track) else ov
        val titulo = track.title
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, titulo)
            put(MediaStore.Audio.Media.ARTIST, artista)
            put(MediaStore.Audio.Media.ALBUM, titulo)
            if (Build.VERSION.SDK_INT >= 30) {
                put(MediaStore.Audio.Media.ALBUM_ARTIST, artista)
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
                        raw, titulo, artista, titulo, lyrics, artBytes, artMime
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
            // Re-afirmar tras reescribir el archivo: en algunos equipos el
            // re-escaneo posterior pisa los metadatos con lo que trae el
            // archivo. La fila manda, no el escáner.
            try {
                resolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, titulo)
                        put(MediaStore.Audio.Media.ARTIST, artista)
                        put(MediaStore.Audio.Media.ALBUM, titulo)
                    },
                    null as String?, null as Array<String>?
                )
            } catch (_: Exception) {}
            // Clave con el nombre REAL en MediaStore (si el sistema renombró
            // por colisión, la ficha con el nombre pedido quedaba huérfana).
            val claveReal = try {
                resolver.query(uri, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            } catch (_: Exception) { null }
            val rutaArte = guardarPortada(artBytes, fileName)
            guardarMeta(if (claveReal.isNullOrBlank()) fileName else claveReal, track, artista, titulo, null, rutaArte)
            uri
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            throw e
        }
    }
}

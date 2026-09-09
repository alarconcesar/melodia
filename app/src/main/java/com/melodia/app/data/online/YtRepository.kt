package com.melodia.app.data.online

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem.InfoType
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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
    private val audioCache = mutableMapOf<String, Pair<Long, YtAudio>>()

    suspend fun search(query: String): List<YtTrack> = withContext(Dispatchers.IO) {
        ensure()
        val ex = yt().getSearchExtractor(query)
        ex.fetchPage()
        val page = ex.initialPage
        pager = ex
        next = try { page.nextPage } catch (_: Exception) { null }
        mapItems(page.items)
    }

    /** Devuelve true si hay más páginas por cargar. */
    fun hasMore(): Boolean = pager != null && next != null

    suspend fun more(): List<YtTrack> = withContext(Dispatchers.IO) {
        val ex = pager ?: return@withContext emptyList()
        val np = next ?: return@withContext emptyList()
        ensure()
        val page = ex.getPage(np)
        next = try { page.nextPage } catch (_: Exception) { null }
        mapItems(page.items)
    }

    private fun mapItems(items: List<org.schabi.newpipe.extractor.InfoItem>): List<YtTrack> {
        return items.mapNotNull { item ->
            try {
                if (item.infoType != InfoType.STREAM) return@mapNotNull null
                val s = item as StreamInfoItem
                if (s.streamType == StreamType.LIVE_STREAM || s.streamType == StreamType.AUDIO_LIVE_STREAM) return@mapNotNull null
                val url = s.url ?: return@mapNotNull null
                val id = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.getOrNull(1)
                    ?: url.substringAfterLast("/").substringBefore("?").ifBlank { return@mapNotNull null }
                val thumbs = try { s.thumbnails } catch (_: Exception) { emptyList() }
                YtTrack(
                    id = id,
                    title = (try { s.name } catch (_: Exception) { "Sin título" }) ?: "Sin título",
                    artist = ((try { s.uploaderName } catch (_: Exception) { "" }) ?: "")
                        .replace(" - Topic", "").replace("VEVO", "").trim(),
                    durationSec = try { s.duration } catch (_: Exception) { -1L },
                    thumbUrl = try { thumbs.maxByOrNull { it.height * it.width }?.url } catch (_: Exception) { null },
                    url = url
                )
            } catch (_: Exception) { null }
        }
    }

    /** high=true: mejor bitrate (alta). false: bitrate modesto (estándar / ahorro de datos). */
    suspend fun bestAudio(videoUrl: String, high: Boolean = true): YtAudio = withContext(Dispatchers.IO) {
        ensure()
        // Caché: las URLs de audio valen horas — re-escuchar es instantáneo
        val cacheKey = "$videoUrl|${if (high) "h" else "s"}"
        synchronized(audioCache) {
            val hit = audioCache[cacheKey]
            if (hit != null && System.currentTimeMillis() - hit.first < 5 * 60 * 60 * 1000L) {
                return@withContext hit.second
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
                audioCache[cacheKey] = System.currentTimeMillis() to it
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

    /** Descarga el audio a Música/Melodia y devuelve su Uri de MediaStore. */
    suspend fun download(track: YtTrack, onProgress: (Float) -> Unit, high: Boolean = true): Uri = withContext(Dispatchers.IO) {
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
            put(MediaStore.Audio.Media.MIME_TYPE, audio.mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Melodia")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo")
        try {
            val conn = URL(audio.url).openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.connect()
            val total = conn.contentLengthLong.takeIf { it > 0 }
            resolver.openOutputStream(uri)?.use { out ->
                conn.inputStream.use { inp ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total != null) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
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
        }
    }
}

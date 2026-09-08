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

    suspend fun search(query: String): List<YtTrack> = withContext(Dispatchers.IO) {
        ensure()
        val ex = yt().getSearchExtractor(query)
        ex.fetchPage()
        ex.initialPage.items.mapNotNull { item ->
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
                    artist = (try { s.uploaderName } catch (_: Exception) { "" }) ?: "",
                    durationSec = try { s.duration } catch (_: Exception) { -1L },
                    thumbUrl = try { thumbs.maxByOrNull { it.height * it.width }?.url } catch (_: Exception) { null },
                    url = url
                )
            } catch (_: Exception) { null }
        }
    }

    suspend fun bestAudio(videoUrl: String): YtAudio = withContext(Dispatchers.IO) {
        ensure()
        val ex = yt().getStreamExtractor(videoUrl)
        ex.fetchPage()
        val audios: List<AudioStream> = ex.audioStreams
        require(audios.isNotEmpty()) { "Sin audio disponible" }
        val ranked = audios.sortedByDescending { try { it.averageBitrate } catch (_: Exception) { 0 } }
        val pick = ranked.firstOrNull { (try { it.codec } catch (_: Exception) { "" } ?: "").contains("mp4a", true) }
            ?: ranked.firstOrNull { (try { it.codec } catch (_: Exception) { "" } ?: "").contains("opus", true) }
            ?: ranked.first()
        val isMp4 = (try { pick.codec } catch (_: Exception) { "" } ?: "").contains("mp4a", true)
        YtAudio(pick.content, if (isMp4) "m4a" else "webm", if (isMp4) "audio/mp4" else "audio/webm")
    }

    private fun sanitize(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().take(80).ifBlank { "audio" }

    /** Descarga el audio a Música/Melodia y devuelve su Uri de MediaStore. */
    suspend fun download(track: YtTrack, onProgress: (Float) -> Unit): Uri = withContext(Dispatchers.IO) {
        val audio = bestAudio(track.url)
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

package com.melodia.app.data.online

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

enum class DlStatus { QUEUED, ACTIVE, PAUSED, DONE, ERROR }

data class DlJob(
    val track: YtTrack,
    val high: Boolean,
    val status: DlStatus,
    val progress: Float = 0f,
    val downloaded: Long = 0L,
    val total: Long = 0L,
    val error: String? = null
)

/**
 * Descargas en paralelo (máx. 3) con pausa, reintento y cola persistida.
 * Baja a un .part en caché privada (reanuda con Range) y al terminar
 * avisa a [onFinished] para guardar en MediaStore + etiquetar.
 */
class DownloadManager(private val context: Context, private val yt: YtRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val slots = Semaphore(3)
    private val dir = File(context.cacheDir, "dl").apply { mkdirs() }

    private val _jobs = MutableStateFlow<List<DlJob>>(emptyList())
    val jobs: StateFlow<List<DlJob>> = _jobs

    /** Progreso por id (compatible con la UI existente). */
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress

    var onFinished: (suspend (YtTrack, Boolean, YtAudio, File) -> Unit)? = null

    private val running = mutableMapOf<String, Job>()

    init {
        // Restaura la cola persistida como pausada (el .part se conserva)
        scope.launch {
            val saved = readQueue()
            if (saved.isNotEmpty()) {
                _jobs.value = saved.map { (t, h, dl, tot) ->
                    val partLen = partFile(t.id, h).let { if (it.exists()) it.length() else 0L }
                    DlJob(t, h, DlStatus.PAUSED, progressOf(partLen, tot), partLen, tot)
                }
                syncProgress()
            }
        }
    }

    fun enqueue(track: YtTrack, high: Boolean) {
        synchronized(running) {
            if (_jobs.value.any { it.track.id == track.id } || running.containsKey(track.id)) return
            _jobs.value += DlJob(track, high, DlStatus.QUEUED)
            persist()
            running[track.id] = scope.launch {
                try {
                    slots.acquire()
                    try { runJob(track, high, fromScratch = false) }
                    finally { slots.release() }
                } catch (_: Exception) {}
                synchronized(running) { running.remove(track.id) }
            }
        }
    }

    fun enqueueAll(tracks: List<YtTrack>, high: Boolean) {
        tracks.forEach { enqueue(it, high) }
    }

    fun pause(id: String) {
        scope.launch {
            synchronized(running) { running.remove(id) }?.cancelAndJoin()
            update(id) { j ->
                if (j.status == DlStatus.ACTIVE || j.status == DlStatus.QUEUED)
                    j.copy(status = DlStatus.PAUSED) else j
            }
            persist()
        }
    }

    fun resume(id: String) {
        val j = _jobs.value.find { it.track.id == id } ?: return
        if (j.status != DlStatus.PAUSED && j.status != DlStatus.ERROR) return
        synchronized(running) {
            if (running.containsKey(id)) return
            update(id) { it.copy(status = DlStatus.QUEUED, error = null) }
            running[id] = scope.launch {
                try {
                    slots.acquire()
                    try { runJob(j.track, j.high, fromScratch = j.status == DlStatus.ERROR && j.downloaded <= 0) }
                    finally { slots.release() }
                } catch (_: Exception) {}
                synchronized(running) { running.remove(id) }
            }
        }
    }

    fun cancel(id: String) {
        scope.launch {
            synchronized(running) { running.remove(id) }?.cancelAndJoin()
            val j = _jobs.value.find { it.track.id == id }
            _jobs.value = _jobs.value.filterNot { it.track.id == id }
            if (j != null) {
                try { partFile(j.track.id, j.high).delete() } catch (_: Exception) {}
            }
            syncProgress()
            persist()
        }
    }

    private suspend fun runJob(track: YtTrack, high: Boolean, fromScratch: Boolean) {
        update(track.id) { it.copy(status = DlStatus.ACTIVE) }
        try {
            val audio = withContext(Dispatchers.IO) { yt.bestAudio(track.url, high) }
            val part = partFile(track.id, high)
            if (fromScratch) try { part.delete() } catch (_: Exception) {}
            var done = if (part.exists()) part.length() else 0L
            var total = _jobs.value.find { it.track.id == track.id }?.total ?: 0L
            // Conexión con Range para reanudar
            val conn = URL(audio.url).openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            if (done > 0) conn.setRequestProperty("Range", "bytes=$done-")
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.connect()
            val code = conn.responseCode
            if (code == 416 || (code == 200 && done > 0)) {
                // El servidor no reanuda: empezar de cero
                try { part.delete() } catch (_: Exception) {}
                done = 0L
            } else if (code !in 200..299 && code != 206) {
                error("Red ${conn.responseCode}")
            }
            val remaining = conn.contentLengthLong.takeIf { it > 0 } ?: 0L
            if (total <= 0 && remaining > 0) total = done + remaining
            update(track.id) { it.copy(total = total) }
            RandomAccessFile(part, "rw").use { raf ->
                raf.seek(done)
                conn.inputStream.use { inp ->
                    val buf = ByteArray(64 * 1024)
                    var lastPush = 0L
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        raf.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - lastPush > 250) {
                            lastPush = now
                            val p = progressOf(done, total)
                            update(track.id) { it.copy(progress = p, downloaded = done, total = total) }
                            syncProgress()
                        }
                    }
                }
            }
            if (total > 0 && done < total) error("Descarga incompleta")
            update(track.id) { it.copy(status = DlStatus.DONE, progress = 1f, downloaded = done, total = total) }
            syncProgress()
            try {
                onFinished?.invoke(track, high, audio, part)
            } finally {
                try { part.delete() } catch (_: Exception) {}
            }
            delay(1500)
            _jobs.value = _jobs.value.filterNot { it.track.id == track.id }
            syncProgress()
            persist()
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            val kept = partFile(track.id, high).let { if (it.exists()) it.length() else 0L }
            val tot = _jobs.value.find { it.track.id == track.id }?.total ?: 0L
            update(track.id) {
                it.copy(
                    status = DlStatus.ERROR, downloaded = kept, total = tot,
                    progress = progressOf(kept, tot),
                    error = (t.message ?: "Error de red").take(80)
                )
            }
            syncProgress()
            persist()
        }
    }

    private fun progressOf(done: Long, total: Long): Float =
        if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f

    private fun partFile(id: String, high: Boolean): File {
        val safe = id.replace(Regex("[^A-Za-z0-9_-]"), "").take(32).ifBlank { "dl" }
        return File(dir, "$safe-${if (high) "h" else "s"}.part")
    }

    private fun update(id: String, f: (DlJob) -> DlJob) {
        _jobs.value = _jobs.value.map { if (it.track.id == id) f(it) else it }
    }

    private fun syncProgress() {
        _progress.value = _jobs.value
            .filter { it.status == DlStatus.ACTIVE || it.status == DlStatus.QUEUED || it.status == DlStatus.PAUSED }
            .associate { it.track.id to it.progress }
    }

    // ── Persistencia de la cola ──
    private fun prefs() = context.getSharedPreferences("melodia_dlq", Context.MODE_PRIVATE)

    private fun persist() {
        try {
            val arr = JSONArray()
            _jobs.value
                .filter { it.status == DlStatus.QUEUED || it.status == DlStatus.PAUSED || it.status == DlStatus.ERROR }
                .take(40)
                .forEach { j ->
                    arr.put(
                        JSONObject()
                            .put("id", j.track.id).put("t", j.track.title).put("a", j.track.artist)
                            .put("d", j.track.durationSec).put("th", j.track.thumbUrl ?: "")
                            .put("u", j.track.url).put("h", j.high)
                            .put("dl", j.downloaded).put("tot", j.total)
                    )
                }
            prefs().edit().putString("q", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun readQueue(): List<Quad> {
        return try {
            val arr = JSONArray(prefs().getString("q", "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val o = arr.getJSONObject(i)
                    Quad(
                        YtTrack(
                            o.getString("id"), o.getString("t"), o.optString("a", ""),
                            o.optLong("d", -1), o.optString("th", "").ifBlank { null }, o.getString("u")
                        ),
                        o.optBoolean("h", true), o.optLong("dl", 0), o.optLong("tot", 0)
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    private data class Quad(val t: YtTrack, val h: Boolean, val dl: Long, val tot: Long)
}

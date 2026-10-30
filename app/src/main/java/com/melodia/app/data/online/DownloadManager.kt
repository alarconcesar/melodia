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
    val error: String? = null,
    /** Velocidad estimada en bytes por segundo (0 = sin dato todavía). */
    val speedBps: Long = 0L
)

/**
 * Descargas en paralelo (máx. 3) con pausa, reintento y cola persistida.
 * Baja a un .part en caché privada (reanuda con Range) y al terminar
 * avisa a [onFinished] para guardar en MediaStore + etiquetar.
 */
class DownloadManager(private val context: Context, private val yt: YtRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Tres transferencias a la vez: la extracción del enlace ya no ocupa cupo.
    private val slots = Semaphore(3)
    // …pero la extracción tampoco es libre: 100 resoluciones a la vez disparan
    // el rate-limit de YouTube. Máx. 3 extracciones concurrentes.
    private val extractSlots = Semaphore(3)
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
            // Si terminó y sigue visible como "Guardada", se retira para bajar
            // de nuevo (antes se ignoraba en silencio durante 8 s).
            _jobs.value.find { it.track.id == track.id && it.status == DlStatus.DONE }?.let { done ->
                _jobs.value = _jobs.value.filterNot { it.track.id == done.track.id }
            }
            if (_jobs.value.any { it.track.id == track.id } || running.containsKey(track.id)) return
            _jobs.value += DlJob(track, high, DlStatus.QUEUED)
            persist()
            running[track.id] = scope.launch {
                try { runJob(track, high, fromScratch = false) } catch (_: Exception) {}
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
                    runJob(j.track, j.high, fromScratch = j.status == DlStatus.ERROR && j.downloaded <= 0)
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
            // Si ya marcó DONE, el .part puede estar copiándose a MediaStore:
            // borrarlo aquí dejaba una copia truncada. DONE lo limpia solo.
            if (j != null && j.status != DlStatus.DONE) {
                try { partFile(j.track.id, j.high).delete() } catch (_: Exception) {}
            }
            syncProgress()
            persist()
        }
    }

    private suspend fun runJob(track: YtTrack, high: Boolean, fromScratch: Boolean) {
        // Sin cupo sigue QUEUED ("En cola"): ACTIVE = transfiriendo de verdad.
        try {
            val audio = withContext(Dispatchers.IO) {
                extractSlots.acquire()
                try { yt.bestAudio(track.url, high) }
                finally { extractSlots.release() }
            }
            runJobFresh(track, high, audio, fromScratch)
        } catch (t: Throwable) {
            // Solo llegan fallos de extracción o del reintento fresco:
            // runJobFresh ya dejó DONE o lanzó ERROR él mismo.
            if (t is kotlinx.coroutines.CancellationException) throw t
            val kept = partFile(track.id, high).let { if (it.exists()) it.length() else 0L }
            val tot = _jobs.value.find { it.track.id == track.id }?.total ?: 0L
            update(track.id) {
                it.copy(
                    status = DlStatus.ERROR, downloaded = kept, total = tot,
                    progress = progressOf(kept, tot),
                    error = com.melodia.app.data.online.YtErrors.message(t, "Error de red").take(90)
                )
            }
            syncProgress()
            persist()
        }
    }

    /**
     * Transfiere bytes con un audio ya resuelto y cierra el ciclo (DONE/ERROR).
     * Separado para reintentar con un enlace fresco ante 403 sin duplicar código.
     */
    private suspend fun runJobFresh(
        track: YtTrack, high: Boolean, audio: YtAudio, fromScratch: Boolean = false,
        /** Reintentos con enlace fresco ante 403 (más de 2 = el CDN no cede). */
        fresh: Int = 0
    ) {
        try {
            val part = partFile(track.id, high)
            if (fromScratch) try { part.delete() } catch (_: Exception) {}
            var done = if (part.exists()) part.length() else 0L
            var total = _jobs.value.find { it.track.id == track.id }?.total ?: 0L
            // Conexión con Range para reanudar
            // Descarga en trozos paralelos (YouTube limita cada conexión). El
            // manager solo aporta el .part, el reanudado y el estado.
            var ultimoBytes = done
            var ultimoMs = System.currentTimeMillis()
            var velocidad = 0L
            var descargado = done
            slots.acquire()
            update(track.id) { it.copy(status = DlStatus.ACTIVE) }
            try {
            descargado = yt.downloadToFile(audio.url, part, done) { d, tot ->
                val ahora = System.currentTimeMillis()
                val dt = ahora - ultimoMs
                if (dt >= 400) {
                    val inst = ((d - ultimoBytes) * 1000L) / dt
                    velocidad = if (velocidad <= 0L) inst else (velocidad * 2 + inst) / 3
                    ultimoBytes = d
                    ultimoMs = ahora
                }
                if (tot != total) total = tot
                update(track.id) {
                    it.copy(
                        progress = progressOf(d, tot), downloaded = d, total = tot,
                        speedBps = velocidad
                    )
                }
                syncProgress()
            }
            } finally { slots.release() }
            done = descargado
            if (total > 0 && done < total) error("Descarga incompleta")
            update(track.id) { it.copy(status = DlStatus.DONE, progress = 1f, downloaded = done, total = total) }
            syncProgress()
            try {
                onFinished?.invoke(track, high, audio, part)
            } finally {
                try { part.delete() } catch (_: Exception) {}
            }
            // La tarjeta queda como "Guardada" unos segundos: desaparecer al
            // instante parecía que la descarga se había esfumado.
            delay(8_000)
            _jobs.value = _jobs.value.filterNot { it.track.id == track.id }
            syncProgress()
            persist()
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            if (t is DlUrlExpiredException && fresh < 2) {
                // La URL del CDN murió antes de tiempo: reintento con enlace
                // fresco. Con tope: si el CDN veta de verdad, cae a ERROR en
                // vez de girar para siempre entre bestAudio y descarga.
                val newAudio = withContext(Dispatchers.IO) {
                    extractSlots.acquire()
                    try { yt.bestAudio(track.url, high, force = true) }
                    finally { extractSlots.release() }
                }
                update(track.id) { it.copy(status = DlStatus.ACTIVE, error = null) }
                runJobFresh(track, high, newAudio, fromScratch = true, fresh = fresh + 1)
                return
            }
            val kept = partFile(track.id, high).let { if (it.exists()) it.length() else 0L }
            val tot = _jobs.value.find { it.track.id == track.id }?.total ?: 0L
            update(track.id) {
                it.copy(
                    status = DlStatus.ERROR, downloaded = kept, total = tot,
                    progress = progressOf(kept, tot),
                    error = com.melodia.app.data.online.YtErrors.message(t, "Error de red").take(90)
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
            // ACTIVE también: al reabrir entra como PAUSED y retoma el .part.
            // Antes se perdía y el .part quedaba huérfano para siempre.
            _jobs.value
                .filter { it.status == DlStatus.QUEUED || it.status == DlStatus.PAUSED || it.status == DlStatus.ERROR || it.status == DlStatus.ACTIVE }
                .take(100)
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

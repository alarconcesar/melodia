package com.melodia.app.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentIndex: Int = -1,
    val currentId: Long = -1L,
    val pitch: Float = 1f,
    val speed: Float = 1f,
    val crossfade: Boolean = false,
    val crossfadeSecs: Int = 3,
    /** elapsedRealtime() del momento en que se midió positionMs (para interpolar). */
    val snapshotElapsedMs: Long = 0L
)

/** ID virtual del tema que suena desde Online (no existe en la biblioteca). */
const val YT_STREAM_ID = -100L

/** Saltos seguidos por error antes de rendirse (evita bucles con archivos muertos). */
const val MAX_ERROR_SKIPS = 5

class MusicPlayer(private val context: Context) {
    var eq: EqualizerManager? = null
    private var exo: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state
    private var queueIds: MutableList<Long> = mutableListOf()
    private var songMap: Map<Long, com.melodia.app.data.model.Song> = emptyMap()
    private var listener: Player.Listener? = null
    private var bound = false

    /**
     * URL de stream vencida o rechazada (403/404/410): el ViewModel re-resuelve
     * el audio y llama a [swapStream] conservando la posición.
     */
    var onStreamError: ((positionMs: Long) -> Unit)? = null
    private var streamRetries = 0
    private val maxStreamRetries = 2
    /** Un archivo local borrado/movido: la app avisa y salta al siguiente. */
    var onUnplayable: ((id: Long) -> Unit)? = null
    private var errorSkips = 0

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val player = (service as? MelodiaPlaybackService.LocalBinder)?.getPlayer()
                if (player != null) {
                    exo = player
                    attachListener(player)
                    updateState()
                    if (pendingRestore != null) { try { applyPendingRestore(player) } catch (_: Exception) {} }
                    while (true) { val op = try { pendingOps.removeFirstOrNull() } catch (_: Exception) { null } ?: break; try { op() } catch (_: Exception) {} }
                }
            } catch (e: Exception) { Log.e("MusicPlayer", "bind fail", e) }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            try { listener?.let { exo?.removeListener(it) } } catch (_: Exception) {}
            exo = null
        }
    }

    private fun attachListener(player: ExoPlayer) {
        try { listener?.let { player.removeListener(it) } } catch (_: Exception) {}
        listener = object : Player.Listener {
            override fun onIsPlayingChanged(v: Boolean) {
                if (!v && !(try { pauseFadeJob?.isActive } catch (_: Exception) { false } ?: false)) {
                    // Pausa manual (o por foco de audio) en medio de un fundido
                    // de crossfade: el volumen quedaba muerto. Se restaura.
                    try { fadeJob?.cancel() } catch (_: Exception) {}
                    fadedOut = false
                    try { exo?.volume = 1f } catch (_: Exception) {}
                }
                updateState()
            }
            override fun onMediaItemTransition(m: MediaItem?, r: Int) {
            errorSkips = 0
            fadedOut = false
            updateState()
            // Fundido de entrada al cambiar de tema
            if (crossfade) rampVolume(1f, (crossfadeSecs * 1000L).coerceAtMost(3000))
        }
            override fun onPlaybackStateChanged(s: Int) { updateState(); attachEq() }
            override fun onAudioSessionIdChanged(id: Int) { try { eq?.attach(id) } catch (_: Exception) {} }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                try {
                    val p = exo ?: return
                    val item = try { p.currentMediaItem } catch (_: Exception) { null }
                    val id = try { item?.mediaId?.toLongOrNull() } catch (_: Exception) { null }
                        ?: queueIds.getOrNull(p.currentMediaItemIndex) ?: return
                    if (id == YT_STREAM_ID) {
                        val code = httpCode(error)
                        if (code == 403 || code == 404 || code == 410) {
                            if (streamRetries < maxStreamRetries) {
                                streamRetries++
                                onStreamError?.invoke(p.currentPosition.coerceAtLeast(0))
                            }
                        }
                        return
                    }
                    // Local irreproducible: avisar y saltar al siguiente (con tope).
                    errorSkips++
                    try { onUnplayable?.invoke(id) } catch (_: Exception) {}
                    if (errorSkips <= MAX_ERROR_SKIPS) {
                        try { p.seekToNextMediaItem(); p.prepare(); p.play() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
            
        }.also { player.addListener(it) }
    }

    fun initialize() {
        if (bound) return
        try {
            // Solo bind al abrir: el foreground + notificación se activan al sonar
            val intent = Intent(context, MelodiaPlaybackService::class.java)
            bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            scope.launch {
                while (isActive) {
                    updateState()
                    checkFadeOut()
                    // En pausa no hace falta latir a 2 Hz: menos wakeups y menos recomposición.
                    delay(if (_state.value.isPlaying) 500L else 1_500L)
                }
            }
        } catch (e: Exception) { Log.e("MusicPlayer", "init fail", e) }
    }

    fun setQueue(songs: List<com.melodia.app.data.model.Song>) {
        songMap = songs.associateBy { it.id }
    }

    /** Bindea el servicio al primer uso (el arranque de la app no lo toca). */
    private fun ensureBound() { if (!bound) initialize() }

    /**
     * Acciones pendientes mientras el servicio conecta. Es una COLA: antes era
     * un slot único y "A continuación" + play rápido en frío perdía los inserts.
     */
    private val pendingOps = ArrayDeque<() -> Unit>()

    fun play(ids: List<Long>, startIndex: Int) {
        ensureBound()
        val p = exo
        if (p == null) { pendingOps.add { play(ids, startIndex) }; return }
        try {
            // Solo los ids con item real: si no, queueIds y timeline se desfasan.
            val pares = ids.mapNotNull { id -> songMap[id]?.let { id to buildItem(it) } }
            if (pares.isEmpty()) return
            queueIds = pares.map { it.first }.toMutableList()
            streamRetries = 0
            pendingRestore = null
            val items = pares.map { it.second }
            val wanted = ids.getOrNull(startIndex)
            val start = pares.indexOfFirst { it.first == wanted }.takeIf { it >= 0 } ?: 0
            val wasShuffle = try { p.shuffleModeEnabled } catch (_: Exception) { false }
            // Con shuffle activo el índice se interpreta en orden mezclado:
            // se apaga un momento para colocar el tema pedido y se restaura.
            try { if (wasShuffle) p.shuffleModeEnabled = false } catch (_: Exception) {}
            try { p.volume = 1f } catch (_: Exception) {}
            fadedOut = false
            p.setMediaItems(items, start.coerceIn(0, items.size - 1), 0)
            try { if (wasShuffle) p.shuffleModeEnabled = true } catch (_: Exception) {}
            p.prepare()
            p.play()
            updateState()
            attachEq()
        } catch (e: Exception) { Log.e("MusicPlayer", "play fail", e) }
    }

    /** Código HTTP del error, caminando la cadena de causas (ExoPlayer anida). */
    private fun httpCode(error: Throwable): Int? {
        var t: Throwable? = error
        var depth = 0
        while (t != null && depth < 6) {
            if (t is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                return t.responseCode
            }
            t = t.cause
            depth++
        }
        return null
    }

    private fun buildItem(s: com.melodia.app.data.model.Song): MediaItem {
        return MediaItem.Builder()
            .setUri(s.dataPath)
            .setMediaId(s.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist.ifBlank { "Desconocido" })
                    .setAlbumTitle(s.album)
                    .build()
            )
            .build()
    }

    /** Inserta un tema local en la posición indicada de la cola. */
    private fun insertAt(id: Long, index: Int): Boolean {
        return try {
            val s = songMap[id] ?: return false
            val p = exo
            if (p == null) {
                // Nadie ha reproducido todavía: la cola se aplica al conectar el servicio
                pendingOps.add { try { insertAt(id, index) } catch (_: Exception) {} }
                ensureBound()
                return true
            }
            ensureRestore(p)
            val idx = index.coerceIn(0, minOf(queueIds.size, p.mediaItemCount))
            queueIds.add(idx, id)
            p.addMediaItem(idx, buildItem(s))
            updateState()
            true
        } catch (e: Exception) { Log.e("MusicPlayer", "insert fail", e); false }
    }

    /** Traduce un índice de la cola visible a índice del timeline (con shuffle difieren). */
    private fun timelineIndexOf(id: Long, uiIndex: Int): Int {
        val p = exo ?: return -1
        return try {
            if (!(try { p.shuffleModeEnabled } catch (_: Exception) { false })) return uiIndex
            val n = p.mediaItemCount
            for (i in 0 until n) {
                if (try { p.getMediaItemAt(i).mediaId } catch (_: Exception) { null } == id.toString()) return i
            }
            -1
        } catch (_: Exception) { -1 }
    }

    /** Aplica la restauración pendiente si la hay (la primera acción real la consume). */
    private fun ensureRestore(p: ExoPlayer) {
        try { if (pendingRestore != null) applyPendingRestore(p) } catch (_: Exception) {}
    }

    /** El tema sonará justo después del actual. */
    fun playNext(id: Long): Boolean {
        val idx = try { exo?.currentMediaItemIndex ?: -1 } catch (_: Exception) { -1 }
        return insertAt(id, if (idx < 0) 0 else idx + 1)
    }

    /** El tema se agrega al final de la cola. */
    fun addToEnd(id: Long): Boolean = insertAt(id, queueIds.size)

    /** Reproduce un stream online (URL directa de audio). */
    fun playStream(url: String, title: String, artist: String, artUrl: String? = null) {
        ensureBound()
        val p = exo
        if (p == null) { pendingOps.add { playStream(url, title, artist, artUrl) }; return }
        try {
            queueIds = mutableListOf(YT_STREAM_ID)
            streamRetries = 0
            pendingRestore = null
            val item = MediaItem.Builder()
                .setUri(url)
                .setMediaId(YT_STREAM_ID.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist.ifBlank { "YouTube" })
                        .setAlbumTitle("Online")
                        .apply { try { if (!artUrl.isNullOrBlank()) setArtworkUri(android.net.Uri.parse(artUrl)) } catch (_: Exception) {} }
                        .build()
                )
                .build()
            p.setMediaItem(item)
            p.prepare()
            p.play()
            updateState()
            attachEq()
        } catch (e: Exception) { Log.e("MusicPlayer", "playStream fail", e) }
    }

    fun pauseOnly() { try { exo?.pause() } catch (_: Exception) {} }

    /**
     * Deja la cola preparada para cuando el usuario le dé play (restauración al
     * abrir la app). A propósito NO bindea el servicio: el arranque sigue siendo
     * ligero y ExoPlayer se crea recién al primer play.
     */
    fun restore(
        ids: List<Long>,
        startIndex: Int,
        positionMs: Long,
        shuffle: Boolean = false,
        repeatMode: Int = Player.REPEAT_MODE_OFF
    ) {
        pendingRestore = PendingRestore(ids, startIndex, positionMs, shuffle, repeatMode)
    }

    private data class PendingRestore(
        val ids: List<Long>,
        val index: Int,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeatMode: Int
    )

    private var pendingRestore: PendingRestore? = null

    /** Aplica la cola restaurada antes de la primera reproducción. */
    private fun applyPendingRestore(p: ExoPlayer) {
        val r = pendingRestore ?: return
        pendingRestore = null
        try {
            val pares = r.ids.mapNotNull { id -> songMap[id]?.let { id to buildItem(it) } }
            if (pares.isEmpty()) return
            queueIds = pares.map { it.first }.toMutableList()
            val wanted = r.ids.getOrNull(r.index)
            val start = pares.indexOfFirst { it.first == wanted }.takeIf { it >= 0 } ?: 0
            // La posición no puede pasar la duración real del tema (se queda al final si no).
            val durConocida = try { songMap[pares[start].first]?.durationMs ?: 0L } catch (_: Exception) { 0L }
            val pos = r.positionMs.coerceAtLeast(0L).let { v ->
                if (durConocida > 3_000L) v.coerceAtMost(durConocida - 2_000L) else v
            }
            try { p.shuffleModeEnabled = r.shuffle } catch (_: Exception) {}
            try {
                val rm = r.repeatMode
                p.repeatMode = if (rm == Player.REPEAT_MODE_OFF || rm == Player.REPEAT_MODE_ONE || rm == Player.REPEAT_MODE_ALL) rm else Player.REPEAT_MODE_OFF
            } catch (_: Exception) {}
            p.setMediaItems(pares.map { it.second }, start.coerceIn(0, pares.size - 1), pos)
            p.prepare()
            p.playWhenReady = false
            updateState()
        } catch (e: Exception) { Log.e("MusicPlayer", "apply restore fail", e) }
    }

    /**
     * Reemplaza la URL del stream actual conservando la posición y el estado
     * (se usa cuando YouTube invalida la URL y devuelve 403 a mitad de tema).
     */
    fun swapStream(url: String, positionMs: Long) {
        try {
            val p = exo ?: return
            val old = p.currentMediaItem ?: return
            val playing = p.isPlaying
            val item = old.buildUpon().setUri(url).build()
            p.setMediaItem(item, positionMs.coerceAtLeast(0L))
            p.prepare()
            if (playing) p.play()
            updateState()
        } catch (e: Exception) { Log.e("MusicPlayer", "swapStream fail", e) }
    }

    // ── Tono (pitch) y velocidad ──
    private var pitch = 1f
    private var speed = 1f

    private fun applyParams() {
        try {
            exo?.playbackParameters = androidx.media3.common.PlaybackParameters(speed, pitch)
            updateState()
        } catch (e: Exception) { Log.e("MusicPlayer", "params fail", e) }
    }

    fun setPitch(p: Float) { pitch = p.coerceIn(0.25f, 2f); applyParams() }
    fun setSpeed(s: Float) { speed = s.coerceIn(0.25f, 2f); applyParams() }
    fun resetPitchSpeed() { pitch = 1f; speed = 1f; applyParams() }

    /** IDs de la cola actual (copia). */
    fun queueIds(): List<Long> = try { queueIds.toList() } catch (_: Exception) { emptyList() }

    /**
     * Quita un item de la cola (local). Devuelve si el reproductor lo aceptó:
     * la UI solo debe filtrar su lista cuando esto es true.
     */
    fun removeAt(index: Int): Boolean {
        return try {
            val p = exo ?: return false
            if (index !in queueIds.indices) return false
            val id = queueIds[index]
            val tl = timelineIndexOf(id, index)
            if (tl < 0 || tl >= p.mediaItemCount) return false
            p.removeMediaItem(tl)
            queueIds.removeAt(index)
            updateState()
            true
        } catch (e: Exception) { Log.e("MusicPlayer", "removeAt fail", e); false }
    }

    /**
     * Mueve un tema dentro de la cola sin cortar la reproducción.
     *
     * ExoPlayer y [queueIds] son dos listas paralelas: si el movimiento nativo
     * falla, se devuelve false SIN tocar queueIds para que la UI no muestre una
     * cola que el reproductor no tiene.
     */
    fun moveItem(from: Int, to: Int): Boolean {
        return try {
            val p = exo ?: return false
            if (from !in queueIds.indices || to !in queueIds.indices || from == to) return false
            val tlFrom = timelineIndexOf(queueIds[from], from)
            val tlTo = timelineIndexOf(queueIds[to], to)
            if (tlFrom < 0 || tlTo < 0 || tlFrom >= p.mediaItemCount || tlTo >= p.mediaItemCount) return false
            p.moveMediaItem(tlFrom, tlTo)
            val id = queueIds.removeAt(from)
            queueIds.add(to, id)
            updateState()
            true
        } catch (e: Exception) {
            Log.e("MusicPlayer", "moveItem fail", e)
            false
        }
    }

    /**
     * Deja en la cola únicamente el tema que está sonando.
     * Se quitan primero los posteriores y después los anteriores para que el
     * índice actual no se mueva en el camino (el tema sigue sonando sin corte).
     */
    fun clearQueueKeepCurrent(): Boolean {
        return try {
            val p = exo ?: return false
            val cur = p.currentMediaItemIndex
            if (cur < 0 || cur >= p.mediaItemCount) return false
            val total = p.mediaItemCount
            if (total > cur + 1) p.removeMediaItems(cur + 1, total)
            if (cur > 0) p.removeMediaItems(0, cur)
            val id = p.currentMediaItem?.mediaId?.toLongOrNull()
                ?: queueIds.getOrNull(cur)
            queueIds.clear()
            if (id != null) queueIds.add(id)
            updateState()
            true
        } catch (e: Exception) {
            Log.e("MusicPlayer", "clearQueue fail", e)
            false
        }
    }

    private fun attachEq() { try { exo?.let { eq?.attach(it.audioSessionId) } } catch (_: Exception) {} }
    fun toggle() {
        ensureBound()
        val p = exo ?: run { pendingOps.add { toggle() }; return }
        try {
            ensureRestore(p)
            if (p.isPlaying) { pauseForFade(); p.pause() } else p.play()
        } catch (_: Exception) {}
    }
    fun next() { ensureBound(); val p = exo; if (p != null) ensureRestore(p); jumpWithFade { try { exo?.seekToNextMediaItem() } catch (_: Exception) {} } }
    fun prev() { ensureBound(); val p = exo; if (p != null) ensureRestore(p); jumpWithFade { try { exo?.seekToPreviousMediaItem() } catch (_: Exception) {} } }

    // ── Fundido entre canciones (activable) ──
    private val prefs = context.getSharedPreferences("melodia_player", Context.MODE_PRIVATE)
    var crossfade: Boolean = prefs.getBoolean("xfade", false)
        private set
    var crossfadeSecs: Int = prefs.getInt("xfade_secs", 3)
        private set
    private var fadeJob: Job? = null
    private var fadedOut = false

    fun setCrossfade(v: Boolean) {
        crossfade = v
        prefs.edit().putBoolean("xfade", v).apply()
        if (!v) { try { exo?.volume = 1f } catch (_: Exception) {} }
        updateState()
    }

    fun setCrossfadeSecs(s: Int) {
        crossfadeSecs = s.coerceIn(1, 12)
        prefs.edit().putInt("xfade_secs", crossfadeSecs).apply()
        updateState()
    }

    private fun rampVolume(to: Float, ms: Long, then: (() -> Unit)? = null) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                val p = exo ?: return@launch
                val from = try { p.volume } catch (_: Exception) { return@launch }
                val steps = 20
                val stepMs = (ms / steps).coerceAtLeast(10)
                repeat(steps) { i ->
                    if (!isActive) return@launch
                    try { p.volume = from + (to - from) * (i + 1) / steps } catch (_: Exception) { return@launch }
                    delay(stepMs)
                }
                then?.invoke()
            } catch (_: Exception) {}
        }
    }

    /** Fundido de salida automático al acercarse el final del tema. */
    private fun checkFadeOut() {
        try {
            val p = exo ?: return
            if (!crossfade || !p.isPlaying || fadedOut) return
            val dur = p.duration
            if (dur <= 0) return
            val remain = dur - p.currentPosition
            val fadeMs = crossfadeSecs * 1000L
            if (remain in 1..fadeMs) {
                fadedOut = true
                rampVolume(0f, remain.coerceAtLeast(300))
            }
        } catch (_: Exception) {}
    }

    private fun jumpWithFade(jump: () -> Unit) {
        try {
            if (!crossfade) { jump(); return }
            rampVolume(0f, 250) {
                try { jump() } catch (_: Exception) {}
                fadedOut = false
                rampVolume(1f, 1200)
            }
        } catch (_: Exception) { try { jump() } catch (_: Exception) {} }
    }

    /** Pausa con fundido gradual (para el temporizador). Job propio: el
     * crossfade ya no lo cancela a mitad de camino, y si el timer se cancela
     * durante el fundido se aborta en vez de pausar igual. */
    private var pauseFadeJob: Job? = null
    fun fadePause(ms: Long = 8000, stillActive: () -> Boolean = { true }) {
        try { pauseFadeJob?.cancel() } catch (_: Exception) {}
        pauseFadeJob = scope.launch {
            try {
                val p = exo ?: return@launch
                if (!p.isPlaying) return@launch
                val from = try { p.volume } catch (_: Exception) { return@launch }
                val steps = 40
                val stepMs = (ms / steps).coerceAtLeast(50)
                repeat(steps) { i ->
                    if (!isActive) return@launch
                    try { p.volume = from + (0f - from) * (i + 1) / steps } catch (_: Exception) { return@launch }
                    delay(stepMs)
                }
                if (!stillActive()) {
                    // Timer cancelado en pleno fundido: se deja sonando normal.
                    try { p.volume = 1f } catch (_: Exception) {}
                    return@launch
                }
                try { p.pause() } catch (_: Exception) {}
                try { p.volume = 1f } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }
    /** Pausa manual: si el timer estaba fundiendo, se aborta su pausa. */
    private fun pauseForFade() {
        try { pauseFadeJob?.cancel() } catch (_: Exception) {}
        pauseFadeJob = null
        try { exo?.volume = 1f } catch (_: Exception) {}
        fadedOut = false
    }
    fun toggleShuffle() { try { exo?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; updateState() } } catch (_: Exception) {} }
    fun cycleRepeat() {
        try { exo?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }; updateState() } } catch (_: Exception) {}
    }
    fun seekTo(f: Float) {
        ensureBound()
        try { exo?.let {
            ensureRestore(it)
            val d = try { it.duration } catch (_: Exception) { androidx.media3.common.C.TIME_UNSET }
            // Sin duración conocida la fracción no significa nada: no saltar al inicio.
            if (d == androidx.media3.common.C.TIME_UNSET || d <= 0) return@let
            cancelCrossfadeRamp()
            it.seekTo((d * f.coerceIn(0f, 1f)).toLong()); updateState()
        } } catch (_: Exception) {}
    }
    /** Salto relativo en ms (doble toque ±10 s en la carátula). */
    fun seekBy(ms: Long) {
        ensureBound()
        try { exo?.let {
            ensureRestore(it)
            val d = try { it.duration } catch (_: Exception) { androidx.media3.common.C.TIME_UNSET }
            val target = it.currentPosition + ms
            val clamped = if (d != androidx.media3.common.C.TIME_UNSET && d > 0) target.coerceIn(0, d) else target.coerceAtLeast(0)
            cancelCrossfadeRamp()
            it.seekTo(clamped); updateState()
        } } catch (_: Exception) {}
    }
    /** Cancela una rampa de crossfade (un seek manual no debe terminar mudo). */
    private fun cancelCrossfadeRamp() {
        val timerFading = try { pauseFadeJob?.isActive } catch (_: Exception) { false } ?: false
        if (timerFading) return
        try { fadeJob?.cancel() } catch (_: Exception) {}
        fadedOut = false
        try { exo?.volume = 1f } catch (_: Exception) {}
    }

    private fun updateState() {
        try {
            val p = exo ?: return
            val dur = p.duration.takeIf { it > 0 } ?: 0L
            val pos = p.currentPosition.coerceAtLeast(0)
            val next = PlayerState(
                isPlaying = p.isPlaying,
                progress = if (dur > 0) pos.toFloat() / dur else 0f,
                positionMs = pos, durationMs = dur,
                shuffle = p.shuffleModeEnabled, repeatMode = p.repeatMode,
                currentIndex = p.currentMediaItemIndex,
                currentId = try { p.currentMediaItem?.mediaId?.toLongOrNull() } catch (_: Exception) { null }
                    ?: queueIds.getOrNull(p.currentMediaItemIndex) ?: -1L,
                pitch = pitch, speed = speed, crossfade = crossfade, crossfadeSecs = crossfadeSecs,
                snapshotElapsedMs = android.os.SystemClock.elapsedRealtime()
            )
            // Emitir solo si cambió algo real: en pausa el snapshot de tiempo no cuenta.
            val prev = _state.value
            val sameAsPrev = next.copy(snapshotElapsedMs = 0L, positionMs = 0L) ==
                prev.copy(snapshotElapsedMs = 0L, positionMs = 0L)
            if (!sameAsPrev || next.positionMs != prev.positionMs) _state.value = next
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            scope.cancel()
            listener?.let { exo?.removeListener(it) }
            if (bound) { try { context.unbindService(connection) } catch (_: Exception) {} ; bound = false }
            exo = null
        } catch (_: Exception) {}
    }
}

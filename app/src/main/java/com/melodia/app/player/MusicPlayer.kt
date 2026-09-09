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
    val crossfade: Boolean = false
)

/** ID virtual del tema que suena desde Online (no existe en la biblioteca). */
const val YT_STREAM_ID = -100L

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

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val player = (service as? MelodiaPlaybackService.LocalBinder)?.getPlayer()
                if (player != null) {
                    exo = player
                    attachListener(player)
                    updateState()
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
            override fun onIsPlayingChanged(v: Boolean) { updateState() }
            override fun onMediaItemTransition(m: MediaItem?, r: Int) {
            fadedOut = false
            updateState()
            // Fundido de entrada al cambiar de tema
            if (crossfade) rampVolume(1f, (crossfadeSecs * 1000L).coerceAtMost(3000))
        }
            override fun onPlaybackStateChanged(s: Int) { updateState(); attachEq() }
            override fun onAudioSessionIdChanged(id: Int) { try { eq?.attach(id) } catch (_: Exception) {} }
        }.also { player.addListener(it) }
    }

    fun initialize() {
        if (bound) return
        try {
            // Solo bind al abrir: el foreground + notificación se activan al sonar
            val intent = Intent(context, MelodiaPlaybackService::class.java)
            bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            scope.launch { while (isActive) { updateState(); checkFadeOut(); delay(500) } }
        } catch (e: Exception) { Log.e("MusicPlayer", "init fail", e) }
    }

    fun setQueue(songs: List<com.melodia.app.data.model.Song>) {
        songMap = songs.associateBy { it.id }
    }

    fun play(ids: List<Long>, startIndex: Int) {
        val p = exo ?: return
        try {
            queueIds = ids.toMutableList()
            val items = ids.mapNotNull { id -> songMap[id]?.let { buildItem(it) } }
            if (items.isEmpty()) return
            try { p.volume = 1f } catch (_: Exception) {}
            fadedOut = false
            p.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), 0)
            p.prepare()
            p.play()
            updateState()
            attachEq()
        } catch (e: Exception) { Log.e("MusicPlayer", "play fail", e) }
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
            val p = exo ?: return false
            val s = songMap[id] ?: return false
            val idx = index.coerceIn(0, queueIds.size)
            queueIds.add(idx, id)
            p.addMediaItem(idx.coerceAtMost(p.mediaItemCount), buildItem(s))
            updateState()
            true
        } catch (e: Exception) { Log.e("MusicPlayer", "insert fail", e); false }
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
        val p = exo ?: return
        try {
            queueIds = mutableListOf(YT_STREAM_ID)
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

    /** Quita un item de la cola (local). */
    fun removeAt(index: Int) {
        try {
            val p = exo ?: return
            if (index < 0 || index >= queueIds.size || index >= p.mediaItemCount) return
            p.removeMediaItem(index)
            queueIds.removeAt(index)
            updateState()
        } catch (e: Exception) { Log.e("MusicPlayer", "removeAt fail", e) }
    }
    private fun attachEq() { try { exo?.let { eq?.attach(it.audioSessionId) } } catch (_: Exception) {} }
    fun toggle() { try { exo?.let { if (it.isPlaying) it.pause() else it.play() } } catch (_: Exception) {} }
    fun next() { jumpWithFade { try { exo?.seekToNextMediaItem() } catch (_: Exception) {} } }
    fun prev() { jumpWithFade { try { exo?.seekToPreviousMediaItem() } catch (_: Exception) {} } }

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

    /** Pausa con fundido gradual (para el temporizador). */
    fun fadePause(ms: Long = 8000) {
        scope.launch {
            try {
                val p = exo ?: return@launch
                if (!p.isPlaying) return@launch
                rampVolume(0f, ms) {
                    try { p.pause() } catch (_: Exception) {}
                    try { p.volume = 1f } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }
    fun toggleShuffle() { try { exo?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; updateState() } } catch (_: Exception) {} }
    fun cycleRepeat() {
        try { exo?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }; updateState() } } catch (_: Exception) {}
    }
    fun seekTo(f: Float) { try { exo?.let { val d = it.duration.coerceAtLeast(0); it.seekTo((d * f).toLong()) } } catch (_: Exception) {} }
    /** Salto relativo en ms (doble toque ±10 s, modo auto). */
    fun seekBy(ms: Long) { try { exo?.let { it.seekTo((it.currentPosition + ms).coerceAtLeast(0)) } } catch (_: Exception) {} }

    private fun updateState() {
        try {
            val p = exo ?: return
            val dur = p.duration.takeIf { it > 0 } ?: 0L
            val pos = p.currentPosition.coerceAtLeast(0)
            _state.value = PlayerState(
                isPlaying = p.isPlaying,
                progress = if (dur > 0) pos.toFloat() / dur else 0f,
                positionMs = pos, durationMs = dur,
                shuffle = p.shuffleModeEnabled, repeatMode = p.repeatMode,
                currentIndex = p.currentMediaItemIndex,
                currentId = queueIds.getOrNull(p.currentMediaItemIndex) ?: -1L,
                pitch = pitch, speed = speed, crossfade = crossfade
            )
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

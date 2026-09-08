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
    val speed: Float = 1f
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
            override fun onMediaItemTransition(m: MediaItem?, r: Int) { updateState() }
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
            scope.launch { while (isActive) { updateState(); delay(500) } }
        } catch (e: Exception) { Log.e("MusicPlayer", "init fail", e) }
    }

    fun setQueue(songs: List<com.melodia.app.data.model.Song>) {
        songMap = songs.associateBy { it.id }
    }

    fun play(ids: List<Long>, startIndex: Int) {
        val p = exo ?: return
        try {
            queueIds = ids.toMutableList()
            val items = ids.mapNotNull { id ->
                songMap[id]?.let { s ->
                    MediaItem.Builder()
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
            }
            if (items.isEmpty()) return
            p.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), 0)
            p.prepare()
            p.play()
            updateState()
            attachEq()
        } catch (e: Exception) { Log.e("MusicPlayer", "play fail", e) }
    }

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
    fun next() { try { exo?.seekToNextMediaItem() } catch (_: Exception) {} }
    fun prev() { try { exo?.seekToPreviousMediaItem() } catch (_: Exception) {} }
    fun toggleShuffle() { try { exo?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; updateState() } } catch (_: Exception) {} }
    fun cycleRepeat() {
        try { exo?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }; updateState() } } catch (_: Exception) {}
    }
    fun seekTo(f: Float) { try { exo?.let { val d = it.duration.coerceAtLeast(0); it.seekTo((d * f).toLong()) } } catch (_: Exception) {} }

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
                pitch = pitch, speed = speed
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

package com.melodia.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.melodia.app.MainActivity
import com.melodia.app.R
import kotlinx.coroutines.launch

/**
 * Servicio de reproducción con notificación propia estilo Media.
 * - Al abrir la app solo se hace bind (sin foreground, sin ANR).
 * - Al sonar: startForeground + notificación con título/artista/controles.
 * - En pausa: la notificación queda visible (se puede retomar).
 * - Sin cola: se retira la notificación y el servicio se detiene.
 */
class MelodiaPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var isForeground = false

    inner class LocalBinder : Binder() {
        fun getPlayer(): ExoPlayer? = session?.player as? ExoPlayer
    }

    private val localBinder = LocalBinder()

    private val notifListener = object : Player.Listener {
        override fun onIsPlayingChanged(v: Boolean) { updateNotification() }
        override fun onMediaItemTransition(m: androidx.media3.common.MediaItem?, r: Int) { updateNotification() }
        override fun onPlaybackStateChanged(s: Int) { updateNotification() }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            // Arranque rápido: suena con ~1s en buffer (ideal para streams)
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 30_000, 1_000, 1_500)
                    .build()
            )
            .build()
        player.addListener(notifListener)
        session = MediaSession.Builder(this, player)
            .setSessionActivity(contentIntent)
            .build()
        // La notificación visible la publica updateNotification(); la de media3
        // queda como respaldo del sistema sin interferir.
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == MediaSessionService.SERVICE_INTERFACE) {
            super.onBind(intent)
        } else {
            localBinder
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        try {
            when (intent?.action) {
                ACTION_TOGGLE -> session?.player?.let { if (it.isPlaying) it.pause() else it.play() }
                ACTION_NEXT -> session?.player?.seekToNextMediaItem()
                ACTION_PREV -> session?.player?.seekToPreviousMediaItem()
                ACTION_STOP -> {
                    session?.player?.let { it.pause(); it.stop() }
                    stopForegroundCompat(true)
                    stopSelf()
                }
            }
        } catch (e: Exception) { Log.e("MelodiaSvc", "action fail", e) }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
                nm?.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Reproducción", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Controles de reproducción de Melodia"
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    private fun serviceIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, MelodiaPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun updateNotification() {
        try {
            val player = session?.player ?: return
            if (player.mediaItemCount == 0) {
                stopForegroundCompat(true)
                return
            }
            val meta = currentMeta(player)
            val title = meta.title?.toString().ifNullOrBlank("Melodia")
            val artist = meta.artist?.toString().ifNullOrBlank("Desconocido")
            val playing = player.isPlaying
            val large = resolveArt(player, meta, title)

            val contentIntent = session?.sessionActivity
            val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_music)
                .setLargeIcon(large)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(contentIntent)
                .setOngoing(playing)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                    R.drawable.ic_notif_prev, "Anterior",
                    serviceIntent(ACTION_PREV)
                )
                .addAction(
                    if (playing) R.drawable.ic_notif_pause else R.drawable.ic_notif_play,
                    if (playing) "Pausar" else "Reproducir",
                    serviceIntent(ACTION_TOGGLE)
                )
                .addAction(
                    R.drawable.ic_notif_next, "Siguiente",
                    serviceIntent(ACTION_NEXT)
                )
                .setStyle(
                    MediaStyle()
                        .setMediaSession(session?.sessionCompatToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .build()

            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, notif)
            }
            isForeground = true
            // Widget de inicio siempre sincronizado (arte reducido para el binder)
            try {
                val wArt = large?.let {
                    if (it.width > 192 || it.height > 192) {
                        android.graphics.Bitmap.createScaledBitmap(it, 192, 192, true)
                    } else it
                }
                com.melodia.app.widget.MelodiaWidget.refresh(this, title, artist, playing, wArt)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e("MelodiaSvc", "notif fail", e)
        }
    }

    private fun stopForegroundCompat(remove: Boolean) {
        try {
            if (isForeground) {
                if (remove) {
                    if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
                    else @Suppress("DEPRECATION") stopForeground(true)
                } else {
                    stopForeground(false)
                }
                isForeground = false
            } else if (remove) {
                val nm = getSystemService(NotificationManager::class.java)
                nm?.cancel(NOTIF_ID)
            }
        } catch (_: Exception) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.isPlaying) {
            stopForegroundCompat(true)
            stopSelf()
        }
    }

    override fun onDestroy() {
        try { session?.player?.removeListener(notifListener) } catch (_: Exception) {}
        session?.run {
            try { player.release() } catch (_: Exception) {}
            try { release() } catch (_: Exception) {}
        }
        session = null
        super.onDestroy()
    }

    private fun String?.ifNullOrBlank(fallback: String): String =
        if (this.isNullOrBlank()) fallback else this

    // ── Carátula de la notificación ──
    private val artScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )
    private var artKey: String? = null
    private var artBmp: android.graphics.Bitmap? = null

    private fun resolveArt(
        player: Player, meta: androidx.media3.common.MediaMetadata, title: String
    ): android.graphics.Bitmap? {
        return try {
            val mediaId = try { player.currentMediaItem?.mediaId } catch (_: Exception) { null }
            val key = "$mediaId|$title"
            if (mediaId == YT_STREAM_ID.toString()) {
                if (key == artKey) return artBmp
                artKey = key
                artBmp = null
                val url = try { meta.artworkUri?.toString() } catch (_: Exception) { null }
                if (!url.isNullOrBlank()) {
                    artScope.launch {
                        try {
                            val bmp = downloadBitmap(url)
                            if (bmp != null && artKey == key) {
                                artBmp = bmp
                                updateNotification()
                            }
                        } catch (_: Exception) {}
                    }
                }
                null
            } else {
                artKey = key
                artBmp = null
                val sid = mediaId?.toLongOrNull() ?: return null
                if (sid <= 0) return null
                localAlbumArt(sid)
            }
        } catch (_: Exception) { null }
    }

    private fun localAlbumArt(songId: Long): android.graphics.Bitmap? {
        return try {
            val albumId = contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.Audio.Media.ALBUM_ID),
                "${android.provider.MediaStore.Audio.Media._ID}=?",
                arrayOf(songId.toString()), null
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else -1L } ?: -1L
            if (albumId <= 0) return null
            if (Build.VERSION.SDK_INT >= 29) {
                contentResolver.loadThumbnail(
                    android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId
                    ),
                    android.util.Size(512, 512), null
                )
            } else {
                @Suppress("DEPRECATION")
                contentResolver.openInputStream(
                    android.content.ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"), albumId
                    )
                )?.use { android.graphics.BitmapFactory.decodeStream(it) }
            }
        } catch (_: Exception) { null }
    }

    private fun downloadBitmap(url: String): android.graphics.Bitmap? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.connect()
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }

    /** Metadatos del item actual (compatible con media3 1.2.x). */
    private fun currentMeta(player: Player): androidx.media3.common.MediaMetadata {
        return try {
            val i = player.currentMediaItemIndex
            if (i in 0 until player.mediaItemCount) player.getMediaItemAt(i).mediaMetadata
            else player.mediaMetadata
        } catch (_: Exception) {
            try { player.mediaMetadata } catch (_: Exception) {
                androidx.media3.common.MediaMetadata.Builder().build()
            }
        }
    }

    companion object {
        const val NOTIF_ID = 2001
        const val CHANNEL_ID = "melodia_playback"
        const val ACTION_TOGGLE = "com.melodia.app.TOGGLE"
        const val ACTION_NEXT = "com.melodia.app.NEXT"
        const val ACTION_PREV = "com.melodia.app.PREV"
        const val ACTION_STOP = "com.melodia.app.STOP"
    }
}

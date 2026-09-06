package com.melodia.app.player

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servicio foreground con MediaSession: muestra la notificación de
 * reproducción del sistema (controles play/pausa/next, carátula en
 * lockscreen) y mantiene la música sonando con la app en segundo plano.
 */
class MelodiaPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    inner class LocalBinder : Binder() {
        fun getPlayer(): ExoPlayer? = session?.player as? ExoPlayer
    }

    private val localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()
        session = MediaSession.Builder(this, player).build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this)
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == MediaSessionService.SERVICE_INTERFACE) {
            super.onBind(intent)
        } else {
            localBinder
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        // Si no está sonando, liberar; si suena, la notificación lo mantiene vivo
        if (player == null || !player.playWhenReady || player.playbackState == ExoPlayer.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val R_NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "melodia_playback"
    }
}

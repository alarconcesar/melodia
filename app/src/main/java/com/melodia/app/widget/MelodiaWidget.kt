package com.melodia.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.melodia.app.MainActivity
import com.melodia.app.R
import com.melodia.app.player.MelodiaPlaybackService

/** Widget 4×1: carátula, título, artista y controles. Lo actualiza el servicio. */
class MelodiaWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        refresh(ctx)
    }

    /**
     * Los botones pasaban por getService directo: con el servicio muerto (tras
     * reinicio, sin haber abierto la app) el tap fallaba en silencio. Ahora
     * pasan por el receiver, que intenta arrancar el servicio y, si no puede,
     * abre la app para que el usuario retome desde ahí.
     */
    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val act = intent.action ?: return
        if (act !in ROUTED) return
        try {
            androidx.core.content.ContextCompat.startForegroundService(
                ctx, Intent(ctx, MelodiaPlaybackService::class.java).setAction(act)
            )
        } catch (_: Exception) {
            try {
                ctx.startActivity(
                    Intent(ctx, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            } catch (_: Exception) {}
        }
    }

    companion object {
        @Volatile private var title = "Melodia"
        @Volatile private var artist = "Tu música"
        @Volatile private var playing = false
        @Volatile private var art: Bitmap? = null
        @Volatile private var artScaled: Bitmap? = null
        @Volatile private var pos: Long = 0L
        @Volatile private var dur: Long = 0L
        private var artScaledFor: Bitmap? = null

        /** Solo avanza la barra (ticker del servicio): sin re-escalar arte. */
        fun tick(ctx: Context, posMs: Long, durMs: Long) {
            pos = posMs
            dur = durMs
            push(ctx)
        }

        fun refresh(ctx: Context, t: String? = null, a: String? = null, p: Boolean? = null, bitmap: Bitmap? = undefined(), posMs: Long? = null, durMs: Long? = null) {
            try {
                if (t != null) title = t
                if (a != null) artist = a
                if (p != null) playing = p
                if (bitmap !== UNSET) {
                    art = bitmap
                    artScaled = null
                }
                if (posMs != null) pos = posMs
                if (durMs != null) dur = durMs
                push(ctx)
            } catch (_: Exception) {}
        }

        private fun push(ctx: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, MelodiaWidget::class.java))
                if (ids.isEmpty()) return
                val rv = RemoteViews(ctx.packageName, R.layout.widget_player)
                rv.setTextViewText(R.id.w_title, title)
                rv.setTextViewText(R.id.w_artist, artist)
                rv.setImageViewResource(
                    R.id.w_play,
                    if (playing) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
                )
                var pig = artScaled
                if (art == null) {
                    artScaled = null
                    pig = null
                } else if (pig == null || artScaledFor !== art) {
                    pig = try {
                        val b = art!!
                        if (b.width > 192 || b.height > 192) {
                            val k = 192f / maxOf(b.width, b.height)
                            Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
                        } else b
                    } catch (_: Exception) { null }
                    artScaled = pig
                    artScaledFor = art
                }
                if (pig != null) {
                    try { rv.setImageViewBitmap(R.id.w_art, pig) } catch (_: Exception) {}
                } else {
                    try { rv.setImageViewResource(R.id.w_art, R.drawable.ic_notif_music) } catch (_: Exception) {}
                }
                rv.setOnClickPendingIntent(R.id.w_prev, route(ctx, MelodiaPlaybackService.ACTION_PREV, 1))
                rv.setOnClickPendingIntent(R.id.w_play, route(ctx, MelodiaPlaybackService.ACTION_TOGGLE, 2))
                rv.setOnClickPendingIntent(R.id.w_next, route(ctx, MelodiaPlaybackService.ACTION_NEXT, 3))
                rv.setOnClickPendingIntent(R.id.w_sleep, route(ctx, MelodiaPlaybackService.ACTION_SLEEP, 5))
                try {
                    rv.setProgressBar(R.id.w_progress, 1000, if (dur > 0) ((pos.toFloat() / dur) * 1000).toInt().coerceIn(0, 1000) else 0, false)
                } catch (_: Exception) {}
                val open = PendingIntent.getActivity(
                    ctx, 4,
                    Intent(ctx, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                rv.setOnClickPendingIntent(R.id.w_root, open)
                ids.forEach { id ->
                    try { mgr.updateAppWidget(id, rv) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        // Marcador para "no cambiar el arte" (null = quitarlo)
        private val UNSET: Bitmap? = null
        private fun undefined(): Bitmap? = UNSET

        /** Acciones que pasan por el receiver (con fallback a abrir la app). */
        internal val ROUTED = setOf(
            MelodiaPlaybackService.ACTION_TOGGLE,
            MelodiaPlaybackService.ACTION_PREV,
            MelodiaPlaybackService.ACTION_NEXT,
            MelodiaPlaybackService.ACTION_SLEEP
        )

        private fun route(ctx: Context, action: String, code: Int): PendingIntent {
            return PendingIntent.getBroadcast(
                ctx, code,
                Intent(ctx, MelodiaWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

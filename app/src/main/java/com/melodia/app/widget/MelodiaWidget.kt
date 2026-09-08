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

    companion object {
        @Volatile private var title = "Melodia"
        @Volatile private var artist = "Tu música"
        @Volatile private var playing = false
        @Volatile private var art: Bitmap? = null

        fun refresh(ctx: Context, t: String? = null, a: String? = null, p: Boolean? = null, bitmap: Bitmap? = undefined()) {
            try {
                if (t != null) title = t
                if (a != null) artist = a
                if (p != null) playing = p
                if (bitmap !== UNSET) art = bitmap
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
                val b = art
                if (b != null) {
                    try { rv.setImageViewBitmap(R.id.w_art, b) } catch (_: Exception) {}
                } else {
                    try { rv.setImageViewResource(R.id.w_art, R.drawable.ic_notif_music) } catch (_: Exception) {}
                }
                rv.setOnClickPendingIntent(R.id.w_prev, svc(ctx, MelodiaPlaybackService.ACTION_PREV, 1))
                rv.setOnClickPendingIntent(R.id.w_play, svc(ctx, MelodiaPlaybackService.ACTION_TOGGLE, 2))
                rv.setOnClickPendingIntent(R.id.w_next, svc(ctx, MelodiaPlaybackService.ACTION_NEXT, 3))
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

        private fun svc(ctx: Context, action: String, code: Int): PendingIntent {
            return PendingIntent.getService(
                ctx, code,
                Intent(ctx, MelodiaPlaybackService::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

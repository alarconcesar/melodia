package com.melodia.app

import android.content.Context
import androidx.room.Room
import com.melodia.app.data.MusicRepository
import com.melodia.app.data.PlaylistRepository
import com.melodia.app.data.StatsRepository
import com.melodia.app.data.art.ArtworkRepository
import com.melodia.app.data.local.MelodiaDatabase
import com.melodia.app.data.online.DownloadManager
import com.melodia.app.data.lyrics.LyricsRepository
import com.melodia.app.data.online.YtRepository
import com.melodia.app.player.EqualizerManager
import com.melodia.app.player.MusicPlayer
import com.melodia.app.player.SleepTimer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val musicRepository = MusicRepository(appContext)
    val eqManager = EqualizerManager(appContext)
    // Player diferido: el servicio + ExoPlayer se crean al primer play, no al abrir la app
    val musicPlayer: MusicPlayer by lazy { MusicPlayer(appContext).also { it.eq = eqManager } }
    val db: MelodiaDatabase = buildDb(appContext)

    /** true si la base vieja ya trae el esquema que Room espera. */
    private fun esquemaOk(path: String): Boolean {
        return try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            try {
                fun cols(t: String): Set<String> {
                    val c = db.rawQuery("PRAGMA table_info($t)", null)
                    val out = mutableSetOf<String>()
                    c?.use { while (it.moveToNext()) out += it.getString(1) }
                    return out
                }
                cols("playlists").containsAll(listOf("id", "name", "createdAt", "coverAlbumId", "pinned")) &&
                    cols("playlist_songs").containsAll(listOf("playlistId", "songId", "position", "dataPath", "title", "artist", "album", "durationMs")) &&
                    cols("favorites").containsAll(listOf("songId", "addedAt")) &&
                    cols("stats").containsAll(listOf("songId", "plays", "lastPlayedAt", "listenMs"))
            } finally { try { db.close() } catch (_: Exception) {} }
        } catch (_: Exception) { false }
    }

    private fun buildDb(ctx: Context): MelodiaDatabase {
        // Si la base existente es anterior y ninguna migración la cubre, Room la
        // borraría en silencio. Se respalda el sqlite primero: recuperable.
        try {
            val f = ctx.getDatabasePath("melodia.db")
            if (f.exists()) {
                val sqlite = android.database.sqlite.SQLiteDatabase.openDatabase(
                    f.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                val v = try { sqlite.version } catch (_: Exception) { -1 } finally {
                    try { sqlite.close() } catch (_: Exception) {}
                }
                if (v in 1 until 3) {
                    try {
                        val bak = java.io.File(ctx.filesDir, "melodia-backup-v$v.db")
                        f.inputStream().use { inp -> bak.outputStream().use { inp.copyTo(it) } }
                    } catch (_: Exception) {}
                    // La v1 es de esquema desconocido: si no trae las 4 tablas
                    // con las columnas actuales, se parte de cero (con respaldo)
                    // en vez de reventar al abrir.
                    if (v == 1 && !esquemaOk(f.absolutePath)) {
                        try { f.delete() } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
        return Room.databaseBuilder(ctx, MelodiaDatabase::class.java, "melodia.db")
            .addMigrations(MelodiaDatabase.MIGRATION_1_3, MelodiaDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
    val playlistRepo = PlaylistRepository(db)
    val statsRepo = StatsRepository(db)
    val ytRepo = YtRepository(appContext)
    val dlManager = DownloadManager(appContext, ytRepo)
    val lyricsRepo = LyricsRepository(appContext)
    val artRepo = ArtworkRepository(appContext)
    val queueStore = com.melodia.app.player.QueueStore(appContext)
    val sleepTimer: SleepTimer by lazy {
        lateinit var t: SleepTimer
        t = SleepTimer { try { musicPlayer.fadePause(stillActive = { t.consumeExpire() }) } catch (_: Exception) {} }
        t
    }
    fun makeMainViewModel() = MainViewModel(musicPlayer, musicRepository, playlistRepo, statsRepo, sleepTimer, ytRepo, dlManager, lyricsRepo, artRepo, queueStore)
}

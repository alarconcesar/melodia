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
    val db: MelodiaDatabase = Room.databaseBuilder(appContext, MelodiaDatabase::class.java, "melodia.db").addMigrations(MelodiaDatabase.MIGRATION_2_3).fallbackToDestructiveMigration().build()
    val playlistRepo = PlaylistRepository(db)
    val statsRepo = StatsRepository(db)
    val ytRepo = YtRepository(appContext)
    val dlManager = DownloadManager(appContext, ytRepo)
    val lyricsRepo = LyricsRepository(appContext)
    val artRepo = ArtworkRepository(appContext)
    val sleepTimer = SleepTimer { try { musicPlayer.fadePause() } catch (_: Exception) {} }
    fun makeMainViewModel() = MainViewModel(musicPlayer, musicRepository, playlistRepo, statsRepo, sleepTimer, ytRepo, dlManager, lyricsRepo, artRepo)
}

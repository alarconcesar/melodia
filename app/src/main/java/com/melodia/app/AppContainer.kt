package com.melodia.app

import android.content.Context
import androidx.room.Room
import com.melodia.app.data.MusicRepository
import com.melodia.app.data.PlaylistRepository
import com.melodia.app.data.StatsRepository
import com.melodia.app.data.art.ArtworkRepository
import com.melodia.app.data.local.MelodiaDatabase
import com.melodia.app.data.lyrics.LyricsRepository
import com.melodia.app.data.online.YtRepository
import com.melodia.app.player.EqualizerManager
import com.melodia.app.player.MusicPlayer
import com.melodia.app.player.SleepTimer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val musicRepository = MusicRepository(appContext)
    val eqManager = EqualizerManager(appContext)
    val musicPlayer = MusicPlayer(appContext).also { it.eq = eqManager; it.initialize() }
    val db: MelodiaDatabase = Room.databaseBuilder(appContext, MelodiaDatabase::class.java, "melodia.db").fallbackToDestructiveMigration().build()
    val playlistRepo = PlaylistRepository(db)
    val statsRepo = StatsRepository(db)
    val ytRepo = YtRepository(appContext)
    val lyricsRepo = LyricsRepository(appContext)
    val artRepo = ArtworkRepository(appContext)
    val sleepTimer = SleepTimer { try { musicPlayer.fadePause() } catch (_: Exception) {} }
    fun makeMainViewModel() = MainViewModel(musicPlayer, musicRepository, playlistRepo, statsRepo, sleepTimer, ytRepo, lyricsRepo, artRepo)
}

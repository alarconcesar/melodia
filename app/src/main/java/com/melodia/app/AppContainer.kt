package com.melodia.app

import android.content.Context
import androidx.room.Room
import com.melodia.app.data.MusicRepository
import com.melodia.app.data.PlaylistRepository
import com.melodia.app.data.local.MelodiaDatabase
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
    val sleepTimer = SleepTimer { try { musicPlayer.pauseOnly() } catch (_: Exception) {} }
    fun makeMainViewModel() = MainViewModel(musicPlayer, musicRepository, playlistRepo, sleepTimer)
}

package com.melodia.app.data.local
import androidx.room.Entity
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val position: Int = 0, val dataPath: String = "", val title: String = "", val artist: String = "", val album: String = "", val durationMs: Long = 0L)

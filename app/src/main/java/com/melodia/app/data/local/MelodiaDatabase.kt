package com.melodia.app.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
@Database(entities = [PlaylistEntity::class, PlaylistSongEntity::class, FavoriteEntity::class, StatEntity::class], version = 2, exportSchema = false)
abstract class MelodiaDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun statDao(): StatDao
}

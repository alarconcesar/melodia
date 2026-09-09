package com.melodia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class, FavoriteEntity::class, StatEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MelodiaDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun statDao(): StatDao

    companion object {
        /** v2 → v3: portada elegida + fijar, sin borrar nada. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN coverAlbumId INTEGER")
                db.execSQL("ALTER TABLE playlists ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

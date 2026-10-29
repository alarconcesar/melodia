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
        /** v1 → v3: crea lo que falte sin borrar nada (idempotente). */
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS playlists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL DEFAULT 0, coverAlbumId INTEGER, pinned INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist_songs (playlistId INTEGER NOT NULL, songId INTEGER NOT NULL, position INTEGER NOT NULL DEFAULT 0, dataPath TEXT NOT NULL DEFAULT '', title TEXT NOT NULL DEFAULT '', artist TEXT NOT NULL DEFAULT '', album TEXT NOT NULL DEFAULT '', durationMs INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(playlistId, songId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS favorites (songId INTEGER PRIMARY KEY NOT NULL, addedAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS stats (songId INTEGER PRIMARY KEY NOT NULL, plays INTEGER NOT NULL DEFAULT 0, lastPlayedAt INTEGER NOT NULL DEFAULT 0, listenMs INTEGER NOT NULL DEFAULT 0)")
            }
        }
        /** v2 → v3: portada elegida + fijar, sin borrar nada. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN coverAlbumId INTEGER")
                db.execSQL("ALTER TABLE playlists ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

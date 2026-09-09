package com.melodia.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Portada elegida (albumId de MediaStore). Null = collage automático. */
    val coverAlbumId: Long? = null,
    /** Fijada arriba de la lista. */
    val pinned: Boolean = false
)

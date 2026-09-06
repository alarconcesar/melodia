package com.melodia.app.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long = System.currentTimeMillis())

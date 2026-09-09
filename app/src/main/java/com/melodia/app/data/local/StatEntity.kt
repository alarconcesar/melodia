package com.melodia.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Estadísticas por canción: reproducciones, última vez y minutos aprox. escuchados. */
@Entity(tableName = "stats")
data class StatEntity(
    @PrimaryKey val songId: Long,
    val plays: Int = 0,
    val lastPlayedAt: Long = 0L,
    val listenMs: Long = 0L
)

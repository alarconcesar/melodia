package com.melodia.app.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dataPath: String,
    val dateAdded: Long
)

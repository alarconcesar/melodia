package com.melodia.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dataPath: String,
    val dateAdded: Long,
    /** Número de pista dentro del disco (0 = sin dato). */
    val trackNumber: Int = 0,
    /** Número de disco (1 = primero; 0 = sin dato). */
    val discNumber: Int = 0,
    /** Artista del álbum: mantiene unidos los compilados. */
    val albumArtist: String = "",
    /** URL de carátula online (solo streams de YouTube); null = arte de MediaStore */
    val artUrl: String? = null
)

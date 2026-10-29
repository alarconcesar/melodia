package com.melodia.app.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.melodia.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class MusicRepository(context: Context, private val dlMeta: DownloadMetaStore = DownloadMetaStore(context)) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("melodia_lib", Context.MODE_PRIVATE)

    private val _minSecs = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("min_secs", 30))
    val minSecs: kotlinx.coroutines.flow.StateFlow<Int> = _minSecs
    private val _scanFolder = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("scan_folder", null))
    val scanFolder: kotlinx.coroutines.flow.StateFlow<String?> = _scanFolder

    /** Carpetas que el usuario decidió no escanear (WhatsApp, grabaciones, etc.). */
    private val _excluded = kotlinx.coroutines.flow.MutableStateFlow(
        try { prefs.getStringSet("excluded_folders", emptySet())?.toSet() ?: emptySet() }
        catch (_: Exception) { emptySet() }
    )
    val excludedFolders: kotlinx.coroutines.flow.StateFlow<Set<String>> = _excluded

    fun setExcludedFolders(paths: Set<String>) {
        val limpio = paths.map { it.trim().trimEnd('/') }.filter { it.isNotBlank() }.toSet()
        prefs.edit().putStringSet("excluded_folders", limpio).apply()
        _excluded.value = limpio
    }

    fun isExcluded(path: String): Boolean =
        com.melodia.app.data.LibraryFilters.isExcluded(path, _excluded.value)

    fun setMinSecs(s: Int) {
        val v = s.coerceIn(0, 300)
        prefs.edit().putInt("min_secs", v).apply()
        _minSecs.value = v
    }

    fun setScanFolder(path: String?) {
        if (path == null) prefs.edit().remove("scan_folder").apply()
        else prefs.edit().putString("scan_folder", path).apply()
        _scanFolder.value = path
    }

    /** Resultado de editar metadatos: en API 29+ puede exigir consentimiento. */
    sealed interface MetaResult {
        data object Ok : MetaResult
        data object Fail : MetaResult
        data class NeedConsent(val sender: IntentSender) : MetaResult
    }

    /** Edita título/artista/álbum en MediaStore. */
    fun updateMetadata(id: Long, title: String, artist: String, album: String): MetaResult {
        val uri = android.content.ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
        )
        val cv = android.content.ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, title.trim())
            put(MediaStore.Audio.Media.ARTIST, artist.trim().ifBlank { "<unknown>" })
            put(MediaStore.Audio.Media.ALBUM, album.trim())
        }
        return try {
            if (resolver.update(uri, cv, null, null) > 0) MetaResult.Ok else MetaResult.Fail
        } catch (e: Exception) {
            // En API 29+ editar filas ajenas lanza RecoverableSecurityException:
            // antes se devolvía false y el cambio "no se guardaba" sin más.
            val sender = try {
                if (Build.VERSION.SDK_INT >= 29) {
                    val ex = generateSequence(e as? Throwable) { it.cause }
                        .filterIsInstance<android.app.RecoverableSecurityException>()
                        .firstOrNull()
                    ex?.userAction?.actionIntent?.intentSender
                } else null
            } catch (_: Exception) { null }
            if (sender != null) MetaResult.NeedConsent(sender) else MetaResult.Fail
        }
    }

    /** Guarda la ficha propia de una descarga (manda sobre MediaStore al leer). */
    fun saveDownloadMeta(fileName: String, title: String, artist: String, album: String, artPath: String?) {
        dlMeta.put(fileName, DownloadMetaStore.Meta(title, artist, album, artPath))
    }

    /**
     * Borra audios del dispositivo. En Android 10+ los archivos de otras apps
     * exigen confirmación del sistema: si hace falta, devuelve su IntentSender.
     */
    suspend fun deleteSongs(ids: List<Long>): IntentSender? = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext null
        val uris = ids.map { ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it) }
        // Se borra uno por uno: si un URI lanza, se pide confirmación SOLO por
        // los que faltan (antes se pedía por TODOS, incluidos los ya borrados).
        val pendientes = mutableListOf<android.net.Uri>()
        uris.forEach { u ->
            try {
                resolver.delete(u, null, null)
            } catch (e: Exception) {
                val recoverable = generateSequence(e as? Throwable) { it.cause }
                    .any { it is android.app.RecoverableSecurityException }
                if (recoverable) pendientes += u
            }
        }
        if (pendientes.isEmpty()) return@withContext null
        // createDeleteRequest existe desde API 29 (el gate era >= 30 y en
        // Android 10 devolvía null = "éxito" falso).
        if (Build.VERSION.SDK_INT < 29) return@withContext null
        try { MediaStore.createDeleteRequest(resolver, pendientes).intentSender } catch (_: Exception) { null }
    }

    /** Limpia fichas de descargas cuyos archivos ya no existen. */
    fun sweepDownloadMeta(existingNames: Set<String>) {
        try { dlMeta.sweep(existingNames) } catch (_: Exception) {}
    }

    fun loadAllSongs(): Flow<List<Song>> = flow {
        val songs = withContext(Dispatchers.IO) { querySongs() }
        emit(songs)
    }

    private fun querySongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val minMs = _minSecs.value * 1000L
        val onlyFolder = _scanFolder.value
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK
        )
        // ALBUM_ARTIST existe desde Android 11; en versiones previas se usa el artista
        val hasAlbumArtist = android.os.Build.VERSION.SDK_INT >= 30
        if (hasAlbumArtist) cols.add(MediaStore.Audio.Media.ALBUM_ARTIST)
        // DATA está deprecada y puede venir vacía en scoped storage: con estas
        // dos columnas se reconstruye una ruta legible para Explorar por carpeta.
        val hasRelPath = android.os.Build.VERSION.SDK_INT >= 29
        if (hasRelPath) {
            cols.add(MediaStore.Audio.Media.RELATIVE_PATH)
            cols.add(MediaStore.Audio.Media.DISPLAY_NAME)
        }
        try {
            resolver.query(uri, cols.toTypedArray(), MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " ASC")?.use { c ->
                val iId = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val iData = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val iAdded = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val iTrack = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val iAlbumArtist = if (hasAlbumArtist) c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1
                val iRel = if (hasRelPath) c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1
                val iDisp = if (hasRelPath) c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME) else -1
                while (c.moveToNext()) {
                    val dur = if (iDur >= 0) c.getLong(iDur) else 0L
                    if (dur < minMs) continue
                    var path = if (iData >= 0) c.getString(iData) ?: "" else ""
                    if (path.isBlank() && iRel >= 0) {
                        val rel = try { c.getString(iRel) ?: "" } catch (_: Exception) { "" }
                        val disp = try { c.getString(iDisp) ?: "" } catch (_: Exception) { "" }
                        if (rel.isNotBlank() || disp.isNotBlank()) {
                            path = ("/storage/emulated/0/" + rel.trimEnd('/') + "/" + disp).replace("//", "/")
                        }
                    }
                    if (onlyFolder != null && !path.startsWith(onlyFolder)) continue
                    if (isExcluded(path)) continue
                    var title = if (iTitle >= 0) c.getString(iTitle) ?: "" else ""
                    if (title.isBlank()) {
                        title = try { java.io.File(path).nameWithoutExtension.ifBlank { "Sin título" } } catch (_: Exception) { "Sin título" }
                    }
                    var artist = if (iArtist >= 0) c.getString(iArtist) ?: "" else ""
                    if (artist.isBlank() || artist == "<unknown>") artist = "Desconocido"
                    var album = if (iAlbum >= 0) c.getString(iAlbum) ?: "" else ""
                    if (album == "<unknown>") album = ""
                    // TRACK viene como disc*1000 + pista cuando hay varios discos
                    val rawTrack = if (iTrack >= 0) c.getInt(iTrack) else 0
                    val disc = if (rawTrack >= 1000) rawTrack / 1000 else 1
                    val track = when {
                        rawTrack <= 0 -> 0
                        rawTrack >= 1000 -> rawTrack % 1000
                        else -> rawTrack
                    }
                    val albumArtist = if (iAlbumArtist >= 0) {
                        (c.getString(iAlbumArtist) ?: "").takeUnless { it.isBlank() || it == "<unknown>" } ?: artist
                    } else artist
                    songs.add(
                        Song(
                            id = if (iId >= 0) c.getLong(iId) else 0L,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = if (iAlbumId >= 0) c.getLong(iAlbumId) else 0L,
                            durationMs = dur,
                            dataPath = if (iData >= 0) c.getString(iData) ?: "" else "",
                            dateAdded = if (iAdded >= 0) c.getLong(iAdded) else 0L,
                            trackNumber = track,
                            discNumber = if (rawTrack > 0) disc else 0,
                            albumArtist = albumArtist
                        ).let { base ->
                            // Lo descargado manda: MediaStore puede perder el
                            // artista/álbum al reescribir las etiquetas del m4a.
                            val nombre = try { java.io.File(path).name } catch (_: Exception) { "" }
                            val m = if (nombre.isNotBlank()) dlMeta.get(nombre) else null
                            if (m == null) base else base.copy(
                                title = m.title.ifBlank { base.title },
                                artist = m.artist.ifBlank { base.artist },
                                album = m.album.ifBlank { base.album },
                                artUrl = m.artPath ?: base.artUrl
                            )
                        }
                    )
                }
            }
        } catch (_: Exception) { }
        return songs
    }
}

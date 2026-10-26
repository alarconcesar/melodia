package com.melodia.app.player

import android.content.Context
import com.melodia.app.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

/** Cola lista para reconstruir tras muerte del proceso o reapertura. */
data class QueueSnapshot(
    val songs: List<Song>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int
)

/** Plan de restauración: cola mapeada contra la biblioteca actual. */
data class RestorePlan(val songs: List<Song>, val index: Int, val positionMs: Long)

object QueueRestore {
    /**
     * Mapea el snapshot contra la biblioteca actual: descarta temas borrados,
     * recoloca el índice sobre el mismo tema y solo conserva la posición si el
     * tema actual sigue existiendo.
     */
    fun plan(snapshot: QueueSnapshot?, library: List<Song>): RestorePlan? {
        if (snapshot == null || library.isEmpty()) return null
        val byId = library.associateBy { it.id }
        val songs = snapshot.songs.mapNotNull { byId[it.id] }
        if (songs.isEmpty()) return null
        val wanted = snapshot.songs.getOrNull(snapshot.index)?.id
        val idx = songs.indexOfFirst { it.id == wanted }.takeIf { it >= 0 } ?: 0
        val position = if (wanted != null && songs[idx].id == wanted) snapshot.positionMs else 0L
        return RestorePlan(songs, idx, position)
    }
}

/**
 * Persiste la cola en SharedPreferences (JSON).
 *
 * - El ViewModel la guarda cuando cambia la cola o el tema actual (una escritura
 *   por cambio, no por tick) y actualiza solo la posición cada 5 s.
 * - El servicio la lee en `onPlaybackResumption` para que el sistema (Auto,
 *   Bluetooth, reanudación tras reinicio) retome donde iba.
 *
 * Es a propósito un archivo plano y no Room: la cola es un snapshot efímero y
 * así el servicio puede leerla sin arrastrar la base de datos completa.
 */
class QueueStore(context: Context) {

    private val prefs = context.getSharedPreferences("melodia_queue", Context.MODE_PRIVATE)

    fun save(songs: List<Song>, index: Int, positionMs: Long, shuffle: Boolean, repeatMode: Int) {
        saveSongs(songs)
        saveState(index, positionMs, shuffle, repeatMode)
    }

    /**
     * Guarda SOLO la lista (JSON). Es la escritura cara: se hace cuando cambia
     * la composición de la cola, no en cada cambio de tema.
     */
    fun saveSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        try {
            val arr = JSONArray()
            songs.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("t", s.title)
                        .put("a", s.artist)
                        .put("al", s.album)
                        .put("ai", s.albumId)
                        .put("d", s.durationMs)
                        .put("p", s.dataPath)
                        .put("ad", s.dateAdded)
                        .put("tn", s.trackNumber)
                        .put("dn", s.discNumber)
                        .put("aa", s.albumArtist)
                        .put("art", s.artUrl ?: "")
                )
            }
            prefs.edit().putString(KEY_SONGS, arr.toString()).apply()
        } catch (_: Exception) { }
    }

    /** Índice/posición/shuffle/repeat: claves sueltas, baratas de escribir. */
    fun saveState(index: Int, positionMs: Long, shuffle: Boolean, repeatMode: Int) {
        try {
            prefs.edit()
                .putInt(KEY_INDEX, index)
                .putLong(KEY_POS, positionMs.coerceAtLeast(0L))
                .putBoolean(KEY_SHUFFLE, shuffle)
                .putInt(KEY_REPEAT, repeatMode)
                .apply()
        } catch (_: Exception) { }
    }

    /** Actualización barata: solo la posición (se llama cada pocos segundos). */
    fun savePosition(positionMs: Long) {
        try { prefs.edit().putLong(KEY_POS, positionMs.coerceAtLeast(0L)).apply() } catch (_: Exception) {}
    }

    fun load(): QueueSnapshot? {
        return try {
            val raw = prefs.getString(KEY_SONGS, null) ?: return null
            val arr = JSONArray(raw)
            if (arr.length() == 0) return null
            val songs = (0 until arr.length()).mapNotNull { i ->
                try {
                    val o = arr.getJSONObject(i)
                    Song(
                        id = o.getLong("id"),
                        title = o.optString("t"),
                        artist = o.optString("a"),
                        album = o.optString("al"),
                        albumId = o.optLong("ai"),
                        durationMs = o.optLong("d"),
                        dataPath = o.optString("p"),
                        dateAdded = o.optLong("ad"),
                        trackNumber = o.optInt("tn"),
                        discNumber = o.optInt("dn"),
                        albumArtist = o.optString("aa"),
                        artUrl = o.optString("art").ifBlank { null }
                    )
                } catch (_: Exception) { null }
            }
            if (songs.isEmpty()) return null
            QueueSnapshot(
                songs = songs,
                index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, songs.size - 1),
                positionMs = prefs.getLong(KEY_POS, 0L).coerceAtLeast(0L),
                shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
                repeatMode = prefs.getInt(KEY_REPEAT, 0)
            )
        } catch (_: Exception) { null }
    }

    fun clear() { try { prefs.edit().clear().apply() } catch (_: Exception) {} }

    private companion object {
        const val KEY_SONGS = "songs"
        const val KEY_INDEX = "idx"
        const val KEY_POS = "pos"
        const val KEY_SHUFFLE = "sh"
        const val KEY_REPEAT = "rep"
    }
}

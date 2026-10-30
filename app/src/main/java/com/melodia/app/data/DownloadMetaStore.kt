package com.melodia.app.data

import android.content.Context
import org.json.JSONObject

/**
 * Metadatos de lo que descarga la app, guardados por nombre de archivo.
 *
 * MediaStore no siempre conserva lo que le pasamos al insertar (y al reescribir
 * las etiquetas del m4a puede volver a escanear y dejar el artista vacío). Como
 * nosotros sabemos exactamente qué bajamos, esta copia manda al leer la
 * biblioteca: título, artista, álbum y ruta de la carátula.
 */
class DownloadMetaStore(context: Context) {

    private val prefs = context.getSharedPreferences("melodia_dl_meta", Context.MODE_PRIVATE)

    data class Meta(val title: String, val artist: String, val album: String, val artPath: String?)

    fun put(fileName: String, meta: Meta) {
        try {
            val o = JSONObject()
                .put("t", meta.title)
                .put("a", meta.artist)
                .put("al", meta.album)
                .put("art", meta.artPath ?: "")
            prefs.edit().putString(fileName, o.toString()).apply()
        } catch (_: Exception) {}
    }

    /** Borra las fichas de archivos que ya no están (evita prefs eternas). */
    fun sweep(existingNames: Set<String>) {
        try {
            val ed = prefs.edit()
            var n = 0
            for (k in prefs.all.keys) {
                if (!existingNames.contains(k)) { ed.remove(k); n++ }
            }
            if (n > 0) ed.apply()
        } catch (_: Exception) {}
    }

    fun get(fileName: String): Meta? {
        return try {
            val raw = prefs.getString(fileName, null) ?: return null
            val o = JSONObject(raw)
            Meta(
                title = o.optString("t").ifBlank { fileName.substringBeforeLast('.') },
                artist = o.optString("a").ifBlank { "YouTube" },
                album = o.optString("al"),
                artPath = o.optString("art").ifBlank { null }
            )
        } catch (_: Exception) { null }
    }
}

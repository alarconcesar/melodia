package com.melodia.app.data.art

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Normaliza un título para comparar ("Tema (Official Video)" → "tema").
 * Quita paréntesis/corchetes, coletillas típicas y puntuación.
 */
fun normTitle(t: String): String {
    var s = t.lowercase()
    s = s.replace(Regex("\\s*[\\[(].*?[\\])]"), " ")
    s = s.replace(Regex("\\b(official|video|audio|lyric|lyrics|music|clip|hq|hd|visualizer|cover|remastered|remaster|version)\\b"), " ")
    s = s.replace(Regex("[^a-z0-9áéíóúñü ]"), " ")
    return s.replace(Regex("\\s+"), " ").trim()
}

/** true si los títulos hablan del mismo tema (igualdad o contención sana). */
fun titlesMatch(a: String, b: String): Boolean {
    val x = normTitle(a)
    val y = normTitle(b)
    if (x.isBlank() || y.isBlank()) return false
    if (x == y) return true
    val (corto, largo) = if (x.length <= y.length) x to y else y to x
    return corto.length >= 4 && largo.contains(corto) && corto.length.toFloat() / largo.length >= 0.5f
}

/** Carátulas de iTunes Search API (sin key) para temas sin arte local. */
class ArtworkRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("melodia_art", Context.MODE_PRIVATE)
    private val mem = mutableMapOf<String, String?>()

    private fun key(artist: String, title: String) =
        "${artist.trim().lowercase()}|${title.trim().lowercase()}"

    fun cached(artist: String, title: String): String? {
        val k = key(artist, title)
        mem[k]?.let { return it }
        return try {
            if (!prefs.contains(k)) null
            else prefs.getString(k, null)?.ifBlank { null }.also { mem[k] = it }
        } catch (_: Exception) { null }
    }

    /**
     * Artista canónico vía iTunes Search (sin key): se acepta solo si el título
     * coincide ([titlesMatch]) y, con pista del canal, el artista encaja con
     * ella. Sin match seguro se devuelve null (mejor "YouTube" que inventar).
     */
    suspend fun fetchArtist(title: String, hint: String): String? = withContext(Dispatchers.IO) {
        try {
            val h = hint.trim()
            val q = if (h.isBlank() || h.equals("YouTube", true)) title else "$title $h"
            val url = URL(
                "https://itunes.apple.com/search?term=${URLEncoder.encode(q, "UTF-8")}" +
                    "&media=music&entity=song&limit=5"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("User-Agent", "Melodia/1.0")
            }
            try {
                if (conn.responseCode != 200) return@withContext null
                val arr = JSONObject(conn.inputStream.bufferedReader().readText())
                    .optJSONArray("results") ?: return@withContext null
                val hn = h.lowercase()
                var fallback: String? = null
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val itunesTitle = o.optString("trackName", "")
                    val itunesArtist = o.optString("artistName", "").trim()
                    if (itunesArtist.isBlank() || !titlesMatch(title, itunesTitle)) continue
                    if (fallback == null) fallback = itunesArtist
                    // Con pista del canal se exige que encaje (en ambos sentidos).
                    if (hn.isBlank() || hn == "youtube") return@withContext itunesArtist
                    val an = itunesArtist.lowercase()
                    if (an.contains(hn) || hn.contains(an)) return@withContext itunesArtist
                }
                // Sin pista útil, el primer título coincidente vale.
                if ((h.isBlank() || h.equals("YouTube", true))) fallback else null
            } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
        } catch (_: Exception) { null }
    }

    suspend fun fetch(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val k = key(artist, title)
        mem[k]?.let { return@withContext it }
        try {
            if (prefs.contains(k)) {
                val c = prefs.getString(k, null)?.ifBlank { null }
                mem[k] = c
                return@withContext c
            }
            val q = URLEncoder.encode("$artist $title".trim(), "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("User-Agent", "Melodia/1.0")
            }
            try {
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().readText()
                val arr = JSONObject(body).optJSONArray("results") ?: return@withContext null
                if (arr.length() == 0) {
                    mem[k] = null
                    prefs.edit().putString(k, "").apply()
                    return@withContext null
                }
                var art = arr.getJSONObject(0).optString("artworkUrl100", "")
                if (art.isBlank()) return@withContext null
                art = art.replace("100x100bb", "600x600bb")
                mem[k] = art
                try {
                    prefs.edit().putString(k, art).apply()
                    // tope aprox. de caché: si crece mucho, se limpia y reaprende
                    if (prefs.all.size > 400) prefs.edit().clear().putString(k, art).apply()
                } catch (_: Exception) {}
                art
            } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d("Artwork", "fetch fail: ${e.message}")
            null
        }
    }

    /**
     * Guarda la miniatura de una descarga como carátula local del tema.
     * Queda en la misma caché que [cached] (clave artista|título), así el
     * escaneo de biblioteca la encuentra sin depender del etiquetado del m4a.
     */
    suspend fun saveLocal(artist: String, title: String, bytes: ByteArray?): String? =
        withContext(Dispatchers.IO) {
            if (bytes == null || bytes.isEmpty()) return@withContext null
            val k = key(artist, title)
            try {
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null
                val scaled = scaleDown(bmp, 640)
                val out = java.io.File(artDir, "dl-${k.hashCode()}.jpg")
                out.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
                if (scaled !== bmp) bmp.recycle()
                val path = "file://${out.absolutePath}"
                mem[k] = path
                prefs.edit().putString(k, path).apply()
                trimCache()
                path
            } catch (_: Exception) {
                null
            }
        }

    // ── Carátula embebida del archivo (offline) ─────────────────────
    private val artDir = java.io.File(context.filesDir, "art").apply { mkdirs() }

    /**
     * Extrae la portada incrustada del audio a un JPEG local y devuelve su
     * `file://`. Evita depender de iTunes (red) cuando el archivo ya trae arte.
     * Cachea también el "no tiene" para no reintentar en cada arranque.
     */
    suspend fun embedded(song: com.melodia.app.data.model.Song): String? = withContext(Dispatchers.IO) {
        val k = "emb:${song.id}:${song.dataPath.hashCode()}"
        try {
            if (prefs.contains(k)) return@withContext prefs.getString(k, null)?.ifBlank { null }
            val out = java.io.File(artDir, "s${song.id}.jpg")
            if (!out.exists()) {
                val bmp = extractEmbedded(song.dataPath) ?: run {
                    prefs.edit().putString(k, "").apply()
                    return@withContext null
                }
                val scaled = scaleDown(bmp, 640)
                out.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
                if (scaled !== bmp) bmp.recycle()
                trimCache()
            }
            val path = "file://${out.absolutePath}"
            prefs.edit().putString(k, path).apply()
            path
        } catch (_: Exception) {
            prefs.edit().putString(k, "").apply()
            null
        }
    }

    private fun extractEmbedded(path: String): android.graphics.Bitmap? {
        if (path.isBlank()) return null
        val r = android.media.MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
            val bytes = r.embeddedPicture ?: return null
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    private fun scaleDown(bmp: android.graphics.Bitmap, max: Int): android.graphics.Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= max && h <= max) return bmp
        val ratio = max.toFloat() / maxOf(w, h)
        return android.graphics.Bitmap.createScaledBitmap(
            bmp, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true
        )
    }

    /** Tope de disco: ~600 portadas; se borran las más viejas. */
    private fun trimCache() {
        try {
            val files = artDir.listFiles() ?: return
            if (files.size > 600) {
                files.sortedBy { it.lastModified() }.take(files.size - 600).forEach { it.delete() }
            }
            // Las claves emb: guardan file:// y la poda las dejaba colgadas:
            // embedded() devolvía rutas de JPG borrados (arte roto permanente).
            val alive = artDir.listFiles()?.map { it.absolutePath }?.toSet() ?: emptySet()
            prefs.all.forEach { (k, v) ->
                val path = (v as? String) ?: return@forEach
                if (path.startsWith("file://")) {
                    val abs = try { java.io.File(java.net.URI(path).path ?: "").absolutePath } catch (_: Exception) { "" }
                    if (abs.isNotBlank() && !alive.contains(abs) && abs.contains(artDir.absolutePath)) {
                        try { prefs.edit().remove(k).apply() } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }
}

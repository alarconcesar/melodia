package com.melodia.app.data.art

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Carátulas de iTunes Search API (sin key) para temas sin arte local. */
class ArtworkRepository(context: Context) {
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
}

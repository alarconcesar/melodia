package com.melodia.app.data.lyrics

import android.content.Context
import com.melodia.app.data.art.titlesMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val ms: Long, val text: String)
data class Lyrics(val synced: List<LyricLine>, val plain: String?, val instrumental: Boolean = false)

/** Letras sincronizadas vía LRCLIB (gratis, sin key) + caché en disco. */
class LyricsRepository(context: Context) {
    private val dir = File(context.cacheDir, "lyrics").apply { mkdirs() }

    suspend fun get(artist: String, title: String, album: String, durationSec: Long, key: String): Lyrics? =
        withContext(Dispatchers.IO) {
            try {
                readCache(key)?.let { return@withContext it }
                // /get exige artist_name real: con artista basura se va directo a /search
                val found = (if (!isJunkArtist(artist)) fetchGet(artist, title, album, durationSec) else null)
                    ?: fetchSearch(artist, title, durationSec)
                if (found != null) writeCache(key, found)
                found
            } catch (_: Exception) { readCache(key) }
        }

    // ── API ──
    private fun getJson(url: String): JSONObject? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("User-Agent", "Melodia/1.0")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            conn.connect()
            if (conn.responseCode != 200) return null
            JSONObject(conn.inputStream.bufferedReader().readText())
        } catch (_: Exception) { null } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun isJunkArtist(a: String) =
        a.isBlank() || a.equals("Desconocido", true) || a.equals("YouTube", true) || a == "<unknown>"

    private fun isJunkAlbum(a: String) =
        a.isBlank() || a.equals("Online", true) || a.equals("Sencillo", true) || a.equals("Sin álbum", true)

    private fun fetchGet(artist: String, title: String, album: String, dur: Long): Lyrics? {
        // Los filtros basura ("Desconocido", "Online") hunden el match: se omiten
        val params = mutableListOf("track_name=${enc(title)}", "duration=$dur")
        if (!isJunkArtist(artist)) params += "artist_name=${enc(artist)}"
        if (!isJunkAlbum(album)) params += "album_name=${enc(album)}"
        val o = getJson("https://lrclib.net/api/get?${params.joinToString("&")}") ?: return null
        return fromJson(o)
    }

    private fun fetchSearch(artist: String, title: String, dur: Long): Lyrics? {
        val q = if (isJunkArtist(artist)) title else "$title $artist"
        val url = "https://lrclib.net/api/search?q=${enc(q)}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("User-Agent", "Melodia/1.0")
        }
        return try {
            conn.connect()
            if (conn.responseCode != 200) return tryClean(artist, title, dur)
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            if (arr.length() == 0) return tryClean(artist, title, dur)
            // mejor match por duración (±10s), si no el primero
            var best: JSONObject? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val d = try { o.optLong("duration", -1) } catch (_: Exception) { -1L }
                if (dur > 0 && d > 0 && kotlin.math.abs(d - dur) <= 10) { best = o; break }
                if (best == null) best = o
            }
            // Sin duración no hay forma de desempatar: el primero solo vale si
            // el título coincide (si no, era la letra de otro tema).
            if (dur <= 0 && best != null) {
                val bt = try { best.optString("trackName", "") } catch (_: Exception) { "" }
                if (!titlesMatch(bt, title)) best = null
            }
            (best?.let { fromJson(it) }) ?: tryClean(artist, title, dur)
        } catch (_: Exception) { tryClean(artist, title, dur) } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    /** Reintento con título limpio ("X (Official Video)" → "X"). */
    private fun tryClean(artist: String, title: String, dur: Long): Lyrics? {
        val clean = cleanTitle(title)
        if (clean == title) return null
        val q = if (isJunkArtist(artist)) clean else "$clean $artist"
        val url = "https://lrclib.net/api/search?q=${enc(q)}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("User-Agent", "Melodia/1.0")
        }
        return try {
            conn.connect()
            if (conn.responseCode != 200) return null
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            if (arr.length() == 0) null else fromJson(arr.getJSONObject(0))
        } catch (_: Exception) { null } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    private fun cleanTitle(t: String): String {
        var s = t
        s = s.replace(Regex("""\s*[\[(].*(official|video|audio|lyric|lyrics|music|clip|hq|hd|visualizer|cover).*[\])]""", RegexOption.IGNORE_CASE), "")
        return s.trim().ifBlank { t }
    }

    private fun fromJson(o: JSONObject): Lyrics? {
        return try {
            val instrumental = o.optBoolean("instrumental", false)
            val syncedRaw = o.optString("syncedLyrics", "")
            val plain = o.optString("plainLyrics", "").ifBlank { null }
            val synced = if (syncedRaw.isNotBlank()) parseLrc(syncedRaw) else emptyList()
            if (synced.isEmpty() && plain == null && !instrumental) null
            else Lyrics(synced, plain, instrumental)
        } catch (_: Exception) { null }
    }

    // ── Caché ──
    private fun cacheFile(key: String) = File(dir, "${key.hashCode()}.json")

    private fun readCache(key: String): Lyrics? {
        return try {
            val f = cacheFile(key)
            if (!f.exists()) return null
            val o = JSONObject(f.readText())
            val arr = o.optJSONArray("synced") ?: JSONArray()
            val lines = mutableListOf<LyricLine>()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                lines += LyricLine(e.getLong("ms"), e.getString("t"))
            }
            Lyrics(lines, o.optString("plain", "").ifBlank { null }, o.optBoolean("inst", false))
        } catch (_: Exception) { null }
    }

    private fun writeCache(key: String, l: Lyrics) {
        try {
            val arr = JSONArray()
            l.synced.take(600).forEach { arr.put(JSONObject().put("ms", it.ms).put("t", it.text)) }
            cacheFile(key).writeText(
                JSONObject().put("synced", arr).put("plain", l.plain ?: "").put("inst", l.instrumental).toString()
            )
            trimCache()
        } catch (_: Exception) {}
    }

    /** Tope de disco: ~300 letras; se borran las más viejas. */
    private fun trimCache() {
        try {
            val files = dir.listFiles() ?: return
            if (files.size <= 300) return
            files.sortedBy { it.lastModified() }.take(files.size - 300).forEach { f ->
                try { f.delete() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    companion object {
        /** Parser LRC: [mm:ss.xx], multi-timestamp, offset y word-timings <mm:ss.xx>. */
        fun parseLrc(raw: String): List<LyricLine> {
            var offset = 0L
            val out = mutableListOf<LyricLine>()
            val tagRe = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")
            val wordRe = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")
            for (rawLine in raw.lines()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                if (line.startsWith("[offset:", true)) {
                    offset = line.substringAfter(":").substringBefore("]").trim().toLongOrNull() ?: 0L
                    continue
                }
                if (line.startsWith("[") && line.getOrNull(1)?.isDigit() != true) continue
                val tags = tagRe.findAll(line).toList()
                if (tags.isEmpty()) continue
                val text = wordRe.replace(tagRe.replace(line, ""), "").trim()
                if (text.isEmpty()) continue
                for (t in tags) {
                    val min = t.groupValues[1].toLongOrNull() ?: continue
                    val sec = t.groupValues[2].toLongOrNull() ?: continue
                    val frac = t.groupValues[3]
                    val ms = when {
                        frac.isEmpty() -> 0L
                        frac.length == 2 -> (frac.toLongOrNull() ?: 0L) * 10
                        else -> frac.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
                    }
                    out += LyricLine(min * 60_000 + sec * 1_000 + ms + offset, text)
                }
            }
            return out.sortedBy { it.ms }
        }
    }
}

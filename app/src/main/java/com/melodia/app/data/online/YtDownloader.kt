package com.melodia.app.data.online

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL

private const val UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

/** Downloader de NewPipeExtractor sobre HttpURLConnection (sin librerías extra). */
class YtDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val conn = URL(request.url()).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = request.httpMethod()
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", UA)
            try {
                request.headers()?.forEach { (k, v) ->
                    if (!k.equals("User-Agent", true)) conn.setRequestProperty(k, v.joinToString(";"))
                }
            } catch (_: Exception) {}
            val data = request.dataToSend()
            if (data != null && data.isNotEmpty()) {
                conn.doOutput = true
                conn.outputStream.use { it.write(data) }
            }
            val code = conn.responseCode
            val body = try {
                (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText() ?: ""
            } catch (_: Exception) { "" }
            if (code == 429) throw org.schabi.newpipe.extractor.exceptions.ReCaptchaException("rate-limited", request.url())
            val headers = try {
                conn.headerFields.filterKeys { it != null }.mapKeys { it.key!! }
            } catch (_: Exception) { emptyMap() }
            return Response(code, conn.responseMessage, headers, body, request.url())
        } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }
}

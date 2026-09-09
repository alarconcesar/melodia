package com.melodia.app.data.online

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Etiquetado mínimo de archivos M4A (contenedor MP4): título, artista, álbum,
 * letra (©lyr) y carátula (covr) dentro de moov > udta > meta > ilst.
 * Sin dependencias: parsea y reescribe las cajas, ajustando stco/co64
 * cuando moov va antes de mdat. Si el archivo no es el esperado,
 * lanza excepción y quien llama conserva la descarga sin etiquetar.
 */
object Mp4Tagger {

    private class Box(val type: String, val contentStart: Int, val contentEnd: Int, val headerSize: Int) {
        val start = contentStart - headerSize
        val size = contentEnd - start
    }

    private fun readBox(data: ByteArray, offset: Int, end: Int): Box? {
        if (offset + 8 > end || offset + 8 > data.size) return null
        var size = ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
        var header = 8
        if (size == 1) {
            if (offset + 16 > end) return null
            val bb = ByteBuffer.wrap(data, offset + 8, 8).order(ByteOrder.BIG_ENDIAN)
            val big = bb.long
            if (big > Int.MAX_VALUE || big < 16) return null
            size = big.toInt()
            header = 16
        }
        if (size < 8 || offset + size > end || offset + size > data.size) return null
        // ISO-8859-1: mapea bytes 1:1 (0xA9 de ©nam/©ART se conserva)
        val type = String(data, offset + 4, 4, Charsets.ISO_8859_1)
        return Box(type, offset + header, offset + size, header)
    }

    private fun childrenOf(data: ByteArray, box: Box): List<Box> {
        val out = mutableListOf<Box>()
        var p = box.contentStart
        // meta tiene 4 bytes de versión/flags antes de sus hijas
        if (box.type == "meta") p += 4
        var guard = 0
        while (p < box.contentEnd && guard++ < 512) {
            val b = readBox(data, p, box.contentEnd) ?: break
            out.add(b)
            p = b.contentEnd
        }
        return out
    }

    private fun be32(v: Int): ByteArray =
        byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun box(type: String, vararg payloads: ByteArray): ByteArray {
        val total = 8 + payloads.sumOf { it.size }
        val bb = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN)
        bb.putInt(total)
        bb.put(type.toByteArray(Charsets.US_ASCII), 0, 4)
        payloads.forEach { bb.put(it) }
        return bb.array()
    }

    /** Caja 'data': tipo 1 = texto UTF-8, 13 = JPEG, 14 = PNG. */
    private fun dataBox(payload: ByteArray, kind: Int): ByteArray =
        box("data", be32(kind), be32(0), payload)

    private fun textItem(fourcc: String, text: String): ByteArray =
        box(fourcc, dataBox(text.toByteArray(Charsets.UTF_8), 1))

    private fun isImage(bytes: ByteArray, mime: String?): Int {
        if (mime?.contains("png", true) == true) return 14
        if (mime?.contains("jpeg", true) == true || mime?.contains("jpg", true) == true) return 13
        if (bytes.size > 3 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) return 14
        return 13 // JPEG por defecto (miniatura de YouTube)
    }

    /**
     * Devuelve el archivo completo con las etiquetas aplicadas.
     * Conserva los ítems ilst existentes salvo los que reemplaza.
     */
    fun tag(
        data: ByteArray,
        title: String,
        artist: String,
        album: String? = null,
        lyrics: String? = null,
        artBytes: ByteArray? = null,
        artMime: String? = null
    ): ByteArray {
        val moovBox = findTopLevel(data, "moov") ?: error("sin moov")
        val moovKids = childrenOf(data, moovBox)
        val udta = moovKids.firstOrNull { it.type == "udta" }

        // 1. Construir el nuevo contenido de ilst
        val existing = mutableListOf<ByteArray>()
        var oldIlst: Box? = null
        var oldMeta: Box? = null
        if (udta != null) {
            val meta = childrenOf(data, udta).firstOrNull { it.type == "meta" }
            if (meta != null) {
                oldMeta = meta
                oldIlst = childrenOf(data, meta).firstOrNull { it.type == "ilst" }
                if (oldIlst != null) {
                    for (item in childrenOf(data, oldIlst)) {
                        if (item.type == "\u00A9nam" || item.type == "\u00A9ART" ||
                            item.type == "\u00A9alb" || item.type == "covr" ||
                            item.type == "\u00A9lyr" || item.type == "day"
                        ) continue
                        existing.add(data.copyOfRange(item.start, item.contentEnd))
                    }
                }
            }
        }
        val fresh = mutableListOf<ByteArray>()
        if (title.isNotBlank()) fresh.add(textItem("\u00A9nam", title))
        if (artist.isNotBlank()) fresh.add(textItem("\u00A9ART", artist))
        if (!album.isNullOrBlank()) fresh.add(textItem("\u00A9alb", album))
        val lyricText = lyrics?.take(60000)
        if (!lyricText.isNullOrBlank()) fresh.add(textItem("\u00A9lyr", lyricText))
        if (artBytes != null && artBytes.size in 1..4_000_000) {
            fresh.add(box("covr", dataBox(artBytes, isImage(artBytes, artMime))))
        }
        if (existing.isEmpty() && fresh.isEmpty()) error("nada que etiquetar")
        val ilstPayload = (existing + fresh).fold(byteArrayOf()) { acc, b -> acc + b }
        val ilstBox = box("ilst", ilstPayload)

        // 2. Nueva cadena udta > meta > ilst (reutiliza hdlr si existe)
        val hdlr = oldMeta?.let { childrenOf(data, it).firstOrNull { c -> c.type == "hdlr" } }
            ?.let { data.copyOfRange(it.start, it.contentEnd) }
            ?: box("hdlr", be32(0), "mdirappl".toByteArray(Charsets.US_ASCII),
                be32(0), be32(0), be32(0), byteArrayOf(0))
        val metaBox = box("meta", be32(0), hdlr, ilstBox)
        val udtaBox = box("udta", metaBox)

        // 3. Reemplazar o añadir udta dentro de moov
        val moovContent = if (udta != null) {
            val head = data.copyOfRange(moovBox.contentStart, udta.start)
            val tail = data.copyOfRange(udta.contentEnd, moovBox.contentEnd)
            head + udtaBox + tail
        } else {
            data.copyOfRange(moovBox.contentStart, moovBox.contentEnd) + udtaBox
        }
        val newMoovLen = moovBox.headerSize + moovContent.size
        val newMoov = be32(newMoovLen) + "moov".toByteArray(Charsets.US_ASCII) +
            (if (moovBox.headerSize == 16) data.copyOfRange(moovBox.start + 8, moovBox.contentStart) else byteArrayOf()) +
            moovContent
        val delta = newMoov.size - moovBox.size

        var out = data.copyOfRange(0, moovBox.start) + newMoov +
            data.copyOfRange(moovBox.contentEnd, data.size)

        // 4. Si moov quedó antes de mdat, los offsets de audio se movieron: ajustar stco/co64
        val mdatPos = findTopLevel(out, "mdat")?.start ?: -1
        if (delta != 0 && mdatPos > moovBox.start) {
            val newMoovBox = findTopLevel(out, "moov") ?: error("moov perdido")
            for (trak in childrenOf(out, newMoovBox).filter { it.type == "trak" }) {
                for (stbl in deepFind(out, trak, listOf("mdia", "minf", "stbl"))) {
                    for (off in childrenOf(out, stbl).filter { it.type == "stco" || it.type == "co64" }) {
                        val entryOff = off.contentStart + 4
                        val count = ByteBuffer.wrap(out, entryOff, 4).order(ByteOrder.BIG_ENDIAN).int
                        if (count < 0 || count > 100000) continue
                        if (off.type == "stco") {
                            for (i in 0 until count) {
                                val p = entryOff + 4 + i * 4
                                val v = ByteBuffer.wrap(out, p, 4).order(ByteOrder.BIG_ENDIAN).int
                                ByteBuffer.wrap(out, p, 4).order(ByteOrder.BIG_ENDIAN).putInt(v + delta)
                            }
                        } else {
                            for (i in 0 until count) {
                                val p = entryOff + 4 + i * 8
                                val v = ByteBuffer.wrap(out, p, 8).order(ByteOrder.BIG_ENDIAN).long
                                ByteBuffer.wrap(out, p, 8).order(ByteOrder.BIG_ENDIAN).putLong(v + delta)
                            }
                        }
                    }
                }
            }
        }
        // 5. Autochequeo: el resultado debe re-parsearse y contener ilst
        val check = findTopLevel(out, "moov") ?: error("verificación fallida")
        require(check.size > 0) { "verificación fallida" }
        return out
    }

    private fun findTopLevel(data: ByteArray, type: String): Box? {
        var p = 0
        var guard = 0
        while (p < data.size && guard++ < 64) {
            val b = readBox(data, p, data.size) ?: break
            if (b.type == type) return b
            p = b.contentEnd
        }
        return null
    }

    private fun deepFind(data: ByteArray, root: Box, path: List<String>): List<Box> {
        var cur = listOf(root)
        for (t in path) cur = cur.flatMap { childrenOf(data, it) }.filter { it.type == t }
        return cur
    }
}

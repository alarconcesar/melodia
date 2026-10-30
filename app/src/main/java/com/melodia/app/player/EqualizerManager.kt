package com.melodia.app.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EqualizerManager(context: Context) {
    private val prefs = context.getSharedPreferences("melodia_eq", Context.MODE_PRIVATE)
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var reverb: PresetReverb? = null
    private var sessionId: Int = 0

    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
    val enabled: StateFlow<Boolean> = _enabled
    private val _bands = MutableStateFlow(List(5) { i -> prefs.getInt("band$i", 0) })
    val bands: StateFlow<List<Int>> = _bands
    private val _bass = MutableStateFlow(prefs.getInt("bass", 0))
    val bassLevel: StateFlow<Int> = _bass
    private val _preset = MutableStateFlow(prefs.getString("preset", "Personalizado") ?: "Personalizado")
    val preset: StateFlow<String> = _preset
    val presets: List<String> = try {
        val tmp = Equalizer(0, 0); val n = tmp.numberOfPresets.toInt(); val l = (0 until n).map { tmp.getPresetName(it.toShort()) }; tmp.release(); l
    } catch (_: Exception) { listOf("Normal", "Rock", "Pop", "Jazz", "Clásica", "Dance", "Hip Hop", "Electrónica") }

    fun attach(newSession: Int) {
        if (newSession == 0 || newSession == sessionId) return
        try {
            releaseFx()
            eq = Equalizer(0, newSession).also { e ->
                e.enabled = _enabled.value
                val nb = e.numberOfBands.coerceAtMost(5).toInt()
                for (i in 0 until nb) {
                    try { e.setBandLevel(i.toShort(), _bands.value.getOrElse(i) { 0 }.coerceIn(-1500, 1500).toShort()) } catch (_: Exception) {}
                }
                // Las bandas que la app no maneja (6+) se dejan en flat: si no,
                // conservan el preset del sistema y suenan sin que la UI lo muestre.
                try {
                    for (i in nb until e.numberOfBands) {
                        try { e.setBandLevel(i.toShort(), 0) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
            bass = BassBoost(0, newSession).also { it.enabled = _enabled.value; try { it.setStrength(_bass.value.coerceIn(0, 1000).toShort()) } catch (_: Exception) {} }
            reverb = PresetReverb(0, newSession).also { it.enabled = false }
            // Solo se marca tras crear los tres efectos: si el dispositivo no
            // tiene efectos, el próximo cambio de sesión reintenta en vez de
            // dejar el EQ muerto para siempre.
            sessionId = newSession
        } catch (e: Exception) {
            try { releaseFx() } catch (_: Exception) {}
            Log.e("EQ", "attach fail", e)
        }
    }

    fun setEnabled(v: Boolean) {
        _enabled.value = v; prefs.edit().putBoolean("enabled", v).apply()
        try { eq?.enabled = v; bass?.enabled = v } catch (_: Exception) {}
    }
    fun setBand(i: Int, millibels: Int) {
        val lvl = millibels.coerceIn(-1500, 1500)
        _bands.value = _bands.value.toMutableList().also { if (i in it.indices) it[i] = lvl }
        _preset.value = "Personalizado"; prefs.edit().putInt("band$i", lvl).putString("preset", "Personalizado").apply()
        try { eq?.setBandLevel(i.toShort(), lvl.toShort()) } catch (_: Exception) {}
    }
    fun setBass(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        _bass.value = s; prefs.edit().putInt("bass", s).apply()
        try { bass?.setStrength(s.toShort()) } catch (_: Exception) {}
    }
    fun usePreset(name: String) {
        _preset.value = name; prefs.edit().putString("preset", name).apply()
        try {
            val e = eq ?: return
            for (i in 0 until e.numberOfPresets) {
                if (e.getPresetName(i.toShort()) == name) {
                    e.usePreset(i.toShort())
                    val nb = e.numberOfBands.coerceAtMost(5).toInt()
                    _bands.value = List(5) { bi -> if (bi < nb) e.getBandLevel(bi.toShort()).toInt() else 0 }
                    // Persistir: si no, al reiniciar volvían las bandas viejas
                    // con el nombre del preset nuevo.
                    try {
                        val ed = prefs.edit()
                        _bands.value.forEachIndexed { bi, lvl -> ed.putInt("band$bi", lvl) }
                        ed.apply()
                    } catch (_: Exception) {}
                    return
                }
            }
        } catch (_: Exception) {}
    }

    // ── Mis presets con nombre (5 bandas + graves) ──
    private val _customPresets = MutableStateFlow(loadCustom())
    val customPresets: StateFlow<Map<String, List<Int>>> = _customPresets

    private fun loadCustom(): Map<String, List<Int>> {
        return try {
            val raw = prefs.getString("custom_eq", "") ?: ""
            if (raw.isBlank()) return emptyMap()
            raw.split(";").mapNotNull { entry ->
                val kv = entry.split("|")
                if (kv.size != 2) return@mapNotNull null
                val vals = kv[1].split(",").mapNotNull { it.toIntOrNull() }
                if (vals.size != 6) return@mapNotNull null
                kv[0] to vals
            }.toMap()
        } catch (_: Exception) { emptyMap() }
    }

    private fun saveCustom() {
        try {
            val raw = _customPresets.value.entries.joinToString(";") { (k, v) -> "$k|${v.joinToString(",")}" }
            prefs.edit().putString("custom_eq", raw).apply()
        } catch (_: Exception) {}
    }

    /**
     * Guarda el preset. Devuelve false si el nombre ya existe (no pisa en
     * silencio) o si está vacío. Los separadores ;|, se quitan porque romperían
     * el formato de guardado.
     */
    fun saveCustomPreset(name: String): Boolean {
        val n = name.replace(Regex("[;|,\n]"), "").trim().take(24)
        if (n.isEmpty() || _customPresets.value.containsKey(n)) return false
        val vals = _bands.value.take(5) + _bass.value
        _customPresets.value = _customPresets.value + (n to vals)
        saveCustom()
        _preset.value = n; prefs.edit().putString("preset", n).apply()
        return true
    }

    fun deleteCustomPreset(name: String) {
        _customPresets.value = _customPresets.value - name
        saveCustom()
    }

    fun applyCustomPreset(name: String) {
        val vals = _customPresets.value[name] ?: return
        val bands = vals.take(5)
        _bands.value = bands
        bands.forEachIndexed { i, lvl ->
            prefs.edit().putInt("band$i", lvl).apply()
            try { eq?.setBandLevel(i.toShort(), lvl.coerceIn(-1500, 1500).toShort()) } catch (_: Exception) {}
        }
        setBass(vals.getOrElse(5) { 0 })
        _preset.value = name; prefs.edit().putString("preset", name).apply()
    }
    private fun releaseFx() { try { eq?.release() } catch (_: Exception) {}; try { bass?.release() } catch (_: Exception) {}; try { reverb?.release() } catch (_: Exception) {}; eq = null; bass = null; reverb = null }
}

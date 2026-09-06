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
        sessionId = newSession
        try {
            releaseFx()
            eq = Equalizer(0, newSession).also { e ->
                e.enabled = _enabled.value
                val nb = e.numberOfBands.coerceAtMost(5).toInt()
                for (i in 0 until nb) {
                    try { e.setBandLevel(i.toShort(), _bands.value.getOrElse(i) { 0 }.coerceIn(-1500, 1500).toShort()) } catch (_: Exception) {}
                }
            }
            bass = BassBoost(0, newSession).also { it.enabled = _enabled.value; try { it.setStrength(_bass.value.coerceIn(0, 1000).toShort()) } catch (_: Exception) {} }
            reverb = PresetReverb(0, newSession).also { it.enabled = false }
        } catch (e: Exception) { Log.e("EQ", "attach fail", e) }
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
                    return
                }
            }
        } catch (_: Exception) {}
    }
    private fun releaseFx() { try { eq?.release() } catch (_: Exception) {}; try { bass?.release() } catch (_: Exception) {}; try { reverb?.release() } catch (_: Exception) {}; eq = null; bass = null; reverb = null }
}

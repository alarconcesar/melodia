package com.melodia.app

import android.content.Context

/**
 * Interruptores de cortesía para aislar un fallo de arranque sin perder la app.
 * Se activan desde la pantalla de error (CrashScreen) y se pueden desactivar
 * desde Ajustes → Información.
 */
object SafeMode {
    /** Sin efectos de vidrio (haze / RenderEffect): lo primero que se sospecha en un crash de dibujado. */
    const val NO_EFFECTS = "no_effects"

    /** Sin extras nuevos: esqueleto de carga, gestos de fila y carrusel de álbumes. */
    const val NO_EXTRAS = "no_extras"

    private const val PREFS = "melodia_safe"

    fun isOn(ctx: Context, key: String): Boolean = try {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, false)
    } catch (_: Exception) { false }

    fun set(ctx: Context, key: String, on: Boolean) = try {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, on).apply()
    } catch (_: Exception) {}

    /** Activa ambos interruptores (modo seguro completo). */
    fun activarTodo(ctx: Context) {
        set(ctx, NO_EFFECTS, true)
        set(ctx, NO_EXTRAS, true)
    }

    fun apagarTodo(ctx: Context) {
        set(ctx, NO_EFFECTS, false)
        set(ctx, NO_EXTRAS, false)
    }

    fun estaActivo(ctx: Context): Boolean = isOn(ctx, NO_EFFECTS) || isOn(ctx, NO_EXTRAS)
}

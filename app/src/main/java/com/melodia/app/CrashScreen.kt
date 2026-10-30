package com.melodia.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Pantalla de error en Views puro (SIN Compose).
 *
 * Se usa cuando la app se cerró sola: como no depende de Compose ni del
 * contenedor de datos, funciona incluso si el fallo está justamente ahí.
 * Muestra el detalle para copiarlo/compartirlo, permite reintentar y abrir
 * en modo seguro (sin efectos de vidrio ni extras).
 */
object CrashScreen {

    /** Solo cuenta si es reciente (30 min): un archivo viejo no debe secuestrar el arranque. */
    fun leer(ctx: Context): String? {
        return try {
            val f = java.io.File(ctx.filesDir, MelodiaApp.CRASH_FILE)
            if (!f.exists()) {
                null
            } else {
                val reciente = System.currentTimeMillis() - f.lastModified() < 30 * 60_000L
                if (!reciente) { f.delete(); null } else f.readText().trim().takeIf { it.length > 10 }
            }
        } catch (_: Exception) { null }
    }

    fun borrar(ctx: Context) {
        try { java.io.File(ctx.filesDir, MelodiaApp.CRASH_FILE).delete() } catch (_: Exception) {}
    }

    fun build(activity: Activity, trace: String): View {
        val ctx: Context = activity
        fun dp(v: Int) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics
        ).toInt()

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(52), dp(20), dp(20))
            setBackgroundColor(0xFF0E1512.toInt())
        }

        fun texto(t: String, size: Float, color: Int, mono: Boolean = false, top: Int = 0): TextView =
            TextView(ctx).apply {
                text = t
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
                if (mono) typeface = Typeface.MONOSPACE
                if (top > 0) setPadding(0, dp(top), 0, 0)
            }

        col.addView(texto("Melodia no pudo abrir", 22f, 0xFFFFFFFF.toInt()))
        col.addView(texto("Detalle técnico — mándame captura de esto:", 13f, 0xFF9FB0A8.toInt(), top = 6))

        val scroll = ScrollView(ctx).apply {
            addView(texto(trace.take(6000), 10.5f, 0xFFD8E6DF.toInt(), mono = true))
        }
        col.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        fun boton(label: String, accion: () -> Unit) {
            col.addView(Button(ctx).apply {
                text = label
                isAllCaps = false
                setOnClickListener { accion() }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) })
        }

        boton("Copiar detalle") {
            try {
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("melodia-crash", trace))
            } catch (_: Exception) {}
        }
        boton("Compartir detalle") {
            try {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, trace.take(8000))
                ctx.startActivity(Intent.createChooser(i, "Compartir detalle"))
            } catch (_: Exception) {}
        }
        boton("Reintentar") {
            // Si era fallo del contenedor, se reconstruye: recreate() solo no
            // sirve (Application no se recrea y el error seguía ahí = bucle).
            try { (ctx.applicationContext as? MelodiaApp)?.reintentarContenedor() } catch (_: Exception) {}
            borrar(ctx)
            activity.recreate()
        }
        boton("Abrir en modo seguro") {
            SafeMode.activarTodo(ctx)
            borrar(ctx)
            activity.recreate()
        }
        col.addView(
            texto(
                "Modo seguro: sin efectos de vidrio, esqueleto ni gestos nuevos. " +
                    "Se desactiva en Ajustes → Información.",
                11f, 0xFF8FA39A.toInt(), top = 8
            )
        )
        return col
    }
}

/** Se abre sola tras un crash (mejor esfuerzo) para que el error quede a la vista. */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent?.getStringExtra("trace") ?: CrashScreen.leer(this) ?: "sin detalle"
        setContentView(CrashScreen.build(this, trace))
    }
}

package com.melodia.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Melodia DS · v1 ──────────────────────────────────────────────
// Familiar-Spotify, minimalista moderno. Dark-first.
// Refs: fondo negro verdoso, cards #13-1B, verde neón, pill-nav,
// titulares grandes + secundarios atenuados, micro-labels uppercase.

object Ds {
    val Green = Color(0xFF2EE56B)
    val GreenDeep = Color(0xFF087A3E)
    val Heart = Color(0xFF2EE56B)

    // Radios: dos familias y listo (card / fila). Menos variantes = más calma.
    val RHero = 24.dp
    val RCard = 16.dp
    val RRow = 12.dp
    val RChip = 12.dp

    // Alturas / tamaños
    val ArtRow = 52.dp
    val ArtHero = 96.dp
    val PlayFab = 56.dp
    val PlayHero = 64.dp

    // Tipo (todos los Text del sistema usan estos estilos + color explícito)
    // Titulares tight (-0.5/-0.25): se sienten más premium que el tracking neutro
    val Display = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
    val Title = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)
    val Sub = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val Micro = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    val HeroShape = RoundedCornerShape(RHero)
    val CardShape = RoundedCornerShape(RCard)
    val RowShape = RoundedCornerShape(RRow)
}

/** Mezcla dos colores (0 = todo a, 1 = todo b). Para containers tonales del acento. */
private fun blend(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * k,
        green = a.green + (b.green - a.green) * k,
        blue = a.blue + (b.blue - a.blue) * k,
        alpha = 1f
    )
}

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF06110A),
    secondary = blend(accent, Color.White, 0.15f),
    onSecondary = Color(0xFF06110A),
    secondaryContainer = blend(accent, Color(0xFF131715), 0.80f),
    onSecondaryContainer = blend(accent, Color.White, 0.30f),
    tertiary = accent,
    onTertiary = Color(0xFF06110A),
    background = Color(0xFF0A0B0A),
    onBackground = Color(0xFFF2F4F2),
    surface = Color(0xFF131413),
    onSurface = Color(0xFFF2F4F2),
    surfaceVariant = Color(0xFF1A1B1A),
    onSurfaceVariant = Color(0xFFA0A5A0),
    outline = Color(0xFF262826),
    outlineVariant = Color(0xFF1D1F1D),
    scrim = Color.Black,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = blend(accent, Color(0xFF10140F), 0.15f),
    onSecondary = Color.White,
    secondaryContainer = blend(accent, Color.White, 0.82f),
    onSecondaryContainer = blend(accent, Color(0xFF10140F), 0.45f),
    tertiary = accent,
    onTertiary = Color.White,
    background = Color(0xFFF4F6F4),
    onBackground = Color(0xFF10140F),
    surface = Color.White,
    onSurface = Color(0xFF10140F),
    surfaceVariant = Color(0xFFE8ECE8),
    onSurfaceVariant = Color(0xFF5F6B62),
    outline = Color(0xFFD8DED8),
    outlineVariant = Color(0xFFC6CFC6),
    scrim = Color.Black
)

// ── Acentos ────────────────────────────────────────────────────────
data class Accent(val name: String, val dark: Color, val light: Color)

val Accents = listOf(
    Accent("Verde", Color(0xFF2EE56B), Color(0xFF087A3E)),
    Accent("Azul", Color(0xFF53B7FF), Color(0xFF02649D)),
    Accent("Morado", Color(0xFFC58AFF), Color(0xFF6A1B9A)),
    Accent("Naranja", Color(0xFFFFAB4A), Color(0xFFBF360C)),
    Accent("Rosa", Color(0xFFFF7EB0), Color(0xFFAD1457))
)

var accentName by mutableStateOf("Verde")
var isDarkMode by mutableStateOf<Boolean?>(null)

fun currentAccent(): Accent = Accents.find { it.name == accentName } ?: Accents[0]

private const val UI_PREFS = "melodia_ui"

fun loadUiPrefs(ctx: android.content.Context) {
    try {
        val p = ctx.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE)
        accentName = p.getString("accent", "Verde") ?: "Verde"
        isDarkMode = if (p.contains("dark")) p.getBoolean("dark", true) else null
    } catch (_: Exception) {}
}

fun saveUiPrefs(ctx: android.content.Context) {
    try {
        ctx.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putString("accent", accentName)
            .apply {
                if (isDarkMode == null) remove("dark") else putBoolean("dark", isDarkMode!!)
            }
            .apply()
    } catch (_: Exception) {}
}

@Composable
fun MelodiaTheme(content: @Composable () -> Unit) {
    val dark = isDarkMode ?: isSystemInDarkTheme()
    val accent = currentAccent()
    MaterialTheme(
        colorScheme = if (dark) darkScheme(accent.dark) else lightScheme(accent.light),
        typography = Typography(
            displaySmall = Ds.Display,
            headlineSmall = Ds.Display.copy(fontSize = 22.sp, lineHeight = 28.sp),
            titleLarge = Ds.Title.copy(fontSize = 20.sp, lineHeight = 26.sp),
            titleMedium = Ds.Title,
            bodyLarge = Ds.Body.copy(lineHeight = 22.sp),
            bodyMedium = Ds.Body.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = Ds.Sub.copy(lineHeight = 18.sp),
            labelLarge = Ds.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            labelMedium = Ds.Sub.copy(fontWeight = FontWeight.Medium),
            labelSmall = Ds.Micro
        ),
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground
            ) { content() }
        }
    )
}

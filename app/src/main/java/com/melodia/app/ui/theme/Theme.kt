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
    val GreenDeep = Color(0xFF0E9F52)
    val Heart = Color(0xFF2EE56B)

    // Radios
    val RHero = 28.dp
    val RCard = 20.dp
    val RRow = 16.dp
    val RChip = 14.dp

    // Alturas / tamaños
    val ArtRow = 56.dp
    val ArtHero = 96.dp
    val PlayFab = 56.dp
    val PlayHero = 72.dp

    // Tipo (todos los Text del sistema usan estos estilos + color explícito)
    val Display = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
    val Title = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val Sub = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val Micro = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val HeroShape = RoundedCornerShape(RHero)
    val CardShape = RoundedCornerShape(RCard)
    val RowShape = RoundedCornerShape(RRow)
}

private val DarkScheme = darkColorScheme(
    primary = Ds.Green,
    onPrimary = Color(0xFF06110A),
    background = Color(0xFF0A0D0B),
    onBackground = Color(0xFFF2F5F2),
    surface = Color(0xFF131715),
    onSurface = Color(0xFFF2F5F2),
    surfaceVariant = Color(0xFF1B211E),
    onSurfaceVariant = Color(0xFF9AA5A0),
    outline = Color(0xFF2A332D)
)

private val LightScheme = lightColorScheme(
    primary = Ds.GreenDeep,
    onPrimary = Color.White,
    background = Color(0xFFF4F6F4),
    onBackground = Color(0xFF10140F),
    surface = Color.White,
    onSurface = Color(0xFF10140F),
    surfaceVariant = Color(0xFFE8ECE8),
    onSurfaceVariant = Color(0xFF5F6B62),
    outline = Color(0xFFD8DED8)
)

var isDarkMode by mutableStateOf<Boolean?>(null)

@Composable
fun MelodiaTheme(content: @Composable () -> Unit) {
    val dark = isDarkMode ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = Typography(),
        content = content
    )
}

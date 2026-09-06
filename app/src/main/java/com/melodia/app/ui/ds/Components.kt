package com.melodia.app.ui.ds

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melodia.app.data.model.Song
import com.melodia.app.ui.screens.fmt
import com.melodia.app.ui.theme.Ds

// ── Micro label uppercase ────────────────────────────────────────
@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = Ds.Micro,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = modifier
    )
}

// ── Section header: título + acción ──────────────────────────────
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = Ds.Title, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        if (action != null) {
            if (onAction != null) {
                Text(
                    text = action, style = Ds.Sub,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onAction).padding(4.dp)
                )
            } else {
                Text(text = action, style = Ds.Sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── TrackRow DS · UN solo corazón ────────────────────────────────
// Tap = reproducir · long-press = añadir a playlist
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    s: Song,
    active: Boolean,
    isFav: Boolean,
    onTap: () -> Unit,
    onFav: () -> Unit,
    onLongPress: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(Ds.ArtRow).clip(Ds.RowShape)
                .background(if (active) cs.primary.copy(alpha = 0.16f) else cs.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                EqBars(color = cs.primary)
            } else {
                Icon(
                    Icons.Default.MusicNote, null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                s.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = Ds.Body, color = cs.onBackground
            )
            Text(
                "${s.artist} · ${fmt(s.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = Ds.Sub, color = cs.onSurfaceVariant
            )
        }
        IconButton(onClick = onFav, modifier = Modifier.size(40.dp)) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                tint = if (isFav) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── QuickCard DS (grid 2 col) ────────────────────────────────────
@Composable
fun QuickCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = Ds.CardShape, color = cs.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .border(1.dp, cs.outline.copy(alpha = 0.45f), Ds.CardShape)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = cs.primary, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Ds.Title, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── HeroCard DS · seguir escuchando ──────────────────────────────
@Composable
fun HeroCard(
    label: String,
    title: String,
    subtitle: String,
    playing: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = Ds.HeroShape, color = cs.surface, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
        .border(1.dp, cs.outline.copy(alpha = 0.4f), Ds.HeroShape)) {
        Column(modifier = Modifier.padding(20.dp)) {
            MicroLabel(label)
            Spacer(Modifier.height(6.dp))
            Text(title, style = Ds.Display, color = cs.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Ds.Body, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape, color = cs.primary,
                    modifier = Modifier.size(Ds.PlayHero).clickable(onClick = onPlay)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                            tint = cs.surface, modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (playing) "Sonando ahora" else "Toca para escuchar",
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
            }
        }
    }
}

// ── Fondo DS · degradado + glow ambiental ─────────────────────────
@Composable
fun DsBackground() {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(cs.surface, cs.surface, cs.background)))
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val green = Color(0xFF2EE56B)
        // Glow superior: degradado radial que se funde a transparente (sin borde duro)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(green.copy(alpha = 0.14f), green.copy(alpha = 0.05f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, -w * 0.3f),
                radius = w * 0.95f
            ),
            radius = w * 0.95f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, -w * 0.3f)
        )
        // Glow inferior sutil
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(green.copy(alpha = 0.06f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, size.height + w * 0.2f),
                radius = w
            ),
            radius = w,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, size.height + w * 0.2f)
        )
    }
}

// ── Barras de ecualizador animadas ───────────────────────────────
@Composable
fun EqBars(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val f by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(380 + i * 140), RepeatMode.Reverse),
                label = "bar$i"
            )
            Box(
                Modifier.width(3.dp).height((7 + 13 * f).dp).clip(CircleShape).background(color)
            )
        }
    }
}

// ── PillNav DS · barra flotante ──────────────────────────────────
@Composable
fun MelodiaPillNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        Triple("Inicio", Icons.Default.Home, 0),
        Triple("Explorar", Icons.Default.Search, 1),
        Triple("Listas", Icons.Default.PlaylistPlay, 2),
        Triple("Favs", Icons.Default.Favorite, 3)
    )
    Surface(
        shape = CircleShape, color = cs.surface.copy(alpha = 0.78f),
        tonalElevation = 4.dp, shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
            .border(1.dp, cs.outline.copy(alpha = 0.6f), CircleShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, idx) ->
                val sel = tab == idx
                // Layout fijo: mismo tamaño siempre (sin saltos)
                Box(
                    modifier = Modifier.weight(1f).clip(CircleShape)
                        .background(if (sel) cs.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onTab(idx) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, null, tint = if (sel) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Text(
                            label,
                            style = Ds.Sub,
                            color = if (sel) cs.primary else cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── MiniPlayer DS · flotante sobre la pill ───────────────────────
@Composable
fun MiniPlayerDs(
    title: String,
    artist: String,
    playing: Boolean,
    progress: Float,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = Ds.CardShape, color = cs.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = 2.dp, shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen)
            .border(1.dp, cs.outline.copy(alpha = 0.55f), Ds.CardShape)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(Ds.RowShape).background(cs.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.MusicNote, null, tint = cs.primary, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                    Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Sub, color = cs.onSurfaceVariant)
                }
                IconButton(onClick = onToggleFav, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                        tint = if (isFav) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(20.dp)
                    )
                }
                Surface(shape = CircleShape, color = cs.primary, modifier = Modifier.size(44.dp).clickable(onClick = onToggle)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                            tint = cs.surface, modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

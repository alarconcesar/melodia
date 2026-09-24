package com.melodia.app.ui.ds

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ContentUris
import android.provider.MediaStore
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.melodia.app.data.model.Song
import com.melodia.app.ui.screens.fmt
import com.melodia.app.ui.theme.Ds

// ── Modelo de carátula: URL online o contenido de MediaStore ─────
@Composable
fun songArtModel(albumId: Long, artUrl: String?): Any? = remember(albumId, artUrl) {
    artUrl ?: if (albumId > 0) ContentUris.withAppendedId(
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId
    ) else null
}

// ── Carátula real del álbum (MediaStore) con fallback a icono ─────
// El icono va DETRÁS: si la imagen carga lo tapa; si no hay arte, se ve el icono.
@Composable
fun SongArt(
    albumId: Long,
    size: Dp,
    shape: Shape,
    iconSize: Dp = 24.dp,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    artUrl: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val model = songArtModel(albumId, artUrl)
    Box(
        modifier = modifier.size(size).clip(shape)
            .background(if (active) cs.primary.copy(alpha = 0.16f) else cs.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            EqBars(color = cs.primary)
        } else {
            Icon(
                Icons.Default.MusicNote, null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
            if (model != null) {
                AsyncImage(
                    model = model, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ── Click con rebote: se hunde al presionar y vuelve con spring ──
@Composable
fun pressClick(onClick: () -> Unit, min: Float = 0.9f): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) min else 1f,
        spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )
    return Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

// ── Spinner propio: M3 CircularProgressIndicator crashea en este BOM ──
// (M3 1.1.2 llama a KeyframesSpecConfig.at() que animation-core 1.6.0 no trae)
@Composable
fun MelodiaSpinner(size: androidx.compose.ui.unit.Dp = 32.dp, width: androidx.compose.ui.unit.Dp = 3.dp, color: androidx.compose.ui.graphics.Color? = null) {
    val cs = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "spin")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900)), label = "a")
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        drawArc(
            color = color ?: cs.primary, startAngle = angle, sweepAngle = 270f, useCenter = false,
            style = Stroke(width.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

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
                    color = MaterialTheme.colorScheme.primary,
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
// Deslizar → derecha encola al final · izquierda marca favorito
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    s: Song,
    active: Boolean,
    isFav: Boolean,
    onTap: () -> Unit,
    onFav: () -> Unit,
    onLongPress: () -> Unit,
    onQueue: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val maxSwipe = with(LocalDensity.current) { 140.dp.toPx() }
    val threshold = maxSwipe * 0.5f
    Box(Modifier.fillMaxWidth()) {
        // Franja de acción: solo se pinta lo que el deslizamiento va descubriendo
        Canvas(Modifier.matchParentSize()) {
            val v = offset.value
            val a = (kotlin.math.abs(v) / maxSwipe).coerceIn(0f, 1f) * 0.18f
            if (v > 0f) {
                drawRect(
                    color = cs.primary.copy(alpha = a),
                    size = androidx.compose.ui.geometry.Size(v, size.height)
                )
            } else if (v < 0f) {
                drawRect(
                    color = cs.primary.copy(alpha = a),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width + v, 0f),
                    size = androidx.compose.ui.geometry.Size(-v, size.height)
                )
            }
        }
        Box(Modifier.matchParentSize()) {
            val leftA = (offset.value / maxSwipe).coerceIn(0f, 1f)
            val rightA = (-offset.value / maxSwipe).coerceIn(0f, 1f)
            if (onQueue != null) Icon(
                Icons.Default.PlaylistAdd, null,
                tint = cs.primary, modifier = Modifier.align(Alignment.CenterStart)
                    .padding(start = 20.dp).size(20.dp)
                    .graphicsLayer { alpha = leftA }
            )
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                tint = cs.primary, modifier = Modifier.align(Alignment.CenterEnd)
                    .padding(end = 20.dp).size(20.dp)
                    .graphicsLayer { alpha = rightA }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { d ->
                        scope.launch {
                            offset.snapTo((offset.value + d).coerceIn(-maxSwipe, maxSwipe))
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val v = offset.value
                        if (v >= threshold && onQueue != null) onQueue()
                        else if (v <= -threshold) onFav()
                        offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                )
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        SongArt(
            albumId = s.albumId, size = Ds.ArtRow, shape = Ds.RowShape,
            iconSize = 24.dp, active = active, artUrl = s.artUrl
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                s.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = if (active) Ds.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) else Ds.Body,
                color = if (active) cs.primary else cs.onBackground
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
}

// ── Esqueleto de carga (mientras se lee la biblioteca) ───────────
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "sk")
    val a by t.animateFloat(
        initialValue = 0.10f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Reverse),
        label = "ska"
    )
    Box(modifier.clip(shape).background(cs.onSurfaceVariant.copy(alpha = a)))
}

/** Silueta de Inicio: evita el parpadeo de "biblioteca vacía" al abrir la app. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp), Ds.HeroShape)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.weight(1f).height(92.dp), Ds.CardShape)
            SkeletonBlock(Modifier.weight(1f).height(92.dp), Ds.CardShape)
        }
        Spacer(Modifier.height(22.dp))
        SkeletonBlock(Modifier.width(150.dp).height(18.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(10.dp))
        repeat(5) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                SkeletonBlock(Modifier.size(56.dp), Ds.RowShape)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(14.dp), RoundedCornerShape(6.dp))
                    Spacer(Modifier.height(8.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.45f).height(11.dp), RoundedCornerShape(6.dp))
                }
            }
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
        val green = cs.primary
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
fun MelodiaPillNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier, badges: List<String> = emptyList()) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        Triple("Inicio", Icons.Default.Home, 0),
        Triple("Explorar", Icons.Default.Search, 1),
        Triple("Listas", Icons.Default.PlaylistPlay, 2),
        Triple("Favs", Icons.Default.Favorite, 3),
        Triple("Online", Icons.Default.Download, 4)
    )
    Surface(
        shape = CircleShape, color = cs.surface.copy(alpha = 0.78f),
        tonalElevation = 4.dp, shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
            .border(1.dp, cs.outline.copy(alpha = 0.6f), CircleShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, idx) ->
                val sel = tab == idx
                // Layout fijo: mismo tamaño siempre (sin saltos)
                val pillBg by animateColorAsState(
                    if (sel) cs.primary.copy(alpha = 0.16f) else Color.Transparent,
                    tween(250), label = "pill"
                )
                Box(
                    modifier = Modifier.weight(1f).clip(CircleShape)
                        .background(pillBg)
                        .clickable { onTab(idx) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val tabScale by animateFloatAsState(
                            if (sel) 1.14f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "tab"
                        )
                        Icon(
                            icon, null,
                            tint = if (sel) cs.primary else cs.onSurfaceVariant,
                            modifier = Modifier.size(21.dp).graphicsLayer { scaleX = tabScale; scaleY = tabScale }
                        )
                        Text(
                            label,
                            style = if (sel) Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) else Ds.Sub,
                            color = if (sel) cs.primary else cs.onSurfaceVariant
                        )
                        // Contador vivo con altura fija (sin saltos de layout)
                        Text(
                            badges.getOrNull(idx) ?: " ",
                            style = Ds.Sub.copy(fontSize = 9.sp),
                            color = cs.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            modifier = Modifier.height(11.dp)
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
    albumId: Long = -1L,
    artUrl: String? = null,
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
                SongArt(
                    albumId = albumId, size = 44.dp, shape = Ds.RowShape,
                    iconSize = 22.dp, artUrl = artUrl
                )
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
                Surface(shape = CircleShape, color = cs.primary, modifier = Modifier.size(44.dp).then(pressClick(onToggle))) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = playing, label = "pp-mini") { p ->
                            Icon(
                                if (p) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                                tint = cs.surface, modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            // Progreso fluido: el estado llega cada 500ms, se interpola con tween
            val smooth by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "minip")
            LinearProgressIndicator(
                progress = smooth, modifier = Modifier.fillMaxWidth().height(2.dp),
                color = cs.primary, trackColor = cs.onSurfaceVariant.copy(alpha = 0.22f)
            )
        }
    }
}

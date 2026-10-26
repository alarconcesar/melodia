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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.platform.LocalContext
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
import com.melodia.app.SafeMode
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
    // Pedir la imagen al tamaño exacto de la fila (no al de la pantalla) y sin
    // filtrado caro: al scrollear se decodifican muchas carátulas por segundo.
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val req = remember(model, px) {
        model?.let { coil.request.ImageRequest.Builder(ctx).data(it).size(px).build() }
    }
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
            if (req != null) {
                AsyncImage(
                    model = req, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
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
    val rowCtx = LocalContext.current.applicationContext
    val safeGestures = remember { SafeMode.isOn(rowCtx, SafeMode.NO_EXTRAS) }
    val offset = remember { Animatable(0f) }
    val maxSwipe = with(LocalDensity.current) { 140.dp.toPx() }
    val threshold = maxSwipe * 0.5f
    Box(Modifier.fillMaxWidth()) {
        // Franja de acción: solo existe mientras hay deslizamiento. Sin esto cada
        // fila visible arrastraba dos capas (Canvas + caja con alpha) al hacer scroll.
        val swiping = !safeGestures && offset.value != 0f
        if (swiping) Canvas(Modifier.matchParentSize()) {
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
        if (swiping) Box(Modifier.matchParentSize()) {
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
                .then(
                    if (safeGestures) Modifier else Modifier.pointerInput(Unit) {
                        // Dirección bloqueada al primer movimiento: si el gesto es
                        // vertical se abandona y la lista scrollea. Con `draggable`
                        // un scroll diagonal abría la franja sin querer.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startY = down.position.y
                            var locked = 0 // 0 = indeciso, 1 = horizontal, -1 = vertical
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                val dx = ch.position.x - startX
                                val dy = ch.position.y - startY
                                if (locked == 0) {
                                    val slop = viewConfiguration.touchSlop
                                    when {
                                        kotlin.math.abs(dx) > slop &&
                                            kotlin.math.abs(dx) > kotlin.math.abs(dy) -> locked = 1
                                        kotlin.math.abs(dy) > slop -> { locked = -1; break }
                                    }
                                }
                                if (locked == 1) {
                                    ch.consume()
                                    val v = dx.coerceIn(-maxSwipe, maxSwipe)
                                    scope.launch { offset.snapTo(v) }
                                }
                            }
                            if (locked == 1) {
                                val v = offset.value
                                if (v >= threshold && onQueue != null) onQueue()
                                else if (v <= -threshold) onFav()
                                scope.launch { offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                        }
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
        if (isFav) {
            // Solo se marca lo que es favorito, y ahora el corazón quita: antes era
            // un icono decorativo y en Favs había que deslizar para sacar un tema.
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onFav),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite, "Quitar de favoritos",
                    tint = cs.primary, modifier = Modifier.size(18.dp)
                )
            }
        }
        }
    }
}

/** Barra de progreso del miniplayer aislada: el tween de 500 ms solo la toca a ella. */
@Composable
private fun MiniProgress(progress: Float) {
    val cs = MaterialTheme.colorScheme
    val smooth by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "minip")
    LinearProgressIndicator(
        progress = smooth, modifier = Modifier.fillMaxWidth().height(2.dp),
        color = cs.primary, trackColor = cs.onSurfaceVariant.copy(alpha = 0.22f)
    )
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
        Spacer(Modifier.height(24.dp))
        SkeletonBlock(Modifier.width(150.dp).height(18.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
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

// ── HeroCard DS · seguir escuchando (sin borde ni texto de relleno) ──
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
    Surface(shape = Ds.HeroShape, color = cs.surface, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(modifier = Modifier.padding(18.dp)) {
            MicroLabel(label)
            Spacer(Modifier.height(6.dp))
            Text(title, style = Ds.Display, color = cs.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = CircleShape, color = cs.primary,
                modifier = Modifier.size(Ds.PlayHero).clickable(onClick = onPlay)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                        tint = cs.surface, modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// ── Fondo DS · plano, sin glows ───────────────────────────────────
// El minimalismo vive del espacio y la tipografía, no de luces de fondo.
@Composable
fun DsBackground() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background))
}

/**
 * Estado vacío único de la app: icono en círculo tenue, título, subtítulo y una
 * acción opcional. Antes cada pantalla inventaba el suyo (tres tamaños, tres
 * grises distintos).
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(26.dp), tint = cs.primary) }
        Spacer(Modifier.height(14.dp))
        Text(title, style = Ds.Title, color = cs.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                action,
                style = Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = cs.primary,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
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

// ── PillNav DS · barra flotante, sin borde ni contadores ──────────
@Composable
fun MelodiaPillNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        Triple("Inicio", Icons.Default.Home, 0),
        Triple("Explorar", Icons.Default.Search, 1),
        Triple("Listas", Icons.Default.PlaylistPlay, 2),
        Triple("Favs", Icons.Default.Favorite, 3),
        Triple("Online", Icons.Default.Download, 4)
    )
    Surface(
        shape = CircleShape, color = cs.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp, shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
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
                    if (sel) cs.primary.copy(alpha = 0.12f) else Color.Transparent,
                    tween(200), label = "pill"
                )
                Box(
                    modifier = Modifier.weight(1f).clip(CircleShape)
                        .background(pillBg)
                        .clickable { onTab(idx) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Cambio de color y un pelo de escala; sin rebote.
                        val tabScale by animateFloatAsState(
                            if (sel) 1.06f else 1f,
                            tween(180), label = "tab"
                        )
                        Icon(
                            icon, null,
                            tint = if (sel) cs.primary else cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).graphicsLayer { scaleX = tabScale; scaleY = tabScale }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            label,
                            style = if (sel) Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) else Ds.Sub,
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
        shape = Ds.CardShape, color = cs.surfaceVariant.copy(alpha = 0.97f),
        tonalElevation = 0.dp, shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen)
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
                if (isFav) {
                    Icon(
                        Icons.Default.Favorite, null,
                        tint = cs.primary, modifier = Modifier.padding(horizontal = 10.dp).size(18.dp)
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
            // Progreso fluido en su propio composable: si el tween vive aquí, cada
            // frame de la animación recompone toda la barra (arte, textos y botones).
            MiniProgress(progress = progress)
        }
    }
}

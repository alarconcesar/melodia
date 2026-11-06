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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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

/** Selección múltiple compartida: la barra vive en la raíz y las filas la leen. */
data class SelectionState(
    val selecting: Boolean = false,
    val selected: Set<Long> = emptySet(),
    val toggle: (Long) -> Unit = {},
    val start: (Long?) -> Unit = {},
    val reportScope: (List<Long>) -> Unit = {}
)

val LocalSelection = androidx.compose.runtime.staticCompositionLocalOf { SelectionState() }

/** Registra los IDs de la colección visible; no borra búsqueda ni cambia el contenido. */
@Composable
fun SelectionScope(ids: List<Long>) {
    val selection = LocalSelection.current
    LaunchedEffect(ids) { selection.reportScope(ids) }
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
        model?.let {
            coil.request.ImageRequest.Builder(ctx).data(it).size(px)
                .crossfade(180) // la carátula aparece con un fundido, no de golpe
                .build()
        }
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
fun pressClick(
    onClick: () -> Unit,
    min: Float = 0.96f,
    role: Role? = null,
    label: String? = null
): Modifier {
    // Modo seguro (NO_EFFECTS): sin animación de escala, solo el click.
    val context = androidx.compose.ui.platform.LocalContext.current
    val safe = remember(context) {
        com.melodia.app.SafeMode.isOn(context, com.melodia.app.SafeMode.NO_EFFECTS)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (safe) {
        return Modifier.clickable(
            interactionSource = interaction, indication = null,
            role = role, onClickLabel = label, onClick = onClick
        )
    }
    val scale by animateFloatAsState(
        if (pressed) min else 1f,
        // Respuesta de pulsación: rápida y con un rebote apenas perceptible.
        spring(stiffness = 900f, dampingRatio = 0.6f),
        label = "press"
    )
    return Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction, indication = null,
            role = role, onClickLabel = label, onClick = onClick
        )
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
                Box(
                    Modifier.heightIn(min = 48.dp).clickable(
                        role = Role.Button, onClickLabel = action, onClick = onAction
                    ).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = action, style = Ds.Sub, color = MaterialTheme.colorScheme.primary)
                }
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
    val selection = LocalSelection.current
    val selecting = selection.selecting
    val selected = selection.selected.contains(s.id)
    // Tic táctil al marcar: el modo de selección se siente físico.
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val onSelectToggle = {
        try { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) } catch (_: Exception) {}
        selection.toggle(s.id)
    }
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val rowCtx = LocalContext.current.applicationContext
    val safeGestures = remember { SafeMode.isOn(rowCtx, SafeMode.NO_EXTRAS) }
    var dragX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var releaseJob by remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val maxSwipe = with(LocalDensity.current) { 140.dp.toPx() }
    val threshold = maxSwipe * 0.5f
    // Feedback de pulsación: la fila se hunde 1.5 % mientras el dedo está encima.
    val pressSource = remember { MutableInteractionSource() }
    val rowPressed by pressSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        if (rowPressed) 0.985f else 1f, spring(stiffness = 1200f, dampingRatio = 0.8f), label = "rowpress"
    )
    // Selección: fondo tenue que aparece con tween y check que entra con
    // spring desde 0.6 (antes aparecía de golpe, sin transición).
    val selBg by animateColorAsState(
        if (selected) cs.primary.copy(alpha = 0.10f) else Color.Transparent,
        tween(180), label = "selbg"
    )
    val selScale = remember { Animatable(1f) }
    LaunchedEffect(selected, selecting) {
        if (selected) {
            selScale.snapTo(0.6f)
            selScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 700f))
        }
    }
    // El corazón late al marcar: aparece pequeño y crece con spring.
    val heart = remember { Animatable(1f) }
    LaunchedEffect(isFav) {
        if (isFav) {
            heart.snapTo(0.7f)
            heart.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    Box(
        Modifier.fillMaxWidth()
            // El detector vive en el contenedor inmóvil. Medido dentro de la fila
            // que se traslada, cada evento restaba el offset anterior a la
            // distancia del dedo y la fila retrocedía: el parpadeo del swipe.
            .then(
                if (safeGestures || selecting) Modifier else Modifier.pointerInput(Unit) {
                    // Dirección bloqueada al primer movimiento: si el gesto es
                    // vertical se abandona y la lista scrollea. Con `draggable`
                    // un scroll diagonal abría la franja sin querer.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Un toque nuevo manda: si la fila venía regresando, el
                        // retorno se congela aquí y el arrastre continúa desde
                        // donde está, sin pelear ni saltar a cero.
                        try { releaseJob?.cancel() } catch (_: Exception) {}
                        releaseJob = null
                        val base = dragX
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
                                // Directo al estado, sin corrutina por evento:
                                // N snapTo en vuelo se pisaban y parpadearon.
                                dragX = (base + dx).coerceIn(-maxSwipe, maxSwipe)
                            }
                        }
                        if (locked == 1) {
                            val v = dragX
                            if (v >= threshold && onQueue != null) onQueue()
                            else if (v <= -threshold) onFav()
                            // Un solo regreso: si un evento viejo llega tarde,
                            // ya no hay snapTo que lo traiga de vuelta.
                            releaseJob = scope.launch {
                                val a = Animatable(v)
                                try {
                                    a.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) { dragX = value }
                                } catch (_: Exception) {}
                            }
                        } else if (base != 0f) {
                            // El toque no fue un deslizamiento: termina de volver.
                            releaseJob = scope.launch {
                                val a = Animatable(base)
                                try {
                                    a.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) { dragX = value }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            )
    ) {
        // Franja de acción: solo existe mientras hay deslizamiento. Sin esto cada
        // fila visible arrastraba dos capas (Canvas + caja con alpha) al hacer scroll.
        val swiping = !safeGestures && dragX != 0f
        if (swiping) Canvas(Modifier.matchParentSize()) {
            val v = dragX
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
            val leftA = (dragX / maxSwipe).coerceIn(0f, 1f)
            val rightA = (-dragX / maxSwipe).coerceIn(0f, 1f)
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
                // Desplaza el nodo completo (incluida su semántica) para que lo
                // que se ve y lo que se mide sean lo mismo. El detector está en
                // el Box exterior y no se mueve.
                .offset { IntOffset(dragX.roundToInt(), 0) }
                .semantics { this.selected = selected }
                .combinedClickable(
                    interactionSource = pressSource,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = if (selecting) {
                        if (selected) "Deseleccionar ${s.title}" else "Seleccionar ${s.title}"
                    } else "Reproducir ${s.title}",
                    onLongClickLabel = if (selecting) "Abrir opciones de ${s.title}" else "Seleccionar varios",
                    onClick = { if (selecting) onSelectToggle() else onTap() },
                    onLongClick = { if (selecting) onLongPress() else selection.start(s.id) }
                )
                .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
                .background(selBg)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        SongArt(
            albumId = s.albumId, size = Ds.ArtRow, shape = Ds.RowShape,
            iconSize = 24.dp, active = active, artUrl = s.artUrl
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            val tituloColor by androidx.compose.animation.animateColorAsState(
                if (active) cs.primary else cs.onBackground, tween(200), label = "titcol"
            )
            Text(
                s.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = if (active) Ds.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) else Ds.Body,
                color = tituloColor
            )
            Text(
                "${s.artist.ifBlank { "Desconocido" }} · ${fmt(s.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = Ds.Sub, color = cs.onSurfaceVariant
            )
        }
        // Ranura derecha con transición: el check de selección y el corazón
        // se cruzan con fade en vez de aparecer/desaparecer de golpe.
        androidx.compose.animation.Crossfade(
            targetState = when {
                selecting -> 2
                isFav -> 1
                else -> 0
            },
            animationSpec = tween(160),
            label = "slotrow"
        ) { modo ->
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                when (modo) {
                    2 -> {
                        if (selected) {
                            // Halo suave detrás del check marcado.
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(cs.primary.copy(alpha = 0.16f))
                            )
                        }
                        Icon(
                            if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            if (selected) "Seleccionado" else "Sin seleccionar",
                            tint = if (selected) cs.primary else cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                                .graphicsLayer { scaleX = selScale.value; scaleY = selScale.value }
                        )
                    }
                    1 -> Box(
                        Modifier.fillMaxSize().clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Quitar de favoritos", onClick = onFav),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite, "Quitar de favoritos",
                            tint = cs.primary,
                            modifier = Modifier.size(18.dp)
                                .graphicsLayer { scaleX = heart.value; scaleY = heart.value }
                        )
                    }
                }
            }
        }
        // La ficha con su botón propio: info, encolar, editar, compartir y
        // excluir sin depender de gestos escondidos.
        IconButton(onClick = onLongPress, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.MoreVert, "Más opciones",
                tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp)
            )
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
    val base = cs.onSurfaceVariant.copy(alpha = 0.10f)
    val brillo = cs.onSurfaceVariant.copy(alpha = 0.26f)
    val t = rememberInfiniteTransition(label = "sk")
    // Barrido de brillo diagonal. Se lee en la fase de dibujo: cero
    // recomposiciones por frame (solo redibuja lo que ya está en pantalla).
    val avance by t.animateFloat(
        initialValue = -0.4f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ska"
    )
    Box(
        modifier.clip(shape).background(base).drawBehind {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val ancho = h * 1.1f + w * 0.18f
            val x = avance * (w + ancho) - ancho
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, brillo, Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x + ancho, h)
                )
            )
        }
    )
}

/**
 * Entrada de pantallas de detalle: fade + deslizamiento corto desde la
 * derecha. Se aplica al contenedor ya compuesto (una sola animación).
 */
@Composable
fun Modifier.enterFromRight(distance: androidx.compose.ui.unit.Dp = 48.dp): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        a.animateTo(1f, tween(260, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = a.value
        translationX = (1f - a.value) * distance.toPx()
    }
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
    val cardPress = remember { MutableInteractionSource() }
    val cardDown by cardPress.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        if (cardDown) 0.985f else 1f, spring(stiffness = 900f, dampingRatio = 0.8f), label = "heropress"
    )
    Surface(
        shape = Ds.HeroShape, color = cs.surface,
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clickable(
                interactionSource = cardPress, indication = null,
                role = Role.Button, onClickLabel = label, onClick = onOpen
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            MicroLabel(label)
            Spacer(Modifier.height(6.dp))
            Text(title, style = Ds.Display, color = cs.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = CircleShape, color = cs.primary,
                modifier = Modifier.size(Ds.PlayHero).then(pressClick(onPlay, min = 0.9f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (playing) "Pausar" else "Reproducir",
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
    // El vacío tiene una entrada breve, no una animación infinita ornamental.
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(180)) }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp)
            .graphicsLayer {
                alpha = entrada.value
                translationY = (1f - entrada.value) * 10.dp.toPx()
            },
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
                modifier = Modifier.clip(CircleShape)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

// ── Barras de ecualizador animadas ───────────────────────────────
@Composable
fun EqBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val levels = List(3) { i ->
        transition.animateFloat(0.25f, 1f,
            infiniteRepeatable(tween(380 + i * 140), RepeatMode.Reverse), label = "eq-$i")
    }
    Canvas(modifier.size(18.dp, 22.dp)) {
        val bar = 3.dp.toPx()
        levels.forEachIndexed { i, level ->
            val height = size.height * level.value
            drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(i * 6.dp.toPx(), (size.height-height)/2),
                size = androidx.compose.ui.geometry.Size(bar, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(bar/2))
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
        Triple("Favoritos", Icons.Default.Favorite, 3),
        Triple("Online", Icons.Default.Download, 4)
    )
    Surface(
        shape = CircleShape, color = cs.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp, shadowElevation = 0.dp,
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
                        .clickable(
                            role = Role.Tab,
                            onClick = { onTab(idx) }
                        )
                        .semantics {
                            contentDescription = label
                            selected = sel
                        }
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
                            style = Ds.Sub.copy(fontSize = 11.sp, fontWeight = if (sel) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
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
        tonalElevation = 0.dp, shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
            .clickable(
                role = Role.Button, onClickLabel = "Abrir reproductor",
                onClick = onOpen
            )
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
                IconButton(
                    onClick = onToggleFav,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFav) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (isFav) cs.primary else cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Surface(shape = CircleShape, color = cs.primary, modifier = Modifier.size(44.dp).then(pressClick(onToggle))) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = playing, label = "pp-mini") { p ->
                            Icon(
                                if (p) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (p) "Pausar" else "Reproducir",
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

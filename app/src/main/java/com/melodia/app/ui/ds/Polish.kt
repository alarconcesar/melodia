package com.melodia.app.ui.ds

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melodia.app.SafeMode
import com.melodia.app.ui.theme.Ds

/** Una cabecera para pantallas completas, sin alturas rígidas ni texto centrado a ojo. */
@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = cs.onSurface)
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(title, style = Ds.Title, color = cs.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun SectionCaption(text: String) {
    Text(text, style = Ds.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).semantics { heading() })
}

/** Acciones de colección con igual jerarquía en artista, álbum y playlist. */
@Composable
fun CollectionActions(onPlay: () -> Unit, onShuffle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onPlay, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp), shape = Ds.RowShape) {
            Text("Reproducir", color = cs.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(onClick = onShuffle, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp), shape = Ds.RowShape) {
            Text("Aleatorio", color = cs.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Dos niveles: contador/cerrar y acciones. No estrangula el texto en 320 dp. */
@Composable
fun SelectionToolbar(
    count: Int,
    allSelected: Boolean,
    hasItems: Boolean,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = Ds.CardShape, color = cs.surfaceVariant, tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(cs.primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = count,
                        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) }, label = "seleccion-contador") { n ->
                        Text(if (n > 99) "99+" else "$n", style = Ds.Title, color = cs.primary)
                    }
                }
                Text("$count ${if (count == 1) "seleccionado" else "seleccionados"}",
                    style = Ds.Body.copy(fontWeight = FontWeight.Medium), color = cs.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 10.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, "Cancelar selección", tint = cs.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onSelectAll, enabled = hasItems,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Icon(Icons.Default.SelectAll, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (allSelected) "Nada" else "Todo", maxLines = 1)
                }
                TextButton(onClick = onDelete, enabled = count > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.error)) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar", maxLines = 1)
                }
            }
        }
    }
}

/** Tarjeta reciente: en selección marca; fuera de ella reproduce. */
@Composable
fun RecentCard(
    song: com.melodia.app.data.model.Song,
    width: androidx.compose.ui.unit.Dp = 132.dp,
    onClick: () -> Unit
) {
    val selection = LocalSelection.current
    val marked = selection.selecting && selection.selected.contains(song.id)
    Column(
        Modifier.width(width)
            .semantics {
                selected = marked
                role = Role.Button
            }
            .then(pressClick({
                if (selection.selecting) selection.toggle(song.id) else onClick()
            }, min = 0.985f, label = if (selection.selecting) "Seleccionar ${song.title}" else "Reproducir ${song.title}"))
    ) {
        SongArt(song.albumId, width, RoundedCornerShape(com.melodia.app.ui.theme.Ds.RCard),
            artUrl = song.artUrl, iconSize = 28.dp, active = marked)
        Spacer(Modifier.height(6.dp))
        Text(song.title, style = com.melodia.app.ui.theme.Ds.Body, color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist.ifBlank { "Desconocido" },
            style = com.melodia.app.ui.theme.Ds.Sub, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Movimiento breve y finito. Modo seguro evita desplazamientos decorativos. */
@Composable
fun Modifier.polishedEntrance(): Modifier {
    val context = LocalContext.current
    val reduced = remember(context) { SafeMode.isOn(context, SafeMode.NO_EFFECTS) }
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) progress.snapTo(1f) else progress.animateTo(1f, tween(200))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8.dp.toPx()
    }
}

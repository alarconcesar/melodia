package com.melodia.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.SongArt
import com.melodia.app.ui.theme.Ds

/** Contenido puro de la ficha: los archivos/permisos permanecen en su wrapper. */
@Composable
fun SongInfoContent(
    song: Song, isFav: Boolean, folder: String, added: String,
    format: String?, sizeLabel: String?,
    onPlay: () -> Unit, onAdd: () -> Unit, onFav: () -> Unit,
    onNext: () -> Unit, onEnd: () -> Unit, onEdit: () -> Unit,
    onShare: () -> Unit, onSelect: () -> Unit, onExclude: () -> Unit,
    onCopyPath: () -> Unit, onClose: () -> Unit,
    onArtist: ((String) -> Unit)? = null, onAlbum: ((String) -> Unit)? = null,
    local: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    // Mostrar lo que sabemos, sin deducir nombres de artista a partir de guiones.
    val title = remember(song.title) {
        song.title.replace(Regex("""\s*\[[A-Za-z0-9_-]{11}]\s*$"""), "").trim().ifBlank { "Sin título" }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Información del tema", style = Ds.Title, color = cs.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() })
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cerrar información", tint = cs.onSurfaceVariant) }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        SongArt(song.albumId, 72.dp, Ds.RowShape, artUrl = song.artUrl)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = Ds.Title.copy(fontSize = 19.sp), color = cs.onSurface,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(song.artist.ifBlank { "Desconocido" }, style = Ds.Body, color = cs.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetadataPill(fmt(song.durationMs))
        format?.let { MetadataPill(it) }
        sizeLabel?.let { MetadataPill(it) }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onPlay, shape = Ds.RowShape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
        Text("Reproducir", color = cs.onPrimary)
    }
    if (local) {
        Spacer(Modifier.height(8.dp))
        // Dos filas de tres acciones: el texto no queda comprimido en cinco celdas de 60 dp.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoTile(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (isFav) "En favoritos" else "Favorito", Modifier.weight(1f), isFav, onFav)
            InfoTile(Icons.Default.PlaylistAdd, "A lista", Modifier.weight(1f), onClick = onAdd)
            InfoTile(Icons.Default.Share, "Compartir", Modifier.weight(1f), onClick = onShare)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoTile(Icons.Default.QueueMusic, "Siguiente", Modifier.weight(1f), onClick = onNext)
            InfoTile(Icons.Default.LowPriority, "Al final", Modifier.weight(1f), onClick = onEnd)
            InfoTile(Icons.Default.Edit, "Editar", Modifier.weight(1f), onClick = onEdit)
        }
    }
    Spacer(Modifier.height(20.dp))
    Text("Detalles", style = Ds.Title, color = cs.onSurface, modifier = Modifier.semantics { heading() })
    InfoDetail("Artista", song.artist.ifBlank { "Desconocido" }, onArtist?.let { action -> { action(song.artist) } })
    if (song.album.isNotBlank()) InfoDetail("Álbum", song.album, onAlbum?.let { action -> { action(song.album) } })
    if (local) {
        InfoDetail("Carpeta", folder)
        InfoDetail("Agregado", added)
        Spacer(Modifier.height(14.dp))
        Surface(shape = Ds.RowShape, color = cs.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("Archivo", style = Ds.Micro, color = cs.onSurfaceVariant)
                Text(song.dataPath.ifBlank { "Ruta no disponible" }, style = Ds.Sub, color = cs.onSurface,
                    modifier = Modifier.padding(top = 6.dp))
                TextButton(onClick = onCopyPath, enabled = song.dataPath.isNotBlank()) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text("Copiar ruta")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSelect, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Default.Checklist, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text("Elegir varios")
        }
        if (song.dataPath.isNotBlank()) {
            TextButton(onClick = onExclude, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = cs.onSurfaceVariant)) {
                Icon(Icons.Default.FolderOff, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Excluir carpeta")
            }
        }
    }
}

@Composable
private fun MetadataPill(value: String) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(8.dp), color = cs.surfaceVariant) {
        Text(value, style = Ds.Sub, color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1)
    }
}

@Composable
private fun InfoTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    modifier: Modifier, active: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = Ds.RowShape, color = if (active) cs.primary.copy(alpha = 0.12f) else cs.surfaceVariant,
        modifier = modifier) {
        Column(Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(21.dp), tint = if (active) cs.primary else cs.onSurfaceVariant)
            Text(label, style = Ds.Sub.copy(fontWeight = FontWeight.Medium),
                color = if (active) cs.primary else cs.onSurface, maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InfoDetail(label: String, value: String, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().then(if (onClick == null) Modifier else
        Modifier.clickable(role = Role.Button, onClick = onClick)).heightIn(min = 48.dp).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = Ds.Body, color = if (onClick != null) cs.primary else cs.onSurface,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (onClick != null) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
    }
}

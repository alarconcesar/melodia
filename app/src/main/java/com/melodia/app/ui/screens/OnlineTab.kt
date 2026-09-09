package com.melodia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melodia.app.MainViewModel
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.ds.MelodiaSpinner
import com.melodia.app.ui.theme.Ds

private fun fmtDur(sec: Long): String {
    if (sec < 0) return "--:--"
    val m = sec / 60
    val s = sec % 60
    return if (m >= 60) "${m / 60}:${(m % 60).toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnlineTab(vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    val query by vm.ytQuery.collectAsState()
    val results by vm.ytResults.collectAsState()
    val loading by vm.ytLoading.collectAsState()
    val error by vm.ytError.collectAsState()
    val dl by vm.dlProgress.collectAsState()
    val busy by vm.busyYt.collectAsState()
    val recent by vm.ytRecent.collectAsState()
    val history by vm.ytHistory.collectAsState()
    val hasMore by vm.ytHasMore.collectAsState()
    val moreLoading by vm.ytMoreLoading.collectAsState()
    val highQ by vm.ytHighQuality.collectAsState()
    val saver by vm.dataSaver.collectAsState()
    val dlHist by vm.dlHistory.collectAsState()
    var dlPick by remember { mutableStateOf<YtTrack?>(null) }
    var showDls by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshDlHistory() }
    if (showDls) DownloadsSheet(vm, onDismiss = { showDls = false })
    dlPick?.let { track ->
        AlertDialog(
            onDismissRequest = { dlPick = null },
            title = { Text("Calidad de descarga", color = cs.onBackground) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(track.title, style = Ds.Body, color = cs.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    ToggleChip(
                        selected = highQ, text = "Alta — mejor sonido, archivo grande",
                        icon = Icons.Default.Download,
                        onClick = { vm.setYtQuality(true); vm.downloadYt(track, true); dlPick = null }
                    )
                    ToggleChip(
                        selected = !highQ, text = "Estándar — liviana, ahorra espacio",
                        icon = Icons.Default.Savings,
                        onClick = { vm.setYtQuality(false); vm.downloadYt(track, false); dlPick = null }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { dlPick = null }) { Text("Cancelar") } }
        )
    }
    val kb = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Online", style = Ds.Display, color = cs.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Busca en YouTube, escucha al instante o guarda en tu música",
            style = Ds.Body, color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToggleChip(
                selected = saver, text = "Ahorro de datos",
                icon = Icons.Default.Savings, onClick = { vm.setDataSaver(!saver) }
            )
            Spacer(Modifier.width(8.dp))
            ToggleChip(
                selected = false,
                text = if (dlHist.isEmpty() && dl.values.isEmpty()) "Descargas" else "Descargas · ${dl.size + dlHist.size}",
                icon = Icons.Default.History, onClick = { showDls = true }
            )
        }
        Spacer(Modifier.height(6.dp))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { vm.setYtQuery(it) },
                placeholder = { Text("Canción, artista…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { kb?.hide(); vm.searchYt() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = cs.primary, modifier = Modifier.size(52.dp)) {
                Box(Modifier.clickable(enabled = !loading) { kb?.hide(); vm.searchYt() }, contentAlignment = Alignment.Center) {
                    if (loading) MelodiaSpinner(size = 22.dp, width = 2.dp, color = cs.surface)
                    else Icon(Icons.Default.Search, null, tint = cs.surface)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                MelodiaSpinner()
            }
            error != null && results.isEmpty() -> Column(Modifier.padding(vertical = 16.dp)) {
                Text(error ?: "", style = Ds.Body, color = cs.error)
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp), color = cs.primary,
                    modifier = Modifier.clickable { vm.searchYt() }
                ) {
                    Text(
                        "Reintentar", style = Ds.Body, color = cs.surface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
            results.isEmpty() -> {
                if (recent.isEmpty() && history.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                    ) {
                        Box(
                            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                                .background(cs.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(32.dp), tint = cs.primary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Busca música en YouTube", style = Ds.Title, color = cs.onBackground)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Escribe una canción o un artista para empezar",
                            style = Ds.Body, color = cs.onSurfaceVariant
                        )
                    }
                }
                if (recent.isNotEmpty()) {
                    Text("Recientes", style = Ds.Sub, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recent.forEach { term ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cs.surfaceVariant,
                                modifier = Modifier.clickable {
                                    vm.setYtQuery(term); vm.searchYt(term)
                                }
                            ) {
                                Text(
                                    term, style = Ds.Body, color = cs.onBackground,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Escuchado online", style = Ds.Sub, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(history, key = { "h-${it.id}" }) { track ->
                            Surface(
                                shape = RoundedCornerShape(14.dp), color = cs.surface,
                                modifier = Modifier.width(190.dp).clickable { vm.playYt(track) }
                            ) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (track.thumbUrl != null) {
                                        AsyncImage(
                                            model = track.thumbUrl, contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                                .background(cs.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            track.title, style = Ds.Body, color = cs.onBackground,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.artist.ifBlank { "YouTube" } +
                                                if (track.durationSec > 0) " · ${fmtDur(track.durationSec)}" else "",
                                            style = Ds.Sub,
                                            color = cs.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                error?.let {
                    Text(it, style = Ds.Sub, color = cs.error, modifier = Modifier.padding(bottom = 6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${results.size} ${if (results.size == 1) "resultado" else "resultados"}",
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = cs.surfaceVariant,
                        modifier = Modifier.clickable { vm.downloadAllYt() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Default.Download, null, Modifier.size(16.dp), tint = cs.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Descargar todo", style = Ds.Body, color = cs.primary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 200.dp)
                ) {
                    items(results, key = { it.id }) { track ->
                        YtRow(
                            track = track,
                            progress = dl[track.id],
                            busy = busy == track.id,
                            onPlay = { vm.playYt(track) },
                            onDownload = { dlPick = track }
                        )
                    }
                    if (hasMore || moreLoading) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (moreLoading) {
                                    MelodiaSpinner(size = 28.dp, width = 3.dp)
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = cs.surfaceVariant,
                                        modifier = Modifier.clickable { vm.loadMoreYt() }
                                    ) {
                                        Text(
                                            "Ver más", style = Ds.Body, color = cs.primary,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Descargas: progreso en vivo + historial con calidad y fecha. */
@Composable
fun DownloadsSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dl by vm.dlProgress.collectAsState()
    val hist by vm.dlHistory.collectAsState()
    val results by vm.ytResults.collectAsState()
    val byId = remember(results) { results.associateBy { it.id } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descargas", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (dl.isEmpty() && hist.isEmpty()) {
                    Text("Aún no descargas nada — elige un tema y guárdalo en tu música", style = Ds.Body, color = cs.onSurfaceVariant)
                }
                dl.forEach { (id, p) ->
                    val t = byId[id]
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            t?.title ?: "Descargando…", style = Ds.Body, color = cs.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = p.coerceIn(0f, 1f),
                                modifier = Modifier.weight(1f),
                                color = cs.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${(p * 100).toInt()}%", style = Ds.Sub, color = cs.primary)
                        }
                    }
                }
                if (hist.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        Text("Historial", style = Ds.Title, color = cs.onBackground, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.clearDlHistory() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Borrar historial", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        hist.forEach { r ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Text(
                                    r.title, style = Ds.Body, color = cs.onBackground,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${r.artist.ifBlank { "YouTube" }} · ${if (r.high) "Alta" else "Estándar"} · ${
                                        try {
                                            java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(r.at))
                                        } catch (_: Exception) { "" }
                                    }",
                                    style = Ds.Sub, color = cs.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun YtRow(
    track: YtTrack,
    progress: Float?,
    busy: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = cs.surface, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.thumbUrl != null) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))) {
                        AsyncImage(
                            model = track.thumbUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (track.durationSec > 0) {
                            Box(
                                Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    fmtDur(track.durationSec), color = Color.White,
                                    style = Ds.Sub.copy(fontSize = 10.sp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                            .background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = cs.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title, style = Ds.Body, color = cs.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${track.artist.ifBlank { "YouTube" }} · ${fmtDur(track.durationSec)}",
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (busy) {
                    MelodiaSpinner(size = 24.dp, width = 2.dp)
                } else {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, "Escuchar", tint = cs.primary, modifier = Modifier.size(28.dp))
                    }
                }
                if (progress == null) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, "Descargar", tint = cs.onSurfaceVariant)
                    }
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    color = cs.primary,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

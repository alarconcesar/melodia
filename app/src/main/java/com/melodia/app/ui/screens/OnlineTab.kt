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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

/**
 * Online · rediseño minimal (4.8).
 *
 * Una sola cosa por pantalla: buscar. El resto (descargas, importar, calidad,
 * ahorro de datos) vive en el menú ⋮; los resultados son filas planas, no
 * tarjetas dentro de tarjetas.
 *
 * El contenido vive en [OnlineTabContent], sin ViewModel, para poder componerlo
 * en tests JVM sin Room ni servicio de audio.
 */
@Composable
fun OnlineTab(vm: MainViewModel) {
    val query by vm.ytQuery.collectAsState()
    val results by vm.ytResults.collectAsState()
    val loading by vm.ytLoading.collectAsState()
    val error by vm.ytError.collectAsState()
    val errorRetry by vm.ytErrorRetry.collectAsState()
    val dl by vm.dlProgress.collectAsState()
    val busy by vm.busyYt.collectAsState()
    val recent by vm.ytRecent.collectAsState()
    val history by vm.ytHistory.collectAsState()
    val hasMore by vm.ytHasMore.collectAsState()
    val moreLoading by vm.ytMoreLoading.collectAsState()
    val highQ by vm.ytHighQuality.collectAsState()
    val saver by vm.dataSaver.collectAsState()

    var dlPick by remember { mutableStateOf<YtTrack?>(null) }
    var showDls by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshDlHistory() }

    OnlineTabContent(
        query = query,
        results = results,
        loading = loading,
        error = error,
        errorRetry = errorRetry,
        dlProgress = dl,
        busyId = busy,
        recent = recent,
        history = history,
        hasMore = hasMore,
        moreLoading = moreLoading,
        saver = saver,
        highQ = highQ,
        onQueryChange = vm::setYtQuery,
        onSearch = { vm.searchYt() },
        onClearQuery = { vm.setYtQuery("") },
        onPlay = { vm.playYt(it) },
        onDownload = { dlPick = it },
        onDownloadAll = { vm.downloadAllYt() },
        onLoadMore = { vm.loadMoreYt() },
        onOpenDownloads = { showDls = true },
        onOpenImport = { vm.clearYtImport(); showImport = true },
        onToggleSaver = { vm.setDataSaver(!saver) },
        onToggleQuality = { vm.setYtQuality(!highQ) }
    )

    if (showDls) DownloadsSheet(vm, onDismiss = { showDls = false })
    if (showImport) ImportPlaylistDialog(vm, onClose = { showImport = false }, onQueued = { showDls = true; showImport = false })
    dlPick?.let { track ->
        QualityDialog(
            track = track,
            highQ = highQ,
            onPick = { high ->
                vm.setYtQuality(high)
                vm.downloadYt(track, high)
                dlPick = null
            },
            onDismiss = { dlPick = null }
        )
    }
}

/** Contenido sin ViewModel: todo entra por parámetros (testeable en JVM). */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnlineTabContent(
    query: String,
    results: List<YtTrack>,
    loading: Boolean,
    error: String?,
    errorRetry: Boolean,
    dlProgress: Map<String, Float>,
    busyId: String?,
    recent: List<String>,
    history: List<YtTrack>,
    hasMore: Boolean,
    moreLoading: Boolean,
    saver: Boolean,
    highQ: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearQuery: () -> Unit,
    onPlay: (YtTrack) -> Unit,
    onDownload: (YtTrack) -> Unit,
    onDownloadAll: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenImport: () -> Unit,
    onToggleSaver: () -> Unit,
    onToggleQuality: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val kb = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        // Cabecera: título + menú. Nada más compite con la búsqueda.
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Online", style = Ds.Title, color = cs.onBackground, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, "Opciones", Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Descargas") },
                        leadingIcon = { Icon(Icons.Default.Download, null) },
                        onClick = { menu = false; onOpenDownloads() }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar playlist") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                        onClick = { menu = false; onOpenImport() }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Ahorro de datos") },
                        trailingIcon = { if (saver) Icon(Icons.Default.Check, null) },
                        onClick = { onToggleSaver() }
                    )
                    DropdownMenuItem(
                        text = { Text("Calidad alta") },
                        trailingIcon = { if (highQ) Icon(Icons.Default.Check, null) },
                        onClick = { onToggleQuality() }
                    )
                }
            }
        }

        // Buscador: campo plano, lupa a la izquierda, limpiar a la derecha.
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar en YouTube", style = Ds.Body) },
            singleLine = true,
            shape = RoundedCornerShape(Ds.RRow),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = cs.surface,
                unfocusedContainerColor = cs.surface,
                disabledContainerColor = cs.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            leadingIcon = {
                IconButton(onClick = { kb?.hide(); onSearch() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Search, "Buscar", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                }
            },
            trailingIcon = {
                when {
                    loading -> Box(Modifier.padding(end = 10.dp)) { MelodiaSpinner(size = 16.dp, width = 2.dp) }
                    query.isNotEmpty() -> IconButton(onClick = onClearQuery, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { kb?.hide(); onSearch() }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 200.dp)
        ) {
            when {
                loading && results.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        MelodiaSpinner()
                    }
                }

                error != null && results.isEmpty() -> item {
                    Column(Modifier.padding(top = 24.dp)) {
                        Text(
                            error, style = Ds.Body,
                            color = if (errorRetry) cs.error else cs.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (errorRetry) {
                            Text(
                                "Reintentar",
                                style = Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                                color = cs.primary,
                                modifier = Modifier.clickable { onSearch() }.padding(vertical = 4.dp)
                            )
                        } else {
                            Text("Cambia las palabras y busca de nuevo.", style = Ds.Sub, color = cs.onSurfaceVariant)
                        }
                    }
                }

                results.isEmpty() -> {
                    if (recent.isEmpty() && history.isEmpty()) {
                        item { OnlineEmptyState() }
                    }
                    if (recent.isNotEmpty()) {
                        item {
                            Text(
                                "Recientes", style = Ds.Sub, color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                            )
                        }
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recent.forEach { term ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = cs.surfaceVariant,
                                        modifier = Modifier.clickable { onQueryChange(term); onSearch() }
                                    ) {
                                        Text(
                                            term, style = Ds.Sub, color = cs.onBackground,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (history.isNotEmpty()) {
                        item {
                            Text(
                                "Escuchado online", style = Ds.Sub, color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(history, key = { "h-${it.id}" }) { track ->
                                    Column(Modifier.width(124.dp).clickable { onPlay(track) }) {
                                        OnlineThumb(track, 124.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            track.title, style = Ds.Body, color = cs.onBackground,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.artist.ifBlank { "YouTube" }, style = Ds.Sub,
                                            color = cs.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${results.size} ${if (results.size == 1) "resultado" else "resultados"}",
                                style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Descargar todo",
                                style = Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                                color = cs.primary,
                                modifier = Modifier.clickable { onDownloadAll() }.padding(4.dp)
                            )
                        }
                    }
                    items(results, key = { it.id }) { track ->
                        YtRowFlat(
                            track = track,
                            progress = dlProgress[track.id],
                            busy = busyId == track.id,
                            onPlay = { onPlay(track) },
                            onDownload = { onDownload(track) }
                        )
                    }
                    if (hasMore || moreLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                if (moreLoading) {
                                    MelodiaSpinner(size = 24.dp, width = 2.dp)
                                } else {
                                    Text(
                                        "Ver más",
                                        style = Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                                        color = cs.primary,
                                        modifier = Modifier.clickable { onLoadMore() }.padding(8.dp)
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

@Composable
private fun OnlineEmptyState() {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Search, null, Modifier.size(26.dp), tint = cs.primary) }
        Spacer(Modifier.height(14.dp))
        Text("Busca música en YouTube", style = Ds.Title, color = cs.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Escribe una canción o un artista para empezar",
            style = Ds.Sub, color = cs.onSurfaceVariant
        )
    }
}

/** Fila plana de resultado: arte, título, artista · duración y descarga. */
@Composable
private fun YtRowFlat(
    track: YtTrack,
    progress: Float?,
    busy: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OnlineThumb(track, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title, style = Ds.Body, color = cs.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist.ifBlank { "YouTube" } +
                        if (track.durationSec > 0) " · ${fmtDur(track.durationSec)}" else "",
                    style = Ds.Sub, color = cs.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            when {
                busy -> Box(Modifier.padding(horizontal = 8.dp)) { MelodiaSpinner(size = 20.dp, width = 2.dp) }
                progress == null -> IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Download, "Descargar", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                }
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                color = cs.primary,
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

/** Miniatura con duración discreta abajo a la derecha. */
@Composable
private fun OnlineThumb(track: YtTrack, size: androidx.compose.ui.unit.Dp) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(RoundedCornerShape(Ds.RCard))) {
        if (track.thumbUrl != null) {
            AsyncImage(
                model = track.thumbUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
            }
        }
        if (track.durationSec > 0 && size > 60.dp) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(fmtDur(track.durationSec), color = Color.White, style = Ds.Sub.copy(fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun ImportPlaylistDialog(vm: MainViewModel, onClose: () -> Unit, onQueued: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val importState by vm.ytImport.collectAsState()
    var url by remember { mutableStateOf("") }
    val busy = importState == "…"
    val ok = importState?.startsWith("OK|") == true
    LaunchedEffect(ok) { if (ok) onQueued() }
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text("Importar playlist", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Pega el link de una playlist de YouTube y se encola entera.",
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = url,
                    onValueChange = { url = it; vm.clearYtImport() },
                    placeholder = { Text("https://youtube.com/playlist?list=…", style = Ds.Sub) },
                    singleLine = true,
                    shape = RoundedCornerShape(Ds.RRow),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = cs.surfaceVariant,
                        unfocusedContainerColor = cs.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                val imp = importState
                if (imp != null && imp != "…") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (ok) {
                            val parts = imp.split("|")
                            "«${parts.getOrNull(2) ?: "Playlist"}»: ${parts.getOrNull(1) ?: "?"} temas en cola"
                        } else imp,
                        style = Ds.Sub,
                        color = if (ok) cs.primary else cs.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.importYtPlaylist(url) },
                enabled = url.isNotBlank() && !busy
            ) { Text(if (busy) "Importando…" else "Importar") }
        },
        dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Cancelar") } }
    )
}

@Composable
private fun QualityDialog(
    track: YtTrack,
    highQ: Boolean,
    onPick: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calidad de descarga", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    track.title, style = Ds.Body, color = cs.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                QualityOption(
                    title = "Alta", subtitle = "Mejor sonido, archivo más grande",
                    selected = highQ, onClick = { onPick(true) }
                )
                QualityOption(
                    title = "Estándar", subtitle = "Liviana, ahorra espacio",
                    selected = !highQ, onClick = { onPick(false) }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun QualityOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(Ds.RRow),
        color = if (selected) cs.primary.copy(alpha = 0.12f) else cs.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Ds.Body, color = if (selected) cs.primary else cs.onBackground)
                Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = cs.primary)
        }
    }
}

/** Descargas: cola con pausa/reintento + historial con calidad y fecha. */
@Composable
fun DownloadsSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val jobs by vm.dlJobs.collectAsState()
    val hist by vm.dlHistory.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descargas", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (jobs.isEmpty() && hist.isEmpty()) {
                    Text("Aún no descargas nada — elige un tema y guárdalo en tu música", style = Ds.Body, color = cs.onSurfaceVariant)
                }
                if (jobs.isNotEmpty()) {
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        jobs.forEach { j ->
                            DlJobRow(
                                job = j,
                                onPause = { vm.pauseDl(j.track.id) },
                                onResume = { vm.resumeDl(j.track.id) },
                                onCancel = { vm.cancelDl(j.track.id) }
                            )
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

private fun mb(b: Long): String = "%.1f MB".format(b / 1048576f)

/** Fila de descarga: progreso, pausa, reintento y cancelar. */
@Composable
private fun DlJobRow(
    job: com.melodia.app.data.online.DlJob,
    onPause: () -> Unit, onResume: () -> Unit, onCancel: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val st = job.status
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            job.track.title, style = Ds.Body, color = cs.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            "${job.track.artist.ifBlank { "YouTube" }} · ${if (job.high) "Alta" else "Estándar"}" +
                when (st) {
                    com.melodia.app.data.online.DlStatus.QUEUED -> " · En cola"
                    com.melodia.app.data.online.DlStatus.PAUSED ->
                        if (job.total > 0) " · Pausada · ${mb(job.downloaded)}/${mb(job.total)}" else " · Pausada"
                    com.melodia.app.data.online.DlStatus.ERROR -> " · ${job.error ?: "Falló"}"
                    com.melodia.app.data.online.DlStatus.DONE -> " · Guardando…"
                    else -> if (job.total > 0) " · ${mb(job.downloaded)}/${mb(job.total)}" else ""
                },
            style = Ds.Sub,
            color = if (st == com.melodia.app.data.online.DlStatus.ERROR) cs.error else cs.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (st == com.melodia.app.data.online.DlStatus.ACTIVE ||
                st == com.melodia.app.data.online.DlStatus.QUEUED ||
                st == com.melodia.app.data.online.DlStatus.PAUSED
            ) {
                LinearProgressIndicator(
                    progress = job.progress.coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f),
                    color = cs.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("${(job.progress * 100).toInt()}%", style = Ds.Sub, color = cs.primary)
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            when (st) {
                com.melodia.app.data.online.DlStatus.ACTIVE,
                com.melodia.app.data.online.DlStatus.QUEUED -> IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Pause, "Pausar", Modifier.size(18.dp), tint = cs.onBackground)
                }
                com.melodia.app.data.online.DlStatus.PAUSED,
                com.melodia.app.data.online.DlStatus.ERROR -> IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, "Reanudar", Modifier.size(18.dp), tint = cs.primary)
                }
                else -> {}
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Quitar", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
            }
        }
    }
}

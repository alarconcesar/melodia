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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.melodia.app.data.online.YtSearchItem
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.ds.EmptyState
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
    val loadingCol by vm.loadingCol.collectAsState()
    val recent by vm.ytRecent.collectAsState()
    val history by vm.ytHistory.collectAsState()
    val hasMore by vm.ytHasMore.collectAsState()
    val moreLoading by vm.ytMoreLoading.collectAsState()
    val highQ by vm.ytHighQuality.collectAsState()
    val saver by vm.dataSaver.collectAsState()
    val notice by vm.ytMsg.collectAsState()

    var dlPickId by rememberSaveable { mutableStateOf<String?>(null) }
    var dlPickTitle by rememberSaveable { mutableStateOf("") }
    var dlPickArtist by rememberSaveable { mutableStateOf("") }
    var dlPickDur by rememberSaveable { mutableStateOf(-1L) }
    var dlPickThumb by rememberSaveable { mutableStateOf<String?>(null) }
    var dlPickUrl by rememberSaveable { mutableStateOf("") }
    val dlPick: YtTrack? = dlPickId?.let {
        YtTrack(it, dlPickTitle, dlPickArtist, dlPickDur, dlPickThumb, dlPickUrl)
    }
    fun setDlPick(t: YtTrack?) {
        dlPickId = t?.id; dlPickTitle = t?.title ?: ""; dlPickArtist = t?.artist ?: ""
        dlPickDur = t?.durationSec ?: -1L; dlPickThumb = t?.thumbUrl; dlPickUrl = t?.url ?: ""
    }
    var showImport by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshDlHistory() }

    OnlineTabContent(
        query = query,
        items = results,
        loading = loading,
        error = error,
        errorRetry = errorRetry,
        dlProgress = dl,
        busyId = busy,
        busyColId = loadingCol,
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
        onDownload = { setDlPick(it) },
        onDownloadAll = { vm.downloadAllYt() },
        onLoadMore = { vm.loadMoreYt() },
        onOpenDownloads = { vm.openDownloads() },
        onOpenImport = { vm.clearYtImport(); showImport = true },
        onToggleSaver = { vm.setDataSaver(!saver) },
        onToggleQuality = { vm.setYtQuality(!highQ) },
        notice = notice,
        onClearNotice = vm::clearYtMsg,
        onDownloadCollection = { vm.downloadYtCollection(it) },
        onOpenCollection = { vm.openYtCollection(it) }
    )

    if (showImport) ImportPlaylistDialog(vm, onClose = { showImport = false }, onQueued = { vm.openDownloads(); showImport = false })
    dlPick?.let { track ->
        QualityDialog(
            track = track,
            highQ = highQ,
            onPick = { high ->
                vm.setYtQuality(high)
                vm.downloadYt(track, high)
                setDlPick(null)
            },
            onDismiss = { setDlPick(null) }
        )
    }
}

/** Estimado de una colección en MB (m4a ~2 MB/min). */
internal fun estimadoColMb(tracks: List<YtTrack>): Long =
    (tracks.sumOf { it.durationSec.coerceAtLeast(0) } / 30).coerceAtLeast(1)

/** Contenido sin ViewModel: todo entra por parámetros (testeable en JVM). */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnlineTabContent(
    query: String,
    items: List<YtSearchItem>,
    loading: Boolean,
    error: String?,
    errorRetry: Boolean,
    dlProgress: Map<String, Float>,
    busyId: String?,
    busyColId: String? = null,
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
    onToggleQuality: () -> Unit,
    notice: String? = null,
    onClearNotice: () -> Unit = {},
    onDownloadCollection: (YtSearchItem.Collection) -> Unit = {},
    onOpenCollection: (YtSearchItem.Collection) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val kb = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        // Buscador + menú ⋮ en la misma línea: el título "Online" repetía lo que
        // ya dice la pestaña seleccionada en la nav.
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
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

        if (notice != null) {
            Text(
                notice, style = Ds.Sub, color = cs.primary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            LaunchedEffect(notice) {
                kotlinx.coroutines.delay(4500)
                onClearNotice()
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 200.dp)
        ) {
            if (loading && items.isNotEmpty()) item {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            when {
                loading && items.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        MelodiaSpinner()
                    }
                }

                error != null && items.isEmpty() -> item {
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

                items.isEmpty() -> {
                    if (recent.isEmpty() && history.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "Busca música en YouTube",
                                subtitle = "Escribe una canción o un artista para empezar."
                            )
                        }
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
                                itemsIndexed(history, key = { i, t -> "h-$i-${t.id}" }) { _, track ->
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
                                "${items.size} ${if (items.size == 1) "resultado" else "resultados"}",
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
                    itemsIndexed(items, key = { i, item -> "${item.key}-$i" }) { _, item ->
                        when (item) {
                            is YtSearchItem.Track -> YtRowFlat(
                                track = item.track,
                                progress = dlProgress[item.track.id],
                                busy = busyId == item.track.id,
                                onPlay = { onPlay(item.track) },
                                onDownload = { onDownload(item.track) }
                            )
                            is YtSearchItem.Collection -> YtCollectionRow(
                                item = item,
                                busy = busyId == item.key || busyColId == item.key,
                                onOpen = { onOpenCollection(item) },
                                onDownload = { onDownloadCollection(item) }
                            )
                        }
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

/** Fila plana de resultado: arte, título, artista · duración y descarga. */
@Composable
fun YtRowFlat(
    track: YtTrack,
    progress: Float?,
    busy: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 5.dp)) {
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

/** Playlist o álbum: el toque abre su contenido, el icono la descarga entera. */
@Composable
fun YtCollectionRow(
    item: YtSearchItem.Collection,
    busy: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(Ds.RCard))) {
            if (item.thumbUrl != null) {
                AsyncImage(
                    model = item.thumbUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QueueMusic, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                    .padding(3.dp)
            ) {
                Icon(Icons.Default.QueueMusic, null, Modifier.size(11.dp), tint = Color.White)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title, style = Ds.Body, color = cs.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    item.uploader.ifBlank { null },
                    if (item.count > 0) "${item.count} temas" else null,
                    "Playlist o álbum"
                ).joinToString(" · "),
                style = Ds.Sub, color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (busy) {
            Box(Modifier.padding(horizontal = 8.dp)) { MelodiaSpinner(size = 20.dp, width = 2.dp) }
        } else {
            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Download, "Descargar entera", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
            }
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
    var url by rememberSaveable { mutableStateOf("") }
    val busy = importState == "…"
    val ok = importState?.startsWith("OK|") == true
    var abierto by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { abierto = false } }
    LaunchedEffect(ok) { if (ok && abierto) onQueued() }
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
                            "«${parts.getOrNull(2) ?: "Playlist"}»: ${parts.getOrNull(1) ?: "?"} temas en cola" +
                                (if (parts.size > 3) " (lista recortada)" else "")
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

/**
 * Lista de temas de una playlist/álbum. La clave incluye el índice porque una
 * lista de YouTube puede repetir el mismo video y LazyColumn no admite claves
 * duplicadas (crash "Key ... was already used").
 */
@Composable
fun YtPreviewList(
    tracks: List<YtTrack>,
    onPlay: (YtTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(modifier.fillMaxWidth()) {
        itemsIndexed(tracks, key = { i, t -> "pv-$i-${t.id}" }) { _, t ->
            Row(
                Modifier.fillMaxWidth().clickable { onPlay(t) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnlineThumb(t, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title, style = Ds.Body, color = cs.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        t.artist.ifBlank { "YouTube" } +
                            if (t.durationSec > 0) " · ${fmtDur(t.durationSec)}" else "",
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp), tint = cs.primary)
            }
        }
    }
}

/**
 * Vista previa de una playlist/álbum de YouTube: se abren sus temas para
 * escuchar uno o descargar la lista entera. Antes tocar la fila descargaba
 * directamente y no había forma de ver qué contenía.
 */
@Composable
fun YtCollectionSheet(vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    val preview by vm.ytPreview.collectAsState()
    val p = preview ?: return
    AlertDialog(
        onDismissRequest = { vm.closeYtPreview() },
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    p.title, style = Ds.Title, color = cs.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        p.loading -> "Cargando temas…"
                        p.tracks.isNotEmpty() -> "${p.tracks.size} temas" + if (p.truncated) " (lista recortada)" else ""
                        else -> "Playlist o álbum"
                    },
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
            }
        },
        text = {
            Box(Modifier.fillMaxWidth().heightIn(max = 380.dp, min = 80.dp)) {
                when {
                    p.loading -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        MelodiaSpinner(size = 24.dp, width = 2.dp)
                    }
                    p.error != null -> Text(p.error, style = Ds.Body, color = cs.error)
                    else -> YtPreviewList(
                        tracks = p.tracks,
                        onPlay = { t -> vm.closeYtPreview(); vm.playYt(t) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.downloadYtPreview() },
                enabled = p.tracks.isNotEmpty()
            ) { Text("Descargar todo") }
        },
        dismissButton = { TextButton(onClick = { vm.closeYtPreview() }) { Text("Cerrar") } }
    )
}

/**
 * Botón de descargas de la barra superior: anillo con el progreso agregado de
 * todo lo que está bajando y acceso a la hoja con la cola completa.
 */
@Composable
fun DownloadsButton(jobs: List<com.melodia.app.data.online.DlJob>, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val activos = jobs.filter {
        it.status == com.melodia.app.data.online.DlStatus.ACTIVE ||
            it.status == com.melodia.app.data.online.DlStatus.QUEUED ||
            it.status == com.melodia.app.data.online.DlStatus.PAUSED
    }
    val totalBytes = activos.sumOf { it.total }
    val doneBytes = activos.sumOf { it.downloaded }
    val progreso = when {
        activos.isEmpty() -> 0f
        totalBytes > 0L -> (doneBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        else -> activos.map { it.progress }.average().toFloat().coerceIn(0f, 1f)
    }
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (activos.isNotEmpty()) {
            CircularProgressIndicator(
                progress = progreso,
                modifier = Modifier.size(28.dp),
                color = cs.primary,
                trackColor = cs.onSurfaceVariant.copy(alpha = 0.22f),
                strokeWidth = 2.dp
            )
        }
        Box(
            Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Download, "Descargas",
                Modifier.size(if (activos.isNotEmpty()) 15.dp else 20.dp),
                tint = if (activos.isNotEmpty()) cs.primary else cs.onSurfaceVariant
            )
        }
    }
}

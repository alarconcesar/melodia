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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.input.KeyboardType
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
    // Al confirmar con datos se abre Descargas: el diálogo no queda huérfano
    // detrás (tapaba el aviso de temas encolados).
    val pendingColState by vm.pendingCol.collectAsState()
    LaunchedEffect(pendingColState) { if (pendingColState != null) showImport = false }
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
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        // Buscador + menú ⋮ en la misma línea: el título "Online" repetía lo que
        // ya dice la pestaña seleccionada en la nav.
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar en YouTube", style = Ds.Body, color = cs.onSurfaceVariant) },
                textStyle = Ds.Body.copy(color = cs.onSurface),
                singleLine = true,
                shape = RoundedCornerShape(Ds.RRow),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cs.surface,
                    unfocusedContainerColor = cs.surface,
                    disabledContainerColor = cs.surface,
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    cursorColor = cs.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    IconButton(onClick = { focus.clearFocus(); kb?.hide(); onSearch() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Search, "Buscar", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                    }
                },
                // Sin ranura vacía: el placeholder conserva ancho a 320 dp.
                trailingIcon = if (loading || query.isNotEmpty()) {
                    {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            if (loading) {
                                MelodiaSpinner(size = 18.dp, width = 2.dp)
                            } else {
                                IconButton(onClick = onClearQuery, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Close, "Limpiar", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); kb?.hide(); onSearch() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { focus.clearFocus(); kb?.hide(); menu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, "Opciones", Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Descargas", style = Ds.Body, color = cs.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Download, null, tint = cs.onSurfaceVariant) },
                        onClick = { menu = false; onOpenDownloads() }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar lista", style = Ds.Body, color = cs.onSurface) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = cs.onSurfaceVariant) },
                        onClick = { menu = false; onOpenImport() }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Ahorro de datos", style = Ds.Body, color = cs.onSurface) },
                        modifier = Modifier.semantics { stateDescription = if (saver) "Activado" else "Desactivado" },
                        trailingIcon = { if (saver) Icon(Icons.Default.Check, null, tint = cs.primary) },
                        onClick = { onToggleSaver() }
                    )
                    DropdownMenuItem(
                        text = { Text("Calidad alta", style = Ds.Body, color = cs.onSurface) },
                        modifier = Modifier.semantics { stateDescription = if (highQ) "Activada" else "Desactivada" },
                        trailingIcon = { if (highQ) Icon(Icons.Default.Check, null, tint = cs.primary) },
                        onClick = { onToggleQuality() }
                    )
                }
            }
        }

        LaunchedEffect(notice) {
            if (notice != null) {
                kotlinx.coroutines.delay(4500)
                onClearNotice()
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp, bottom = 200.dp)
        ) {
            if (notice != null) item {
                Text(
                    notice, style = Ds.Sub, color = cs.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            if (loading && items.isNotEmpty()) item {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    MelodiaSpinner(size = 18.dp, width = 2.dp)
                }
            }
            // Un fallo de reproducción o de paginación también debe verse con resultados.
            if (error != null && items.isNotEmpty() && !loading) item {
                OnlineSearchError(error, errorRetry, onSearch)
            }
            when {
                loading && items.isEmpty() -> item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MelodiaSpinner(size = 24.dp, width = 2.dp)
                        Text("Buscando en YouTube…", style = Ds.Sub, color = cs.onSurfaceVariant)
                    }
                }

                error != null && items.isEmpty() -> item {
                    OnlineSearchError(error, errorRetry, onSearch)
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
                                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
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
                                        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Buscar $term") {
                                            focus.clearFocus(); kb?.hide(); onQueryChange(term); onSearch()
                                        }.heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            term, style = Ds.Sub, color = cs.onBackground,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
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
                                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(history, key = { i, t -> "h-$i-${t.id}" }) { _, track ->
                                    Column(Modifier.width(124.dp).clickable(role = Role.Button,
                                        onClickLabel = "Reproducir ${track.title}") { onPlay(track) }) {
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
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    "${items.size} ${if (items.size == 1) "resultado" else "resultados"}",
                                    style = Ds.Sub, color = cs.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onDownloadAll, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Descargar todo", style = Ds.Sub, color = cs.primary)
                            }
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
                                    TextButton(onClick = onLoadMore, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Ver más", style = Ds.Sub, color = cs.primary)
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

@Composable
private fun OnlineSearchError(error: String, retry: Boolean, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(error, style = Ds.Body, color = if (retry) cs.error else cs.onSurfaceVariant)
        if (retry) {
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Reintentar", style = Ds.Sub, color = cs.primary)
            }
        } else {
            Text("Cambia las palabras y busca de nuevo.", style = Ds.Sub, color = cs.onSurfaceVariant)
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
    Column(
        modifier.fillMaxWidth()
            .then(com.melodia.app.ui.ds.pressClick(onPlay, min = 0.985f,
                role = Role.Button, label = "Reproducir ${track.title}"))
            .semantics {
                stateDescription = when {
                    busy -> "Preparando audio"
                    progress == null -> "Disponible online"
                    progress <= 0f -> "En cola"
                    progress >= 1f -> "Finalizando descarga"
                    else -> "Descargando"
                }
            }
            .padding(vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OnlineThumb(track, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title, style = Ds.Body, color = cs.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist.ifBlank { "YouTube" } +
                        if (track.durationSec > 0) " · ${fmtDur(track.durationSec)}" else "",
                    style = Ds.Sub, color = cs.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            // Slot fijo: el título no cambia de ancho al empezar una descarga.
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                when {
                    busy || (progress != null && progress <= 0f) -> MelodiaSpinner(size = 20.dp, width = 2.dp)
                    progress == null -> IconButton(onClick = onDownload, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Download, "Descargar", Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
        }
        when {
            busy -> Text("Preparando audio…", style = Ds.Sub, color = cs.primary)
            progress != null && progress <= 0f -> Text("En cola", style = Ds.Sub, color = cs.onSurfaceVariant)
            progress != null && progress >= 1f -> Text("Finalizando descarga…", style = Ds.Sub, color = cs.primary)
        }
        if (progress != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    color = cs.primary, trackColor = cs.surfaceVariant,
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${(progress.coerceIn(0f, 1f) * 100).toInt()}%", style = Ds.Sub, color = cs.primary,
                    maxLines = 1
                )
            }
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
        modifier.fillMaxWidth().clickable(role = Role.Button,
            onClickLabel = "Abrir ${item.title}", onClick = onOpen).padding(vertical = 6.dp),
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
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    item.uploader.ifBlank { null },
                    if (item.count > 0) "${item.count} temas" else null,
                    "Lista o álbum"
                ).joinToString(" · "),
                style = Ds.Sub, color = cs.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (busy) {
                MelodiaSpinner(size = 20.dp, width = 2.dp)
            } else {
                IconButton(onClick = onDownload, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Download, "Descargar entera", Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                }
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
    val importState by vm.ytImport.collectAsState()
    var url by rememberSaveable { mutableStateOf("") }
    val ok = importState?.startsWith("OK|") == true
    var abierto by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { abierto = false } }
    LaunchedEffect(ok) { if (ok && abierto) onQueued() }
    ImportPlaylistDialogContent(
        url = url,
        importState = importState,
        onUrlChange = { url = it; vm.clearYtImport() },
        onImport = { vm.importYtPlaylist(url) },
        onClose = onClose
    )
}

/** Diálogo puro: comparte los estados reales sin arrancar Room ni el reproductor. */
@Composable
internal fun ImportPlaylistDialogContent(
    url: String,
    importState: String?,
    onUrlChange: (String) -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val busy = importState == "…"
    val ok = importState?.startsWith("OK|") == true
    val canImport = url.isNotBlank() && !busy
    fun submit() {
        if (canImport) { focus.clearFocus(); onImport() }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        containerColor = cs.surface,
        tonalElevation = 0.dp,
        title = { Text("Importar lista", style = Ds.Title, color = cs.onSurface) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    "Pega el enlace de una lista de YouTube para añadirla a Descargas.",
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = url,
                    onValueChange = onUrlChange,
                    enabled = !busy,
                    label = { Text("Enlace de la lista", style = Ds.Sub, color = cs.onSurfaceVariant) },
                    placeholder = {
                        Text("https://youtube.com/playlist?list=…", style = Ds.Sub, color = cs.onSurfaceVariant)
                    },
                    textStyle = Ds.Body.copy(color = cs.onSurface),
                    singleLine = true,
                    isError = importState != null && !busy && !ok,
                    shape = Ds.RowShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = cs.surfaceVariant,
                        unfocusedContainerColor = cs.surfaceVariant,
                        disabledContainerColor = cs.surfaceVariant,
                        errorContainerColor = cs.surfaceVariant,
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        disabledTextColor = cs.onSurfaceVariant,
                        errorTextColor = cs.onSurface,
                        cursorColor = cs.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Se importan hasta 100 temas. Puedes pausar la cola desde Descargas.",
                    style = Ds.Sub, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                if (busy) {
                    // LinearProgressIndicator indeterminado de M3 1.1.2 usa
                    // KeyframesSpecConfig.at(), ausente en animation-core 1.6.
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                        MelodiaSpinner(size = 20.dp, width = 2.dp)
                    }
                }
                val imp = importState
                if (imp != null && imp != "…") {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (ok) {
                            val parts = imp.split("|")
                            "«${parts.getOrNull(2) ?: "Lista"}»: ${parts.getOrNull(1) ?: "?"} temas en cola" +
                                (if (parts.size > 3) " (lista recortada)" else "")
                        } else imp,
                        style = Ds.Sub,
                        color = if (ok) cs.primary else cs.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    if (!ok) {
                        Text(
                            "Revisa el enlace y vuelve a pulsar Importar.",
                            style = Ds.Sub, color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = canImport, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (busy) "Importando…" else "Importar", style = Ds.Sub,
                    color = if (canImport) cs.primary else cs.onSurface.copy(alpha = 0.38f))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar", style = Ds.Sub,
                    color = if (busy) cs.onSurface.copy(alpha = 0.38f) else cs.primary)
            }
        }
    )
}

@Composable
internal fun QualityDialog(
    track: YtTrack,
    highQ: Boolean,
    onPick: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surface,
        tonalElevation = 0.dp,
        title = { Text("Calidad de descarga", style = Ds.Title, color = cs.onSurface) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    track.title, style = Ds.Body, color = cs.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text("Elige una calidad para iniciar la descarga.", style = Ds.Sub, color = cs.onSurfaceVariant)
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
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar", style = Ds.Sub, color = cs.primary)
            }
        }
    )
}

@Composable
private fun QualityOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(Ds.RRow),
        color = if (selected) cs.secondaryContainer else cs.surfaceVariant,
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Ds.Body, color = if (selected) cs.primary else cs.onBackground)
                Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant)
            }
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = cs.primary)
            }
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
                Modifier.fillMaxWidth().clickable(role = Role.Button,
                    onClickLabel = "Reproducir ${t.title}") { onPlay(t) }
                    .heightIn(min = 56.dp).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnlineThumb(t, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title, style = Ds.Body, color = cs.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        t.artist.ifBlank { "YouTube" } +
                            if (t.durationSec > 0) " · ${fmtDur(t.durationSec)}" else "",
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
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
    val preview by vm.ytPreview.collectAsState()
    val p = preview ?: return
    YtCollectionPreviewDialog(
        preview = p,
        onClose = { vm.closeYtPreview() },
        onDownload = { vm.downloadYtPreview() },
        onPlay = { t -> vm.closeYtPreview(); vm.playYt(t) }
    )
}

@Composable
internal fun YtCollectionPreviewDialog(
    preview: MainViewModel.YtPreview,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onPlay: (YtTrack) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val p = preview
    val canDownload = p.tracks.isNotEmpty() && !p.loading
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = cs.surface,
        tonalElevation = 0.dp,
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    p.title, style = Ds.Title, color = cs.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    when {
                        p.loading -> "Cargando temas…"
                        p.tracks.isNotEmpty() -> "${p.tracks.size} temas" + if (p.truncated) " (lista recortada)" else ""
                        else -> "Lista o álbum"
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
                    p.error != null -> Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(p.error, style = Ds.Body, color = cs.error)
                        Text("Cierra y vuelve a abrir la lista para reintentar.",
                            style = Ds.Sub, color = cs.onSurfaceVariant)
                    }
                    p.tracks.isEmpty() -> EmptyState(
                        icon = Icons.Default.QueueMusic,
                        title = "Sin temas visibles",
                        subtitle = "Prueba con otra lista o álbum."
                    )
                    else -> YtPreviewList(tracks = p.tracks, onPlay = onPlay)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDownload,
                enabled = canDownload,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Descargar todo", style = Ds.Sub,
                    color = if (canDownload) cs.primary else cs.onSurface.copy(alpha = 0.38f))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cerrar", style = Ds.Sub, color = cs.primary)
            }
        }
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
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
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
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button,
                onClickLabel = "Abrir descargas", onClick = onClick),
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

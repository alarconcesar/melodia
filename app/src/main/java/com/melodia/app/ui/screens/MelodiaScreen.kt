package com.melodia.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melodia.app.MainViewModel
import com.melodia.app.player.YT_STREAM_ID
import com.melodia.app.player.QueueOps
import com.melodia.app.player.PlaybackClock
import com.melodia.app.SafeMode
import com.melodia.app.SortMode
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.DsBackground
import com.melodia.app.ui.ds.EmptyState
import com.melodia.app.ui.ds.HeroCard
import com.melodia.app.ui.ds.HomeSkeleton
import com.melodia.app.ui.ds.MelodiaPillNav
import com.melodia.app.ui.ds.MelodiaSpinner
import com.melodia.app.ui.ds.MicroLabel
import com.melodia.app.ui.ds.MiniPlayerDs
import com.melodia.app.ui.ds.SectionHeader
import com.melodia.app.ui.ds.SongArt
import com.melodia.app.ui.ds.TrackRow
import com.melodia.app.ui.ds.pressClick
import com.melodia.app.ui.ds.songArtModel
import com.melodia.app.ui.theme.Ds
import com.melodia.app.ui.theme.Accents
import com.melodia.app.ui.theme.accentName
import com.melodia.app.ui.theme.saveUiPrefs
import com.melodia.app.ui.theme.isDarkMode
import java.util.Calendar

fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..11 -> "Buenos días"
        in 12..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

private fun toast(ctx: android.content.Context, msg: String) {
    try { android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
}

/** Abre la pantalla de ajustes de la app (para permisos denegados). */
private fun openAppSettings(ctx: android.content.Context) {
    try {
        ctx.startActivity(
            android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", ctx.packageName, null)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {}
}

@Composable
fun MelodiaScreen(vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    var tab by remember { mutableStateOf(0) }
    val songs by vm.filtered.collectAsState()
    val all by vm.songs.collectAsState()
    val query by vm.query.collectAsState()
    // Solo isPlaying en la raíz: el PlayerState completo se observa dentro del
    // miniplayer y del reproductor, para no recomponer toda la pantalla cada tick.
    val isPlaying by vm.playerState.map { it.isPlaying }.distinctUntilChanged()
        .collectAsState(initial = false)
    val current by vm.currentSong.collectAsState()
    val folders by vm.folders.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favSongs by vm.favSongs.collectAsState()
    LaunchedEffect(Unit) { vm.refreshDlHistory() }
    val favIds by vm.favIds.collectAsState()
    val sort by vm.sort.collectAsState()
    val folderSel by vm.folder.collectAsState()
    val albumSel by vm.album.collectAsState()
    val artistSel by vm.artistF.collectAsState()
    val openPid by vm.openPlaylist.collectAsState()
    val openSongs by vm.openPlaylistSongs.collectAsState()
    val allLinks by vm.allPlaylistSongs.collectAsState()
    val sleepLeft by vm.sleep.minsLeft.collectAsState()
    val selecting by vm.selecting.collectAsState()
    val selected by vm.selected.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    val dlJobs by vm.dlJobs.collectAsState()
    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { vm.clearSelection(); vm.reload() }
    var showPlayer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showAddTo by remember { mutableStateOf<Song?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showXfade by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showLib by remember { mutableStateOf(false) }
    // Etiqueta para el reporte de fluidez (Ajustes → Rendimiento).
    LaunchedEffect(tab, showPlayer, showSettings, showLyrics) {
        com.melodia.app.perf.FrameMonitor.screen(
            when {
                showSettings -> "Ajustes"
                showLyrics -> "Letra"
                showPlayer -> "Reproductor"
                tab == 0 -> "Inicio"
                tab == 1 -> "Explorar"
                tab == 2 -> "Listas"
                tab == 3 -> "Favs"
                else -> "Online"
            }
        )
    }
    var showBackup by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf<Song?>(null) }
    var renameId by remember { mutableStateOf<Long?>(null) }
    // Áreas de las barras flotantes para el blur (Haze)
    // Alto real de la barra inferior: el miniplayer se apoya justo encima (sin taparla)

    // ── Reporte del último crash (si la app se cerró sola) ──
    val appCtx = LocalContext.current.applicationContext
    // Modo seguro: si la app se cerró sola, entra sin efectos de vidrio ni extras
    val safeEffects = remember { SafeMode.isOn(appCtx, SafeMode.NO_EFFECTS) }
    val safeExtras = remember { SafeMode.isOn(appCtx, SafeMode.NO_EXTRAS) }
    var lastCrash by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        lastCrash = try {
            val f = java.io.File(appCtx.filesDir, com.melodia.app.MelodiaApp.CRASH_FILE)
            if (f.exists()) f.readText() else null
        } catch (_: Exception) { null }
    }
    if (lastCrash != null) {
        val crashText = lastCrash!!
        AlertDialog(
            onDismissRequest = {},
            title = { Text("La app se cerró") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        "Copia el detalle y mándame una captura:",
                        style = com.melodia.app.ui.theme.Ds.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        crashText.lines().take(25).joinToString("\n"),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val cm = appCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", crashText))
                    } catch (_: Exception) {}
                    try { java.io.File(appCtx.filesDir, com.melodia.app.MelodiaApp.CRASH_FILE).delete() } catch (_: Exception) {}
                    lastCrash = null
                }) { Text("Copiar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    try { java.io.File(appCtx.filesDir, com.melodia.app.MelodiaApp.CRASH_FILE).delete() } catch (_: Exception) {}
                    lastCrash = null
                }) { Text("Cerrar") }
            }
        )
    }

    // ── Back Android por capas: ajustes → letra → player → detalle → filtro → salir ──
    if (showSettings) {
        BackHandler { showSettings = false }
    } else if (showLyrics) {
        BackHandler { showLyrics = false }
    } else if (showPlayer) {
        BackHandler { showPlayer = false }
    } else if (openPid != null) {
        BackHandler { vm.openPl(null) }
    } else if (folderSel != null || albumSel != null || artistSel != null) {
        BackHandler { vm.clearFilters() }
    }

    Box(Modifier.fillMaxSize().background(cs.background)) {
        DsBackground()
        Column(Modifier.fillMaxSize()) {
            // ── Top bar DS compacta: sin logo, solo saludo + nombre ──
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = Ds.Micro, color = cs.onSurfaceVariant)
                    Text("Melodia", style = Ds.Title, color = cs.onBackground)
                }
                if (sleepLeft != null) AssistChip(
                    onClick = { vm.sleep.cancel() },
                    label = { Text("${sleepLeft}m") },
                    leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(14.dp)) }
                )
                DownloadsButton(dlJobs, onClick = { showDownloads = true })
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, "Ajustes", tint = cs.onSurfaceVariant)
                }
            }
            // ── Search: solo donde filtra (Inicio / Explorar), el resto gana espacio ──
            AnimatedVisibility(visible = !selecting && (tab <= 1 || query.isNotBlank())) {
                OutlinedTextField(
                    value = query, onValueChange = vm::setQuery,
                    placeholder = { Text("Buscar temas, álbumes, artistas", style = Ds.Body, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) } },
                    singleLine = true, shape = RoundedCornerShape(Ds.RRow),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = cs.primary.copy(alpha = 0.45f),
                        unfocusedContainerColor = cs.surface,
                        focusedContainerColor = cs.surface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            if (folderSel != null || albumSel != null || artistSel != null) {
                AssistChip(
                    onClick = { vm.clearFilters() },
                    label = { Text(folderSel?.substringAfterLast("/") ?: albumSel ?: artistSel ?: "", maxLines = 1) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
            // ── Barra de selección múltiple ──
            if (selecting) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selected.size} ${if (selected.size == 1) "seleccionado" else "seleccionados"}",
                        style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Todo", style = Ds.Sub, color = cs.primary,
                        modifier = Modifier.clickable { vm.selectAll(songs.map { it.id }) }.padding(6.dp)
                    )
                    Text(
                        "Borrar", style = Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = if (selected.isEmpty()) cs.onSurfaceVariant else cs.error,
                        modifier = Modifier.clickable(enabled = selected.isNotEmpty()) {
                            showDeleteConfirm = true
                        }.padding(6.dp)
                    )
                    Text(
                        "Cancelar", style = Ds.Sub, color = cs.onSurfaceVariant,
                        modifier = Modifier.clickable { vm.clearSelection() }.padding(6.dp)
                    )
                }
            }

            // ── Content: sin blur en vivo (Haze re-blurea en cada frame de scroll) ──
            Box(Modifier.weight(1f)) {
                // Sin transición entre pestañas: animar dos pantallas completas
                // cuesta frames justo cuando el usuario acaba de tocar.
                androidx.compose.runtime.CompositionLocalProvider(
                    com.melodia.app.ui.ds.LocalSelection provides
                        com.melodia.app.ui.ds.SelectionState(selecting, selected, vm::toggleSelected)
                ) {
                androidx.compose.runtime.key(tab) {
                val t = tab
                when (t) {
                    0 -> HomeTab(vm, songs, all, current?.id, isPlaying, favIds,
                        onAdd = { showInfo = it }, onTab = { tab = it },
                        onOpenPlayer = { showPlayer = true })
                    1 -> ExploreTab(vm, folders, albums, artists, folderSel, albumSel, artistSel, songs, all,
                        current?.id, isPlaying, favIds, onAdd = { showInfo = it })
                    2 -> {
                        if (openPid == null) PlaylistsTab(playlists, allLinks, all,
                            onOpen = { vm.openPl(it) }, onCreate = { showCreate = true },
                            onDelete = { vm.deletePlaylist(it) }, onRename = { renameId = it },
                            onPin = { vm.togglePinPlaylist(it) },
                            onCover = { id, album -> vm.setPlaylistCover(id, album) },
                            onPlay = { pid ->
                                val mine = allLinks.filter { it.playlistId == pid }
                                val byId = all.associateBy { it.id }
                                val list = mine.mapNotNull { byId[it.songId] }
                                if (list.isNotEmpty()) vm.playListAt(list, 0)
                            })
                        else PlaylistDetail(
                            playlists.find { it.id == openPid }?.name ?: "Playlist",
                            openSongs, onBack = { vm.openPl(null) },
                            onPlay = { vm.playPlaylistEntities(openSongs, it) },
                            onShuffle = { vm.shufflePlaylistEntities(openSongs) },
                            onMove = { sid, up -> vm.moveInPlaylist(openPid!!, sid, up) },
                            onRemove = { sid -> vm.removeFromPlaylist(openPid!!, sid) })
                    }
                    3 -> FavsTab(favSongs, current?.id, isPlaying, favIds,
                        onPlayAt = { vm.playFavAt(it) }, onFav = { vm.toggleFav(it) }, onAdd = { showInfo = it },
                        onQueue = { s -> vm.queueEnd(s.id) { ok -> toast(appCtx, if (ok) "Añadido al final de la cola" else "No se pudo añadir") } })
                    else -> OnlineTab(vm)
                }
                }
                }
            }
        } // fin Column principal
        // ── Barra inferior: miniplayer y nav apilados en la misma columna ──
        // Antes el miniplayer se posicionaba con un padding calculado del alto de
        // la nav y se solapaba con ella. Apilados, el orden es el del layout.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = current != null && !showPlayer,
                enter = slideInVertically { it } + fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = slideOutVertically { it } + fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                current?.let { s ->
                    Box(Modifier.padding(bottom = 10.dp)) {
                        MiniPlayerHost(
                            vm = vm,
                            song = s,
                            isFav = s.id != YT_STREAM_ID && favIds.contains(s.id),
                            onOpen = { showPlayer = true }
                        )
                    }
                }
            }
            MelodiaPillNav(tab = tab, onTab = { tab = it })
        }
        // ── Now Playing (consume todos los toques: nada pasa a la lista) ──
        AnimatedVisibility(
            visible = showPlayer, enter = slideInVertically { it }, exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier.fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                current?.let { s -> NowPlaying(s, vm, s.id != YT_STREAM_ID && favIds.contains(s.id), onClose = { showPlayer = false }, onAdd = { showAddTo = s }, onLyrics = { showLyrics = true }) }
            }
        }
        // ── Letra en tiempo real (encima del player) ──
        AnimatedVisibility(
            visible = showLyrics, enter = slideInVertically { it }, exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier.fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                current?.let { s -> LyricsSheet(s, vm, onClose = { showLyrics = false }) }
            }
        }
        // ── Ajustes: pantalla propia, entra desde la derecha ──
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(tween(300)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier.fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                SettingsScreen(
                    vm = vm,
                    onClose = { showSettings = false },
                    onOpenEq = { showEq = true },
                    onOpenLibrary = { showLib = true },
                    onOpenBackup = { showBackup = true },
                    onOpenCrossfade = { showXfade = true },
                    onOpenStats = { showStats = true }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Borrar ${selected.size} ${if (selected.size == 1) "tema" else "temas"}") },
            text = {
                Text("Se quitan del dispositivo y de la biblioteca de Melodia. No se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteSelected { sender ->
                        deleteLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(sender).build()
                        )
                    }
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDownloads) DownloadsSheet(vm, onDismiss = { showDownloads = false })

    YtCollectionSheet(vm)

    if (showCreate) TextDialog("Nueva playlist", "", onDismiss = { showCreate = false }) { vm.createPlaylist(it) { showCreate = false } }
    renameId?.let { pid ->
        TextDialog("Renombrar", playlists.find { it.id == pid }?.name ?: "", onDismiss = { renameId = null }) {
            vm.renamePlaylist(pid, it); renameId = null
        }
    }
    showAddTo?.let { s ->
        AlertDialog(
            onDismissRequest = { showAddTo = null },
            title = { Text("Añadir a playlist", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                LazyColumn {
                    if (playlists.isEmpty()) {
                        item { Text("Aún no hay playlists — créala desde la sección Listas", style = Ds.Body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(playlists) { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.addToPlaylist(p.id, s) { ok ->
                                    android.widget.Toast.makeText(
                                        appCtx,
                                        if (ok) "Agregada a ${p.name}" else "Ya está en ${p.name}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showAddTo = null
                            }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.QueueMusic, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddTo = null }) { Text("Cerrar") } }
        )
    }
    showInfo?.let { s ->
        TrackInfoSheet(
            song = s, isFav = favIds.contains(s.id),
            onPlay = { vm.playSong(s.id); showInfo = null },
            onFav = { vm.toggleFav(s.id) },
            onAdd = { showAddTo = s; showInfo = null },
            onClose = { showInfo = null }, vm = vm
        )
    }
    if (showSort) {
        AlertDialog(
            onDismissRequest = { showSort = false },
            title = { Text("Ordenar", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    SortMode.values().forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { vm.setSort(m); showSort = false }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sort == m, onClick = { vm.setSort(m); showSort = false })
                            Text(
                                when (m) { SortMode.TITLE -> "Título"; SortMode.ARTIST -> "Artista"; SortMode.RECENT -> "Recientes"; SortMode.LONGEST -> "Duración" },
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSort = false }) { Text("Cerrar") } }
        )
    }
    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text("Apagar en…", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 60).forEach { m ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { vm.sleep.start(m); showSleep = false }.padding(vertical = 10.dp)) {
                            Text("$m minutos", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable { vm.sleep.cancel(); showSleep = false }.padding(vertical = 10.dp)) {
                        Text("Cancelar temporizador", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("Cerrar") } }
        )
    }
    if (showXfade) {
        CrossfadeSheet(vm, onDismiss = { showXfade = false })
    }
    if (showStats) {
        StatsSheet(vm, onDismiss = { showStats = false })
    }
    if (showLib) {
        LibrarySheet(vm, onDismiss = { showLib = false })
    }
    if (showBackup) {
        BackupSheet(vm, onDismiss = { showBackup = false })
    }
    if (showEq) {
        val eq = vm.player.eq
        if (eq == null) {
            AlertDialog(
                onDismissRequest = { showEq = false },
                title = { Text("Ecualizador", color = MaterialTheme.colorScheme.onBackground) },
                text = { Text("EQ no disponible en este dispositivo") },
                confirmButton = { TextButton(onClick = { showEq = false }) { Text("Cerrar") } }
            )
        } else {
            EqSheet(eq, onClose = { showEq = false })
        }
    }
}

// ── INICIO (estilo Spotify, lenguaje DS) ─────────────────────────
@Composable
fun HomeTab(
    vm: MainViewModel, songs: List<Song>, all: List<Song>,
    currentId: Long?, playing: Boolean, favs: Set<Long>, onAdd: (Song) -> Unit,
    onTab: (Int) -> Unit, onOpenPlayer: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val current by vm.currentSong.collectAsState()
    val query by vm.query.collectAsState()
    val homeYt by vm.homeYt.collectAsState()
    val homeYtLoading by vm.homeYtLoading.collectAsState()
    val busyYt by vm.busyYt.collectAsState()
    val favSongs by vm.favSongs.collectAsState()
    // La biblioteca filtra al instante; YouTube espera a que deje de teclear.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            vm.clearHomeYt()
        } else {
            kotlinx.coroutines.delay(650)
            vm.searchHomeYt(query)
        }
    }
    val loaded by vm.loaded.collectAsState()
    val ctx = LocalContext.current.applicationContext
    val safeExtras2 = remember { SafeMode.isOn(ctx, SafeMode.NO_EXTRAS) }
    if (all.isEmpty() && !loaded && !safeExtras2) {
        HomeSkeleton()
        return
    }
    if (all.isEmpty()) {
        // Estado vacío accionable: antes solo pedía el permiso con texto, sin botón
        val audioPerm =
            if (android.os.Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO
            else android.Manifest.permission.READ_EXTERNAL_STORAGE
        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.reload()
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.MusicNote, null, Modifier.size(34.dp), tint = cs.primary) }
                Spacer(Modifier.height(18.dp))
                Text("Tu biblioteca está vacía", style = Ds.Title, color = cs.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Melodia reproduce la música que ya está en tu teléfono. Concede el permiso de audio y aparecerá aquí.",
                    style = Ds.Body, color = cs.onSurfaceVariant, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = CircleShape, color = cs.primary,
                    modifier = Modifier.clickable { permLauncher.launch(audioPerm) }
                ) {
                    Text(
                        "Conceder permiso de audio", style = Ds.Body, color = cs.onPrimary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Abrir ajustes de la app", style = Ds.Sub, color = cs.onSurfaceVariant,
                    modifier = Modifier.clickable { openAppSettings(ctx) }.padding(10.dp)
                )
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 200.dp)) {
        item {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                val hero = current ?: all.firstOrNull()
                if (hero != null) {
                    HeroCard(
                        label = if (current != null) "Seguir escuchando" else "Tu biblioteca",
                        title = hero.title, subtitle = "${hero.artist} · ${all.size} temas",
                        playing = playing && current != null,
                        onPlay = { if (current != null) vm.player.toggle() else vm.playShuffle() },
                        onOpen = { if (current != null) onOpenPlayer() else vm.playShuffle() }
                    )
                }
            }
        }
        item {
            val q by vm.query.collectAsState()
            if (q.isNotBlank()) {
                val artists by vm.artists.collectAsState()
                val albums by vm.albums.collectAsState()
                val pls by vm.playlists.collectAsState()
                val mArtists = artists.filter { it.name.contains(q, true) }.take(4)
                val mAlbums = albums.filter { it.name.contains(q, true) }.take(4)
                val mPls = pls.filter { it.name.contains(q, true) }.take(4)
                if (mArtists.isNotEmpty() || mAlbums.isNotEmpty() || mPls.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Coincidencias", style = Ds.Title, color = cs.onBackground)
                        Spacer(Modifier.height(8.dp))
                        mArtists.forEach { a ->
                            SearchMatchRow(
                                icon = Icons.Default.Person, title = a.name,
                                sub = "${a.count} ${if (a.count == 1) "tema" else "temas"} · Artista",
                                onTap = { vm.setQuery(""); vm.setArtist(a.name); onTab(1) }
                            )
                        }
                        mAlbums.forEach { a ->
                            SearchMatchRow(
                                icon = Icons.Default.Album, title = a.name,
                                sub = "${a.artist} · Álbum",
                                onTap = { vm.setQuery(""); vm.setAlbum(a.name); onTab(1) }
                            )
                        }
                        mPls.forEach { p ->
                            SearchMatchRow(
                                icon = Icons.Default.QueueMusic, title = p.name,
                                sub = "Playlist",
                                onTap = { vm.setQuery(""); vm.openPl(p.id); onTab(2) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        item {
            val recent by vm.recentPlayed.collectAsState()
            if (recent.isNotEmpty() && query.isBlank()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("Escuchado recientemente", style = Ds.Title, color = cs.onBackground)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(recent, key = { i, s -> "rec-$i-${s.id}" }) { _, s ->
                            Column(
                                Modifier.width(132.dp).clickable { vm.playById(s.id) }
                            ) {
                                SongArt(
                                    albumId = s.albumId, size = 132.dp,
                                    shape = RoundedCornerShape(Ds.RCard),
                                    artUrl = s.artUrl, iconSize = 28.dp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    s.title, style = Ds.Body, color = cs.onBackground,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    s.artist.ifBlank { "Desconocido" }, style = Ds.Sub,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (!safeExtras2) item {
            val albums by vm.albums.collectAsState()
            val shown = remember(albums) { albums.sortedByDescending { it.count }.take(14) }
            if (shown.isNotEmpty() && query.isBlank()) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tus álbumes", style = Ds.Title, color = cs.onBackground, modifier = Modifier.weight(1f))
                        Text(
                            "Ver todos", style = Ds.Sub, color = cs.primary,
                            modifier = Modifier.clickable { onTab(1) }.padding(4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        itemsIndexed(shown, key = { i, a -> "al-$i-${a.name}|${a.artist}" }) { _, a ->
                            Column(
                                Modifier.width(138.dp).clickable {
                                    vm.setQuery(""); vm.setAlbum(a.name); onTab(1)
                                }
                            ) {
                                SongArt(
                                    albumId = a.artId, size = 138.dp, shape = RoundedCornerShape(Ds.RCard),
                                    iconSize = 34.dp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    a.name, style = Ds.Body, color = cs.onBackground,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    a.artist.ifBlank { "Varios artistas" }, style = Ds.Sub,
                                    color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item { SectionHeader("Tus temas", "Aleatorio", onAction = { vm.playShuffle() }) }
        if (songs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "Sin resultados",
                    subtitle = if (query.isBlank()) "Tu biblioteca aún no tiene temas."
                    else "Nada coincide con «$query». Prueba con otra palabra."
                )
            }
        } else {
            items(songs, key = { it.id }) { s ->
                TrackRow(s, s.id == currentId && playing, favs.contains(s.id),
                    onTap = { vm.playSong(s.id) }, onFav = { vm.toggleFav(s.id) }, onLongPress = { onAdd(s) },
                    onQueue = { vm.queueEnd(s.id) { ok -> toast(ctx, if (ok) "Añadido al final de la cola" else "No se pudo añadir") } })
            }
        }
        // ── YouTube: lo que no está en tu biblioteca, sin cambiar de pestaña ──
        if (query.isNotBlank()) {
            item {
                SectionHeader("En YouTube", "Ver en Online", onAction = {
                    vm.setYtQuery(query)
                    onTab(4)
                    vm.searchYt(query)
                })
            }
            when {
                homeYtLoading && homeYt.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        MelodiaSpinner(size = 22.dp, width = 2.dp)
                    }
                }
                homeYt.isEmpty() -> item {
                    Text(
                        "Sin resultados en YouTube para «$query»",
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                else -> itemsIndexed(homeYt.take(8), key = { i, it -> "h-$i-${it.key}" }) { _, item ->
                    when (item) {
                        is com.melodia.app.data.online.YtSearchItem.Track -> YtRowFlat(
                            track = item.track,
                            progress = null,
                            busy = busyYt == item.track.id,
                            onPlay = { vm.playYt(item.track) },
                            onDownload = { vm.downloadYt(item.track) }
                        )
                        is com.melodia.app.data.online.YtSearchItem.Collection -> YtCollectionRow(
                            item = item,
                            busy = busyYt == item.key,
                            onOpen = { vm.openYtCollection(item) },
                            onDownload = { vm.downloadYtCollection(item) }
                        )
                    }
                }
            }
        }
    }
}

// ── EXPLORAR ─────────────────────────────────────────────────────
@Composable
fun ExploreTab(
    vm: MainViewModel,
    folders: List<com.melodia.app.data.MusicFolder>,
    albums: List<com.melodia.app.data.Album>,
    artists: List<com.melodia.app.data.Artist>,
    folderSel: String?, albumSel: String?, artistSel: String?,
    songs: List<Song>, all: List<Song>,
    currentId: Long?, playing: Boolean, favs: Set<Long>, onAdd: (Song) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current.applicationContext
    var seg by remember { mutableStateOf(0) }
    var artistDetail by remember { mutableStateOf<String?>(null) }
    var albumDetail by remember { mutableStateOf<String?>(null) }
    if (artistDetail != null || albumDetail != null) {
        BackHandler { artistDetail = null; albumDetail = null }
    }
    // Detalle de artista / álbum con cabecera propia
    val aName = artistDetail
    val alName = albumDetail
    if (aName != null) {
        val mine = remember(all, aName) { all.filter { (it.artist.ifBlank { "Desconocido" }) == aName } }
        if (mine.isEmpty()) artistDetail = null
        else {
            ArtistDetail(
                name = aName, songs = mine, currentId = currentId, playing = playing, favs = favs,
                onBack = { artistDetail = null },
                onPlay = { vm.playArtist(aName) }, onShuffle = { vm.shuffleArtist(aName) },
                onTap = { vm.playListAt(mine, mine.indexOfFirst { s -> s.id == it }.takeIf { i -> i >= 0 } ?: 0) },
                onFav = { vm.toggleFav(it) }, onLongPress = onAdd,
                onQueue = { vm.queueEnd(it.id) { ok -> toast(ctx, if (ok) "Añadido al final de la cola" else "No se pudo añadir") } }
            )
            return
        }
    }
    if (alName != null) {
        val mine = remember(all, alName) { all.filter { (it.album.ifBlank { "Sin álbum" }) == alName } }
        if (mine.isEmpty()) albumDetail = null
        else {
            AlbumDetail(
                album = albums.find { it.name == alName }, name = alName, songs = mine,
                currentId = currentId, playing = playing, favs = favs,
                onBack = { albumDetail = null },
                onPlay = { vm.playAlbum(alName) }, onShuffle = { vm.shuffleAlbum(alName) },
                onTap = { vm.playListAt(mine, mine.indexOfFirst { s -> s.id == it }.takeIf { i -> i >= 0 } ?: 0) },
                onFav = { vm.toggleFav(it) }, onLongPress = onAdd,
                onQueue = { vm.queueEnd(it.id) { ok -> toast(ctx, if (ok) "Añadido al final de la cola" else "No se pudo añadir") } }
            )
            return
        }
    }
    // Portada por artista una sola vez: antes cada fila recorría toda la biblioteca.
    val artistArt = remember(all, artists) {
        val m = HashMap<String, Long>(artists.size * 2)
        for (song in all) {
            val k = song.artist.ifBlank { "Desconocido" }
            if ((m[k] ?: 0L) <= 0L && song.albumId > 0L) m[k] = song.albumId
        }
        m
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 200.dp)) {
        if (folderSel != null || albumSel != null || artistSel != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp, 8.dp, 20.dp, 8.dp)) {
                    Text("${songs.size} en selección", style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (songs.isNotEmpty()) FilledTonalButton(onClick = { if (songs.isNotEmpty()) vm.playListAt(songs, 0) }) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Reproducir")
                    }
                }
            }
            items(songs, key = { it.id }) { s ->
                TrackRow(s, s.id == currentId && playing, favs.contains(s.id),
                    onTap = { vm.playSong(s.id) }, onFav = { vm.toggleFav(s.id) }, onLongPress = { onAdd(s) },
                    onQueue = { vm.queueEnd(s.id) { ok -> toast(ctx, if (ok) "Añadido al final de la cola" else "No se pudo añadir") } })
            }
        } else {
            item {
                // Sub-pestañas: Carpetas · Artistas · Álbumes
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToggleChip(selected = seg == 0, text = "Carpetas") { seg = 0 }
                    ToggleChip(selected = seg == 1, text = "Artistas") { seg = 1 }
                    ToggleChip(selected = seg == 2, text = "Álbumes") { seg = 2 }
                }
            }
            when (seg) {
                0 -> {
                    items(folders) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.setFolder(f.path) }.padding(horizontal = 20.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = cs.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                                Text("${f.count} temas", style = Ds.Sub, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
                1 -> {
                    items(artists) { ar ->
                        val artId = artistArt[ar.name] ?: 0L
                        Row(
                            Modifier.fillMaxWidth().clickable { artistDetail = ar.name }.padding(horizontal = 20.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (artId > 0) SongArt(artId, 48.dp, CircleShape, iconSize = 20.dp)
                            else Box(
                                Modifier.size(48.dp).clip(CircleShape).background(cs.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Person, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ar.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                                Text("${ar.count} temas", style = Ds.Sub, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
                else -> {
                    items(albums) { a ->
                        Row(
                            Modifier.fillMaxWidth().clickable { albumDetail = a.name }.padding(horizontal = 20.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (a.artId > 0) SongArt(a.artId, 48.dp, RoundedCornerShape(Ds.RRow), iconSize = 20.dp)
                            else Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(Ds.RRow)).background(cs.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Album, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                                Text("${a.artist} · ${a.count}", style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Cabecera de colección (artista o álbum): arte, conteo, duración y botones propios. */
@Composable
private fun CollectionHeader(
    title: String,
    subtitle: String,
    art: @Composable () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, "Atrás", tint = cs.onBackground)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp)) { art() }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Ds.Title, color = cs.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onPlay, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Reproducir")
            }
            FilledTonalButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Aleatorio")
            }
        }
    }
}

private fun totalDur(list: List<Song>): String {
    val ms = list.sumOf { it.durationMs }
    val m = (ms / 60000).toInt()
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

@Composable
private fun ArtistDetail(
    name: String, songs: List<Song>,
    currentId: Long?, playing: Boolean, favs: Set<Long>,
    onBack: () -> Unit, onPlay: () -> Unit, onShuffle: () -> Unit,
    onTap: (Long) -> Unit, onFav: (Long) -> Unit, onLongPress: (Song) -> Unit,
    onQueue: ((Song) -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val artId = remember(songs) { songs.firstOrNull { it.albumId > 0 }?.albumId ?: 0L }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 200.dp)) {
        item {
            CollectionHeader(
                title = name,
                subtitle = "${songs.size} temas · ${totalDur(songs)}",
                art = {
                    if (artId > 0) SongArt(artId, 84.dp, CircleShape, iconSize = 32.dp)
                    else Box(
                        Modifier.size(84.dp).clip(CircleShape).background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Person, null, Modifier.size(36.dp), tint = cs.onSurfaceVariant) }
                },
                onPlay = onPlay, onShuffle = onShuffle, onBack = onBack
            )
        }
        items(songs, key = { it.id }) { s ->
            TrackRow(s, s.id == currentId && playing, favs.contains(s.id),
                onTap = { onTap(s.id) }, onFav = { onFav(s.id) }, onLongPress = { onLongPress(s) },
                onQueue = onQueue?.let { q -> { q(s) } })
        }
    }
}

@Composable
private fun AlbumDetail(
    album: com.melodia.app.data.Album?, name: String, songs: List<Song>,
    currentId: Long?, playing: Boolean, favs: Set<Long>,
    onBack: () -> Unit, onPlay: () -> Unit, onShuffle: () -> Unit,
    onTap: (Long) -> Unit, onFav: (Long) -> Unit, onLongPress: (Song) -> Unit,
    onQueue: ((Song) -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val artId = album?.artId ?: songs.firstOrNull { it.albumId > 0 }?.albumId ?: 0L
    val artist = album?.artist?.ifBlank { songs.firstOrNull()?.artist ?: "" } ?: ""
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 200.dp)) {
        item {
            CollectionHeader(
                title = name,
                subtitle = (if (artist.isNotBlank()) "$artist · " else "") + "${songs.size} temas · ${totalDur(songs)}",
                art = {
                    if (artId > 0) SongArt(artId, 84.dp, RoundedCornerShape(Ds.RRow), iconSize = 32.dp)
                    else Box(
                        Modifier.size(84.dp).clip(RoundedCornerShape(Ds.RRow)).background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Album, null, Modifier.size(36.dp), tint = cs.onSurfaceVariant) }
                },
                onPlay = onPlay, onShuffle = onShuffle, onBack = onBack
            )
        }
        items(songs, key = { it.id }) { s ->
            TrackRow(s, s.id == currentId && playing, favs.contains(s.id),
                onTap = { onTap(s.id) }, onFav = { onFav(s.id) }, onLongPress = { onLongPress(s) },
                onQueue = onQueue?.let { q -> { q(s) } })
        }
    }
}

// ── LISTAS ───────────────────────────────────────────────────────
@Composable
fun PlaylistsTab(
    playlists: List<com.melodia.app.data.local.PlaylistEntity>,
    links: List<com.melodia.app.data.local.PlaylistSongEntity>,
    songs: List<Song>,
    onOpen: (Long) -> Unit, onCreate: () -> Unit, onDelete: (Long) -> Unit, onRename: (Long) -> Unit,
    onPlay: (Long) -> Unit, onPin: (Long) -> Unit, onCover: (Long, Long?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val byId = remember(songs) { songs.associateBy { it.id } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 6.dp, 20.dp, 200.dp)) {
        item {
            Surface(shape = Ds.CardShape, color = cs.surface, modifier = Modifier.fillMaxWidth().clickable(onClick = onCreate)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, tint = cs.primary) }
                    Spacer(Modifier.width(12.dp))
                    Text("Nueva playlist", style = Ds.Title, color = cs.onBackground)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (playlists.isEmpty()) item {
            EmptyState(
                icon = Icons.Default.PlaylistPlay,
                title = "Sin listas todavía",
                subtitle = "Crea una arriba y arrastra temas desde su ficha."
            )
        }
        items(playlists, key = { it.id }) { p ->
            var menu by remember { mutableStateOf(false) }
            var coverPick by remember { mutableStateOf(false) }
            if (coverPick) {
                CoverPickerSheet(
                    mine = links.filter { it.playlistId == p.id },
                    songs = songs, current = p.coverAlbumId,
                    onPick = { onCover(p.id, it); coverPick = false },
                    onDismiss = { coverPick = false }
                )
            }
            val mine = remember(links, p.id) { links.filter { it.playlistId == p.id } }
            val arts = remember(mine, byId) {
                mine.mapNotNull { byId[it.songId]?.albumId }.filter { it > 0 }.distinct().take(4)
            }
            Row(Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaylistCover(arts, p.coverAlbumId)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (p.pinned) {
                            Icon(Icons.Default.PushPin, null, Modifier.size(14.dp), tint = cs.primary)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(p.name, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        if (mine.isEmpty()) "Vacía" else "${mine.size} ${if (mine.size == 1) "tema" else "temas"} · ${fmtTotal(mine.sumOf { it.durationMs })}",
                        style = Ds.Sub, color = cs.onSurfaceVariant
                    )
                }
                if (mine.isNotEmpty()) {
                    IconButton(onClick = { onPlay(p.id) }) {
                        Icon(Icons.Default.PlayArrow, null, tint = cs.primary)
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null, tint = cs.onSurfaceVariant) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (p.pinned) "Desfijar" else "Fijar arriba") }, onClick = { menu = false; onPin(p.id) })
                        DropdownMenuItem(text = { Text("Elegir portada") }, onClick = { menu = false; coverPick = true })
                        DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menu = false; onRename(p.id) })
                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete(p.id) })
                    }
                }
            }
        }
    }
}

/** Portada: elegida por ti, collage 2×2 automático, una sola si hay una, icono si no hay. */
@Composable
fun PlaylistCover(albumIds: List<Long>, chosen: Long? = null) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Ds.RRow)
    Box(
        Modifier.size(52.dp).clip(shape).background(cs.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            chosen != null && chosen > 0 ->
                SongArt(chosen, size = 52.dp, shape = shape, iconSize = 24.dp)
            albumIds.isEmpty() ->
                Icon(Icons.Default.QueueMusic, null, Modifier.size(24.dp), tint = cs.onSurfaceVariant)
            albumIds.size == 1 ->
                SongArt(albumIds[0], size = 52.dp, shape = shape, iconSize = 24.dp)
            else -> {
                val cells = (albumIds + List(4) { -1L }).take(4)
                Column {
                    Row { MiniCover(cells[0]); MiniCover(cells[1]) }
                    Row { MiniCover(cells[2]); MiniCover(cells[3]) }
                }
            }
        }
    }
}

/** Elige la portada entre las carátulas de la lista, o vuelve al collage automático. */
@Composable
fun CoverPickerSheet(
    mine: List<com.melodia.app.data.local.PlaylistSongEntity>,
    songs: List<Song>, current: Long?,
    onPick: (Long?) -> Unit, onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val byId = remember(songs) { songs.associateBy { it.id } }
    val arts = remember(mine, byId) {
        mine.mapNotNull { byId[it.songId] }
            .filter { it.albumId > 0 }
            .distinctBy { it.albumId }
            .take(12)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Portada", color = cs.onBackground) },
        text = {
            if (arts.isEmpty()) {
                Text("Esta lista aún no tiene temas con carátula", style = Ds.Body, color = cs.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                    .background(cs.surfaceVariant)
                                    .clickable { onPick(null) }
                                    .border(
                                        if (current == null) 2.dp else 0.dp,
                                        if (current == null) cs.primary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.AutoAwesome, null, tint = cs.onSurfaceVariant) }
                            Text("Auto", style = Ds.Sub, color = cs.onSurfaceVariant)
                        }
                    }
                    items(arts, key = { it.id }) { s ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.clickable { onPick(s.albumId) }
                                    .border(
                                        if (current == s.albumId) 2.dp else 0.dp,
                                        if (current == s.albumId) cs.primary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) { SongArt(s.albumId, size = 56.dp, shape = RoundedCornerShape(12.dp), iconSize = 20.dp) }
                            Text(
                                s.title, style = Ds.Sub, color = cs.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(56.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun MiniCover(albumId: Long) {
    val cs = MaterialTheme.colorScheme
    if (albumId > 0) {
        SongArt(albumId, size = 26.dp, shape = RectangleShape, iconSize = 12.dp)
    } else {
        Box(Modifier.size(26.dp).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(12.dp), tint = cs.onSurfaceVariant)
        }
    }
}

private fun fmtTotal(ms: Long): String {
    val m = (ms / 60000).toInt()
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

/**
 * Reloj de UI para interpolar la posición: tick de 100 ms solo mientras suena.
 * No consulta al reproductor, así que no añade llamadas JNI.
 */
@Composable
private fun playbackNow(playing: Boolean): Long {
    val now = remember { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(playing) {
        while (playing) {
            now.value = android.os.SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(100)
        }
        now.value = android.os.SystemClock.elapsedRealtime()
    }
    return now.value
}

/**
 * Miniplayer aislado del árbol: es el único que observa [MainViewModel.playerState]
 * completo, así el tick de progreso no recompone la lista ni las pestañas.
 */
@Composable
private fun MiniPlayerHost(
    vm: MainViewModel,
    song: Song,
    isFav: Boolean,
    onOpen: () -> Unit
) {
    val st by vm.playerState.collectAsState()
    Box(Modifier.fillMaxWidth()) {
        MiniPlayerDs(
            title = song.title, artist = song.artist, albumId = song.albumId, artUrl = song.artUrl,
            playing = st.isPlaying, progress = st.progress, isFav = isFav,
            onToggleFav = { if (song.id != YT_STREAM_ID) vm.toggleFav(song.id) },
            onToggle = { vm.player.toggle() },
            onOpen = onOpen
        )
    }
}

/**
 * Barra de progreso aislada: el tick de 100 ms solo recompone este bloque,
 * no toda la pantalla del reproductor.
 */
@Composable
private fun PlaybackProgress(state: com.melodia.app.player.PlayerState, onSeek: (Float) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val nowElapsed = playbackNow(state.isPlaying)
    val livePos = PlaybackClock.positionMs(
        positionMs = state.positionMs,
        snapshotElapsedMs = state.snapshotElapsedMs,
        nowElapsedMs = nowElapsed,
        isPlaying = state.isPlaying,
        durationMs = state.durationMs,
        speed = state.speed
    )
    val smoothPos by androidx.compose.animation.core.animateFloatAsState(
        PlaybackClock.progress(livePos, state.durationMs).coerceIn(0f, 1f),
        androidx.compose.animation.core.tween(180), label = "heroslider"
    )
    SeekBar(progress = smoothPos, onSeek = onSeek)
    Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Text(fmt(livePos), style = Ds.Sub, color = cs.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(fmt(state.durationMs), style = Ds.Sub, color = cs.onSurfaceVariant)
    }
}

/**
 * Barra de avance propia: pista de 4dp, relleno del acento y perilla de 12dp.
 * El Slider de Material era grueso y con sombra; acá manda el trazo fino.
 */
@Composable
private fun SeekBar(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    var widthPx by remember { mutableStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val shown = (if (dragging) dragValue else progress).coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(28.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { off -> onSeek((off.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        dragging = true
                        dragValue = (off.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = { dragging = false; onSeek(dragValue) },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    dragValue = (change.position.x / widthPx).coerceIn(0f, 1f)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val cy = size.height / 2f
            val r = 2.dp.toPx()
            drawRoundRect(
                color = cs.onSurfaceVariant.copy(alpha = 0.20f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, cy - r),
                size = androidx.compose.ui.geometry.Size(size.width, r * 2),
                cornerRadius = CornerRadius(r)
            )
            val fill = (size.width * shown).coerceAtLeast(0f)
            if (fill > 0f) {
                drawRoundRect(
                    color = cs.primary,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, cy - r),
                    size = androidx.compose.ui.geometry.Size(fill, r * 2),
                    cornerRadius = CornerRadius(r)
                )
            }
            val knobR = 6.dp.toPx()
            val knobX = fill.coerceIn(knobR, (size.width - knobR).coerceAtLeast(knobR))
            drawCircle(color = cs.primary, radius = knobR, center = androidx.compose.ui.geometry.Offset(knobX, cy))
        }
    }
}

@Composable
fun PlaylistDetail(
    name: String,
    songs: List<com.melodia.app.data.local.PlaylistSongEntity>,
    onBack: () -> Unit, onPlay: (Int) -> Unit, onShuffle: () -> Unit,
    onMove: (Long, Boolean) -> Unit, onRemove: (Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 6.dp, 20.dp, 200.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = cs.onBackground) }
                Column(Modifier.weight(1f)) {
                    Text(name, style = Ds.Title, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (songs.isEmpty()) "Vacía" else "${songs.size} ${if (songs.size == 1) "tema" else "temas"} · ${fmtTotal(songs.sumOf { it.durationMs })}",
                        style = Ds.Sub, color = cs.onSurfaceVariant
                    )
                }
                if (songs.isNotEmpty()) {
                    IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Aleatorio", tint = cs.onBackground) }
                    FilledTonalButton(onClick = { onPlay(0) }) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Text("Reproducir")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (songs.isEmpty()) item {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Sin temas — mantén presionada una canción para agregarla", style = Ds.Body, color = cs.onSurfaceVariant)
            }
        }
        itemsIndexed(songs, key = { idx0, e -> "pl-$idx0-${e.playlistId}-${e.songId}" }) { idx, e ->
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().clickable { onPlay(idx) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${idx + 1}", style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.title.ifBlank { "Tema" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                    Text("${e.artist} · ${fmt(e.durationMs)}", style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Subir") }, enabled = idx > 0,
                            onClick = { menu = false; onMove(e.songId, true) }
                        )
                        DropdownMenuItem(
                            text = { Text("Bajar") }, enabled = idx < songs.size - 1,
                            onClick = { menu = false; onMove(e.songId, false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Quitar") },
                            onClick = { menu = false; onRemove(e.songId) }
                        )
                    }
                }
            }
        }
    }
}

// ── FAVS ─────────────────────────────────────────────────────────
@Composable
fun FavsTab(
    favSongs: List<Song>, currentId: Long?, playing: Boolean, favs: Set<Long>,
    onPlayAt: (Int) -> Unit, onFav: (Long) -> Unit, onAdd: (Song) -> Unit,
    onQueue: ((Song) -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    if (favSongs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EmptyState(
                icon = Icons.Default.Favorite,
                title = "Sin favoritos",
                subtitle = "Toca el corazón de un tema o desliza la fila a la izquierda."
            )
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp)) {
            itemsIndexed(favSongs, key = { _, s -> s.id }) { idx, s ->
                TrackRow(s, s.id == currentId && playing, true,
                    onTap = { onPlayAt(idx) }, onFav = { onFav(s.id) }, onLongPress = { onAdd(s) },
                    onQueue = onQueue?.let { q -> { q(s) } })
            }
        }
    }
}

// ── NOW PLAYING DS ───────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlaying(s: Song, vm: MainViewModel, isFav: Boolean, onClose: () -> Unit, onAdd: () -> Unit, onLyrics: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val state by vm.playerState.collectAsState()
    val dlMap by vm.dlProgress.collectAsState()
    val curTrack by vm.currentTrack.collectAsState()
    val queue by vm.queueSongs.collectAsState()
    val queueTotal = queue.size
    val isStream = s.id == YT_STREAM_ID
    var showQueue by remember { mutableStateOf(false) }
    var showPitch by remember { mutableStateOf(false) }
    var tapMsg by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    LaunchedEffect(tapMsg) { if (tapMsg != null) { kotlinx.coroutines.delay(900); tapMsg = null } }
    if (showQueue) QueueSheet(vm, onDismiss = { showQueue = false })
    if (showPitch) PitchSheet(vm, state.pitch, state.speed, onDismiss = { showPitch = false })
    val safeFx = remember { SafeMode.isOn(ctx.applicationContext, SafeMode.NO_EFFECTS) }
    BoxWithConstraints(Modifier.fillMaxSize().background(cs.background)) {
        // Fondo teñido con la carátula actual (color dinámico, sin costo)
        val bgModel = songArtModel(s.albumId, s.artUrl)
        if (bgModel != null) {
            // Fondo a 160 px y sin desenfoque en vivo: el gradiente hace el resto.
            // Un blur(60dp) a pantalla completa es un RenderEffect por frame.
            val bgReq = remember(bgModel) {
                coil.request.ImageRequest.Builder(ctx).data(bgModel).size(160).build()
            }
            AsyncImage(
                model = bgReq, contentDescription = null,
                contentScale = ContentScale.Crop, alpha = 0.22f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            cs.background.copy(alpha = 0.45f),
                            cs.background.copy(alpha = 0.72f),
                            cs.background.copy(alpha = 0.94f)
                        )
                    )
                )
            )
        } else {
            DsBackground()
        }
        val dlProg = if (isStream) curTrack?.let { dlMap[it.id] } else null
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Barra superior: cerrar · posición en la cola · menú ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Cerrar", Modifier.size(26.dp), tint = cs.onBackground)
                }
                Text(
                    if (queueTotal > 0) "${state.currentIndex + 1} de $queueTotal" else "Reproduciendo",
                    style = Ds.Micro, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dlProg != null) {
                        Surface(shape = CircleShape, color = cs.primary.copy(alpha = 0.16f)) {
                            Text(
                                "${(dlProg * 100).toInt()}%", style = Ds.Sub, color = cs.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    var npMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { npMenu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.MoreVert, "Más opciones", tint = cs.onBackground)
                        }
                        DropdownMenu(expanded = npMenu, onDismissRequest = { npMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Cola de reproducción") },
                                leadingIcon = { Icon(Icons.Default.QueueMusic, null) },
                                onClick = { npMenu = false; showQueue = true }
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.pitch != 1f || state.speed != 1f) "Tono y velocidad · activo" else "Tono y velocidad") },
                                leadingIcon = { Icon(Icons.Default.Tune, null) },
                                onClick = { npMenu = false; showPitch = true }
                            )
                            if (isStream && curTrack != null && dlProg == null) {
                                DropdownMenuItem(
                                    text = { Text("Descargar audio") },
                                    leadingIcon = { Icon(Icons.Default.Download, null) },
                                    onClick = { npMenu = false; vm.downloadYt(curTrack!!) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Añadir a playlist") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                                onClick = { npMenu = false; onAdd() }
                            )
                        }
                    }
                }
            }

            // ── Carátula: ocupa todo el aire disponible (antes sobraba un hueco
            // enorme entre el título y los controles) ──
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val artSize = minOf(maxWidth, maxHeight).coerceIn(170.dp, 430.dp)
                AnimatedContent(
                    targetState = s.id, label = "nowplaying",
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f)) togetherWith
                            (fadeOut(tween(160)) + scaleOut(targetScale = 0.97f))
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(Modifier.size(artSize), contentAlignment = Alignment.Center) {
                        SongArt(
                            albumId = s.albumId, size = artSize, shape = Ds.HeroShape,
                            iconSize = artSize * 0.36f, artUrl = s.artUrl
                        )
                        // Doble toque: izquierda −10 s, derecha +10 s
                        Box(
                            Modifier.matchParentSize().pointerInput(s.id) {
                                detectTapGestures(onDoubleTap = { off ->
                                    if (off.x < size.width / 2) { vm.player.seekBy(-10_000); tapMsg = "−10 segundos" }
                                    else { vm.player.seekBy(10_000); tapMsg = "+10 segundos" }
                                })
                            }
                        )
                        tapMsg?.let {
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.7f)) {
                                Text(
                                    it, color = Color.White, style = Ds.Body,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Título y artista, pegados a los controles ──
            Text(
                s.title, style = Ds.Display, color = cs.onBackground,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${s.artist.ifBlank { "Desconocido" }} · ${s.album.ifBlank { "Sencillo" }}",
                style = Ds.Body, color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            PlaybackProgress(state) { vm.player.seekTo(it) }
            Spacer(Modifier.height(4.dp))

            // ── Transporte ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { vm.player.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle, "Aleatorio",
                        tint = if (state.shuffle) cs.primary else cs.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.player.prev() }) {
                    Icon(
                        Icons.Default.SkipPrevious, "Anterior",
                        Modifier.size(36.dp), tint = cs.onBackground
                    )
                }
                Surface(
                    shape = CircleShape, color = cs.primary,
                    modifier = Modifier.size(Ds.PlayHero)
                        .then(pressClick({ vm.player.toggle() }, min = 0.88f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = state.isPlaying, label = "pp-hero") { p ->
                            Icon(
                                if (p) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                                tint = cs.surface, modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = { vm.player.next() }) {
                    Icon(
                        Icons.Default.SkipNext, "Siguiente",
                        Modifier.size(36.dp), tint = cs.onBackground
                    )
                }
                IconButton(onClick = { vm.player.cycleRepeat() }) {
                    val icon = when (state.repeatMode) {
                        2 -> Icons.Default.RepeatOne
                        1 -> Icons.Default.Repeat
                        else -> Icons.Default.Repeat
                    }
                    Icon(
                        icon, "Repetir",
                        tint = if (state.repeatMode != 0) cs.primary else cs.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Acciones de la canción ──
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isStream) {
                    NpAction(Icons.Default.Favorite, "Favorito", active = isFav) { vm.toggleFav(s.id) }
                }
                NpAction(Icons.Default.Lyrics, "Letra") { onLyrics() }
                NpAction(Icons.Default.QueueMusic, "Cola") { showQueue = true }
            }
        }
    }
}

/** Acción secundaria del reproductor: icono + etiqueta en una píldora plana. */
@Composable
private fun NpAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (active) cs.primary.copy(alpha = 0.14f) else cs.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (active) cs.primary else cs.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(label, style = Ds.Sub, color = if (active) cs.primary else cs.onBackground)
    }
}

// ── Tono (pitch) y velocidad ───────────────────────────────────────
@Composable
fun PitchSheet(vm: MainViewModel, pitch: Float, speed: Float, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var p by remember(pitch) { mutableStateOf(pitch) }
    var v by remember(speed) { mutableStateOf(speed) }
    fun semis(x: Float): String {
        val st = (12 * kotlin.math.log2(x)).toInt()
        return if (st == 0) "±0" else if (st > 0) "+$st" else "$st"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tono y velocidad", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tono", style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f))
                    Text(
                        "${(p * 100).toInt()}% · ${semis(p)} st",
                        style = Ds.Sub, color = cs.primary
                    )
                }
                Slider(
                    value = p, onValueChange = { p = it; vm.setPitch(it) },
                    valueRange = 0.5f..1.5f, steps = 19
                )
                Text(
                    "Más grave ← → Más agudo (sin cambiar la velocidad)",
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Velocidad", style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f))
                    Text("${(v * 100).toInt()}%", style = Ds.Sub, color = cs.primary)
                }
                Slider(
                    value = v, onValueChange = { v = it; vm.setSpeed(it) },
                    valueRange = 0.5f..1.5f, steps = 19
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { p = 1f; v = 1f; vm.resetPitchSpeed() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Restablecer (100%)") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}

// ── Biblioteca: carpeta + duración mínima ──────────────────────────
@Composable
fun LibrarySheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val minSecs by vm.minSecs.collectAsState()
    val scanFolder by vm.scanFolder.collectAsState()
    val folders by vm.folders.collectAsState()
    var mins by remember(minSecs) { mutableStateOf(minSecs) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Biblioteca", color = cs.onBackground) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Ignorar audios de menos de", style = Ds.Body, color = cs.onBackground)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(0, 15, 30, 60).forEach { s ->
                        val sel = mins == s
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sel) cs.primary else cs.surfaceVariant,
                            modifier = Modifier.weight(1f).clickable { mins = s; vm.setMinSecs(s) }
                        ) {
                            Box(Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (s == 0) "Todo" else "${s}s",
                                    style = Ds.Body,
                                    color = if (sel) cs.surface else cs.onBackground
                                )
                            }
                        }
                    }
                }
                Text(
                    "Oculta tonos, notas de voz y efectos cortos",
                    style = Ds.Sub, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Divider(Modifier.padding(vertical = 10.dp))
                Text("Carpeta", style = Ds.Body, color = cs.onBackground)
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp), color = cs.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().clickable { vm.setScanFolder(null) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Todo el dispositivo", style = Ds.Body,
                            color = if (scanFolder == null) cs.primary else cs.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        if (scanFolder == null) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = cs.primary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                folders.forEach { f ->
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = cs.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().clickable { vm.setScanFolder(f.path) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${f.count} temas", style = Ds.Sub, color = cs.onSurfaceVariant)
                            }
                            if (scanFolder == f.path) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = cs.primary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}

// ── Respaldo y restauración ────────────────────────────────────────
@Composable
fun BackupSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            busy = true
            vm.restore(ctx, uri) { msg = it; busy = false }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Respaldo", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Guarda tus playlists y favoritos en un archivo, y restáuralos cuando quieras",
                    style = Ds.Body, color = cs.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        busy = true
                        vm.backup(ctx) { msg = it; busy = false }
                    },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (busy) "Trabajando…" else "Crear respaldo")
                }
                OutlinedButton(
                    onClick = { picker.launch("application/json") },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restaurar desde archivo")
                }
                msg?.let {
                    Text(it, style = Ds.Sub, color = cs.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cerrar") } }
    )
}

// ── Mis estadísticas ───────────────────────────────────────────────
@Composable
fun StatsSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val sum by vm.statsSummary.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mis estadísticas", color = cs.onBackground) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (sum.plays == 0) {
                    Text(
                        "Aún no hay datos — reproduce algo y vuelve aquí",
                        style = Ds.Body, color = cs.onSurfaceVariant
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("${sum.plays}", "reproducciones", Modifier.weight(1f))
                        StatCard("${sum.minutes}", "minutos", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    if (sum.topSongs.isNotEmpty()) {
                        Text("Más escuchadas", style = Ds.Title, color = cs.onBackground)
                        Spacer(Modifier.height(4.dp))
                        sum.topSongs.forEachIndexed { i, (s, plays) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { vm.playById(s.id); onDismiss() }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text("${i + 1}", style = Ds.Sub, color = cs.primary, modifier = Modifier.width(24.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.artist.ifBlank { "Desconocido" }, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("$plays", style = Ds.Sub, color = cs.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (sum.topArtists.isNotEmpty()) {
                        Text("Artistas top", style = Ds.Title, color = cs.onBackground)
                        Spacer(Modifier.height(4.dp))
                        sum.topArtists.forEach { (a, plays) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)) {
                                Text(a, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$plays", style = Ds.Sub, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (sum.plays > 0) {
                    TextButton(onClick = { confirmClear = true }) { Text("Borrar", color = cs.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Borrar estadísticas", color = cs.onBackground) },
            text = { Text("Se eliminarán tus reproducciones e historial.", style = Ds.Body, color = cs.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { vm.clearStats(); confirmClear = false }) { Text("Borrar", color = cs.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SearchMatchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, sub: String, onTap: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onTap)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(cs.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(14.dp), color = cs.surface, modifier = modifier) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = Ds.Display, color = cs.primary)
            Text(label, style = Ds.Sub, color = cs.onSurfaceVariant)
        }
    }
}

// ── Fundido entre canciones ────────────────────────────────────────
@Composable
fun CrossfadeSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val st by vm.playerState.collectAsState()
    var secs by remember(st.crossfade) { mutableStateOf(vm.player.crossfadeSecs) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fundido entre canciones", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (st.crossfade) "Activado" else "Desactivado",
                        style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f)
                    )
                    Switch(checked = st.crossfade, onCheckedChange = { vm.toggleCrossfade() })
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Duración", style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f))
                    Text("$secs s", style = Ds.Sub, color = cs.primary)
                }
                Slider(
                    value = secs.toFloat(),
                    onValueChange = { secs = it.toInt(); vm.setCrossfadeSecs(it.toInt()) },
                    valueRange = 1f..12f, steps = 10, enabled = st.crossfade
                )
                Text(
                    "El volumen baja al final de cada tema y sube con el siguiente",
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}

// ── Chip seleccionable (estable, sin APIs experimentales) ──────────
@Composable
fun ToggleChip(
    selected: Boolean, text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) cs.primary.copy(alpha = 0.14f) else cs.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(15.dp), tint = if (selected) cs.primary else cs.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                style = if (selected) Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) else Ds.Sub,
                color = if (selected) cs.primary else cs.onBackground
            )
        }
    }
}

// ── Cola de reproducción ───────────────────────────────────────────
@Composable
fun QueueSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val queue by vm.queueSongs.collectAsState()
    val current by vm.currentSong.collectAsState()
    val playing by vm.playerState.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // La lista se lleva como máximo ~la mitad de la ventana: en pantallas
        // cortas el pie (Vaciar / Guardar) no se sale de la hoja.
        BoxWithConstraints {
            val listMax = (maxHeight * 0.52f).coerceIn(140.dp, 360.dp)
            QueueSheetContent(
                queue = queue,
                currentId = current?.id,
                shuffle = playing.shuffle,
                repeatMode = playing.repeatMode,
                listMaxHeight = listMax,
                onPlay = { idx ->
                    val picked = queue.getOrNull(idx)
                    if (picked != null && picked.id != YT_STREAM_ID) {
                        // Tocar el tema que ya suena = play/pausa, no reiniciarlo
                        if (picked.id == current?.id) vm.player.toggle() else vm.playQueueAt(idx)
                        onDismiss()
                    }
                },
                onMove = { from, to ->
                    if (!vm.moveQueueAt(from, to)) toast(ctx, "No se pudo mover")
                },
                onRemove = { idx -> vm.removeQueueAt(idx) },
                onToggleShuffle = { vm.player.toggleShuffle() },
                onCycleRepeat = { vm.player.cycleRepeat() },
                onClear = { confirmClear = true },
                onSave = { showSave = true },
                onClose = onDismiss
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Vaciar la cola?", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Text(
                    "Se quitan todos los temas menos el que está sonando.",
                    style = Ds.Body, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearQueue()
                    toast(ctx, "Cola vaciada")
                }) { Text("Vaciar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }

    if (showSave) {
        TextDialog("Guardar cola", defaultQueueName(), onDismiss = { showSave = false }) { name ->
            showSave = false
            vm.saveQueueAsPlaylist(name) { saved ->
                toast(ctx, if (saved > 0) "Guardada con $saved ${if (saved == 1) "tema" else "temas"}" else "No se pudo guardar")
                if (saved > 0) onDismiss()
            }
        }
    }
}

/**
 * Contenido de la cola, sin ViewModel ni ventana: se puede componer en la JVM
 * con datos falsos y verificar botones y textos sin tocar Room ni el servicio.
 */
@Composable
fun QueueSheetContent(
    queue: List<Song>,
    currentId: Long?,
    shuffle: Boolean,
    repeatMode: Int,
    listMaxHeight: androidx.compose.ui.unit.Dp = 350.dp,
    onPlay: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isStream = queue.any { it.id == YT_STREAM_ID }
    val curIdx = queue.indexOfFirst { it.id == currentId }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (curIdx > 2) curIdx - 2 else 0
    )
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = cs.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(0.94f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cola", style = Ds.Title, color = cs.onBackground)
                    Text(
                        if (queue.isEmpty()) "Sin temas"
                        else "${queue.size} ${if (queue.size == 1) "tema" else "temas"} · ${fmtTotal(queue.sumOf { it.durationMs })}",
                        style = Ds.Sub, color = cs.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Cerrar", Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Repetición y aleatorio sin salir de la cola
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToggleChip(
                    selected = shuffle, text = "Aleatorio",
                    icon = Icons.Default.Shuffle, onClick = onToggleShuffle
                )
                Spacer(Modifier.width(8.dp))
                ToggleChip(
                    selected = repeatMode != 0,
                    text = when (repeatMode) {
                        2 -> "Repetir uno"
                        1 -> "Repetir todo"
                        else -> "No repetir"
                    },
                    icon = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    onClick = onCycleRepeat
                )
            }
            Spacer(Modifier.height(6.dp))
            if (queue.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.QueueMusic, null, Modifier.size(30.dp), tint = cs.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("La cola está vacía", style = Ds.Body, color = cs.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = listMaxHeight)
                ) {
                    itemsIndexed(queue, key = { i, e -> "$i-${e.id}" }) { idx, e ->
                        QueueRow(
                            index = idx, song = e,
                            isCurrent = e.id == currentId,
                            canUp = !isStream && QueueOps.canMoveUp(idx, queue.size),
                            canDown = !isStream && QueueOps.canMoveDown(idx, queue.size),
                            canRemove = !isStream && queue.size > 1,
                            onPlay = { onPlay(idx) },
                            onUp = { onMove(idx, idx - 1) },
                            onDown = { onMove(idx, idx + 1) },
                            onRemove = { onRemove(idx) }
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val canClear = queue.size > 1 && !isStream
                    TextButton(onClick = onClear, enabled = canClear) {
                        Icon(
                            Icons.Default.Delete, null, Modifier.size(16.dp),
                            tint = if (canClear) cs.error else cs.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Vaciar", color = if (canClear) cs.error else cs.onSurfaceVariant)
                    }
                    TextButton(onClick = onSave, enabled = !isStream) {
                        Icon(
                            Icons.Default.PlaylistAdd, null, Modifier.size(16.dp),
                            tint = if (!isStream) cs.primary else cs.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar lista", color = if (!isStream) cs.primary else cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Nombre propuesto al guardar una cola: "Cola 26 oct". */
private fun defaultQueueName(): String = try {
    "Cola " + java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        .format(java.util.Date())
} catch (_: Exception) { "Mi cola" }

@Composable
private fun QueueRow(
    index: Int, song: Song, isCurrent: Boolean,
    canUp: Boolean, canDown: Boolean, canRemove: Boolean,
    onPlay: () -> Unit, onUp: () -> Unit, onDown: () -> Unit, onRemove: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) cs.primary.copy(alpha = 0.09f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(start = 8.dp, end = 2.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp), tint = cs.primary)
            } else {
                Text("${index + 1}", style = Ds.Sub, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, style = Ds.Body,
                color = if (isCurrent) cs.primary else cs.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist.ifBlank { "Desconocido" }, style = Ds.Sub,
                color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        QueueIconBtn(Icons.Default.KeyboardArrowUp, "Subir", canUp, onUp)
        QueueIconBtn(Icons.Default.KeyboardArrowDown, "Bajar", canDown, onDown)
        QueueIconBtn(Icons.Default.Close, "Quitar", canRemove, onRemove)
    }
}

@Composable
private fun QueueIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, enabled: Boolean, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    // Caja propia (no IconButton): el mínimo táctil de Material (48dp) por botón
    // dejaría el título del tema sin espacio en pantallas de 360dp.
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, label, Modifier.size(18.dp),
            tint = cs.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.28f)
        )
    }
}

// ── Diálogos ─────────────────────────────────────────────────────
// ── Editar metadatos ───────────────────────────────────────────────
@Composable
fun EditMetadataSheet(song: Song, vm: MainViewModel, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Editar datos", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("Título") },
                    singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist, onValueChange = { artist = it }, label = { Text("Artista") },
                    singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album, onValueChange = { album = it }, label = { Text("Álbum") },
                    singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank() || saving) return@TextButton
                    saving = true
                    vm.updateMetadata(song, title, artist, album) { ok ->
                        saving = false
                        android.widget.Toast.makeText(
                            ctx,
                            if (ok) "Datos actualizados" else "No se pudo guardar",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        if (ok) onSaved()
                    }
                },
                enabled = title.isNotBlank() && !saving
            ) { Text(if (saving) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}

// ── Ficha del tema (long-press): info útil + acciones ────────────
@Composable
fun TrackInfoSheet(song: Song, isFav: Boolean, onPlay: () -> Unit, onFav: () -> Unit, onAdd: () -> Unit, onClose: () -> Unit, vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    if (showEdit) {
        EditMetadataSheet(song, vm, onDismiss = { showEdit = false }, onSaved = { showEdit = false; onClose() })
    }
    val folder = try { java.io.File(song.dataPath).parent?.substringAfterLast("/") ?: "—" } catch (_: Exception) { "—" }
    val added = try {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(song.dateAdded * 1000))
    } catch (_: Exception) { "—" }
    fun compartir() {
        try {
            val f = java.io.File(song.dataPath)
            if (f.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f)
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(android.content.Intent.createChooser(send, "Compartir"))
            }
        } catch (_: Exception) {}
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            // Cabecera real del tema: arte, título y artista · álbum.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SongArt(
                    albumId = song.albumId, size = 52.dp,
                    shape = RoundedCornerShape(Ds.RRow), artUrl = song.artUrl, iconSize = 24.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title, style = Ds.Title, color = cs.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOf(song.artist.ifBlank { "Desconocido" }, song.album.ifBlank { "—" })
                            .joinToString(" · "),
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                InfoRow("Duración", fmt(song.durationMs))
                InfoRow("Carpeta", folder)
                InfoRow("Agregado", added)
                Spacer(Modifier.height(6.dp))
                Text("Ruta", style = Ds.Micro, color = cs.onSurfaceVariant)
                Text(
                    song.dataPath.ifBlank { "—" }, style = Ds.Sub, color = cs.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reproducir", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onAdd,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("A lista", maxLines = 1)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    InfoAction(
                        icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = "Favorito",
                        active = isFav,
                        onClick = onFav
                    )
                    if (song.id != YT_STREAM_ID) {
                        InfoAction(Icons.Default.Edit, "Editar") { showEdit = true }
                        InfoAction(Icons.Default.Share, "Compartir") { compartir() }
                        InfoAction(Icons.Default.Checklist, "Elegir") {
                            vm.startSelection(song.id)
                            onClose()
                        }
                    }
                }
                if (song.id != YT_STREAM_ID) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                vm.queueNext(song.id) { ok ->
                                    android.widget.Toast.makeText(
                                        ctx, if (ok) "Sonará a continuación" else "No se pudo agregar",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onClose()
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.QueueMusic, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Siguiente", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                vm.queueEnd(song.id) { ok ->
                                    android.widget.Toast.makeText(
                                        ctx, if (ok) "Agregado al final" else "No se pudo agregar",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onClose()
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.LowPriority, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Al final", maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } }
    )
}

/** Acción de la ficha: icono con etiqueta corta debajo, ancho fijo. */
@Composable
private fun InfoAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp).clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 8.dp)
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (active) cs.primary else cs.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            label, style = Ds.Sub,
            color = if (active) cs.primary else cs.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

// ── Diálogo de color de acento (ahora vive dentro de Ajustes) ─────
// Se mantiene por si se necesita como diálogo suelto en el futuro.

@Composable
fun TextDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            OutlinedTextField(value = v, onValueChange = { v = it }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { if (v.isNotBlank()) onOk(v.trim()) }, enabled = v.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun EqSheet(eq: com.melodia.app.player.EqualizerManager, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val enabled by eq.enabled.collectAsState()
    val bands by eq.bands.collectAsState()
    val bass by eq.bassLevel.collectAsState()
    val preset by eq.preset.collectAsState()
    val custom by eq.customPresets.collectAsState()
    var presetMenu by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val freqs = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Ecualizador", color = cs.onBackground) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(if (enabled) "Activado" else "Desactivado", style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { eq.setEnabled(it) })
                }
                Divider(Modifier.padding(vertical = 8.dp))
                Box {
                    OutlinedButton(onClick = { presetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(preset, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onBackground)
                    }
                    DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                        eq.presets.forEach { p ->
                            DropdownMenuItem(text = { Text(p) }, onClick = { presetMenu = false; eq.usePreset(p) })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                bands.forEachIndexed { i, lvl ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row {
                            Text(freqs.getOrElse(i) { "B${i + 1}" }, style = Ds.Sub, color = cs.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text("${lvl / 100}dB", style = Ds.Sub, color = cs.onSurfaceVariant)
                        }
                        Slider(value = lvl.toFloat(), onValueChange = { eq.setBand(i, it.toInt()) }, valueRange = -1500f..1500f, enabled = enabled)
                    }
                }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row {
                        Text("Bass Boost", style = Ds.Sub, color = cs.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text("${bass / 10}%", style = Ds.Sub, color = cs.onSurfaceVariant)
                    }
                    Slider(value = bass.toFloat(), onValueChange = { eq.setBass(it.toInt()) }, valueRange = 0f..1000f, enabled = enabled)
                }
                Divider(Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Mis presets", style = Ds.Body, color = cs.onBackground, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showSave = true }, enabled = enabled) { Text("Guardar actual") }
                }
                if (custom.isEmpty()) {
                    Text("Ajusta las bandas y guárdalas con un nombre", style = Ds.Sub, color = cs.onSurfaceVariant)
                } else {
                    custom.keys.sorted().forEach { name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = enabled) { eq.applyCustomPreset(name) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                name, style = Ds.Body,
                                color = if (preset == name) cs.primary else cs.onBackground,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { eq.deleteCustomPreset(name) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Eliminar", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text("Se aplica en tiempo real y se guarda", style = Ds.Sub, color = cs.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Listo") } }
    )
    if (showSave) {
        TextDialog("Guardar preset", "", onDismiss = { showSave = false }) {
            eq.saveCustomPreset(it); showSave = false
        }
    }
}

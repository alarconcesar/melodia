package com.melodia.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melodia.app.MainViewModel
import com.melodia.app.SortMode
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.DsBackground
import com.melodia.app.ui.ds.HeroCard
import com.melodia.app.ui.ds.MelodiaPillNav
import com.melodia.app.ui.ds.MicroLabel
import com.melodia.app.ui.ds.MiniPlayerDs
import com.melodia.app.ui.ds.QuickCard
import com.melodia.app.ui.ds.SectionHeader
import com.melodia.app.ui.ds.SongArt
import com.melodia.app.ui.ds.TrackRow
import com.melodia.app.ui.ds.pressClick
import com.melodia.app.ui.theme.Ds
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

@Composable
fun MelodiaScreen(vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    var tab by remember { mutableStateOf(0) }
    val songs by vm.filtered.collectAsState()
    val all by vm.songs.collectAsState()
    val query by vm.query.collectAsState()
    val state by vm.playerState.collectAsState()
    val current by vm.currentSong.collectAsState()
    val folders by vm.folders.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favSongs by vm.favSongs.collectAsState()
    val favIds by vm.favIds.collectAsState()
    val sort by vm.sort.collectAsState()
    val folderSel by vm.folder.collectAsState()
    val albumSel by vm.album.collectAsState()
    val artistSel by vm.artistF.collectAsState()
    val openPid by vm.openPlaylist.collectAsState()
    val openSongs by vm.openPlaylistSongs.collectAsState()
    val sleepLeft by vm.sleep.minsLeft.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showAddTo by remember { mutableStateOf<Song?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf<Song?>(null) }
    var renameId by remember { mutableStateOf<Long?>(null) }
    // Áreas de las barras flotantes para el blur (Haze)
    var navArea by remember { mutableStateOf<Rect?>(null) }
    var miniArea by remember { mutableStateOf<Rect?>(null) }
    var contentPos by remember { mutableStateOf(Offset.Zero) }

    // ── Back Android por capas: player → detalle → filtro → salir ──
    if (showPlayer) {
        BackHandler { showPlayer = false }
    } else if (openPid != null) {
        BackHandler { vm.openPl(null) }
    } else if (folderSel != null || albumSel != null || artistSel != null) {
        BackHandler { vm.clearFilters() }
    }

    Box(Modifier.fillMaxSize().background(cs.background)) {
        DsBackground()
        Column(Modifier.fillMaxSize()) {
            // ── Top bar DS ──
            Row(
                Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = cs.surface, modifier = Modifier.clickable { }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp, 4.dp, 14.dp, 4.dp)) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.MusicNote, null, tint = cs.primary, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(greeting(), style = Ds.Sub, color = cs.onSurfaceVariant)
                            Text("Melodia", style = Ds.Title, color = cs.onBackground)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (sleepLeft != null) AssistChip(
                    onClick = { vm.sleep.cancel() },
                    label = { Text("${sleepLeft}m") },
                    leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(14.dp)) }
                )
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = cs.onBackground) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Ecualizador") },
                            leadingIcon = { Icon(Icons.Default.Equalizer, null) },
                            onClick = { showMenu = false; showEq = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sleepLeft != null) "Temporizador (${sleepLeft}m)" else "Temporizador") },
                            leadingIcon = { Icon(Icons.Default.Timer, null) },
                            onClick = { showMenu = false; showSleep = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDarkMode != false) "Modo claro" else "Modo oscuro") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, null) },
                            onClick = { showMenu = false; isDarkMode = !(isDarkMode ?: false) }
                        )
                    }
                }
            }
            // ── Search ──
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                placeholder = { Text("Buscar temas, álbumes, artistas", style = Ds.Body, maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) } },
                singleLine = true, shape = RoundedCornerShape(Ds.RChip),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            )
            if (folderSel != null || albumSel != null || artistSel != null) {
                AssistChip(
                    onClick = { vm.clearFilters() },
                    label = { Text(folderSel?.substringAfterLast("/") ?: albumSel ?: artistSel ?: "", maxLines = 1) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
            // ── Content: única zona con blur (las barras están fuera) ──
            // Velo con las mismas formas redondeadas de las barras (sin "cuadro")
            val density = LocalDensity.current
            fun rounded(rect: Rect, radiusPx: Float): RoundRect {
                val moved = rect.translate(Offset(-contentPos.x, -contentPos.y))
                val c = CornerRadius(radiusPx)
                return RoundRect(moved, c, c, c, c)
            }
            val miniR = with(density) { 20.dp.toPx() }
            val hazeAreas = listOfNotNull(
                navArea?.let { rounded(it, it.height / 2f) },
                miniArea.takeIf { current != null && !showPlayer }?.let { rounded(it, miniR) }
            )
            Box(
                Modifier.weight(1f)
                    .onGloballyPositioned { contentPos = it.positionInRoot() }
                    .haze(
                        *hazeAreas.toTypedArray(),
                        backgroundColor = cs.surface.copy(alpha = 0.32f),
                        blurRadius = 26.dp
                    )
            ) {
                AnimatedContent(
                    targetState = tab, label = "tabs",
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { t ->
                when (t) {
                    0 -> HomeTab(vm, songs, all, current?.id, state.isPlaying, favIds,
                        onAdd = { showInfo = it }, onTab = { tab = it },
                        onOpenPlayer = { showPlayer = true })
                    1 -> ExploreTab(vm, folders, albums, artists, folderSel, albumSel, artistSel, songs,
                        current?.id, state.isPlaying, favIds, onAdd = { showInfo = it })
                    2 -> {
                        if (openPid == null) PlaylistsTab(playlists,
                            onOpen = { vm.openPl(it) }, onCreate = { showCreate = true },
                            onDelete = { vm.deletePlaylist(it) }, onRename = { renameId = it })
                        else PlaylistDetail(openSongs, onBack = { vm.openPl(null) },
                            onPlay = { vm.playPlaylistEntities(openSongs, it) },
                            onRemove = { sid -> vm.removeFromPlaylist(openPid!!, sid) })
                    }
                    else -> FavsTab(favSongs, current?.id, state.isPlaying, favIds,
                        onPlayAt = { vm.playFavAt(it) }, onFav = { vm.toggleFav(it) }, onAdd = { showInfo = it })
                }
                }
            }
        } // fin Column principal
        // ── Pill nav overlay: la lista pasa por debajo con blur ──
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .onGloballyPositioned { navArea = it.boundsInRoot() }
        ) {
            MelodiaPillNav(tab = tab, onTab = { tab = it })
        }
        // ── MiniPlayer flotante ──
        AnimatedVisibility(
            visible = current != null && !showPlayer,
            enter = slideInVertically { it }, exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            current?.let { s ->
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 96.dp)
                        .onGloballyPositioned { miniArea = it.boundsInRoot() }
                ) {
                    MiniPlayerDs(
                        title = s.title, artist = s.artist, albumId = s.albumId,
                        playing = state.isPlaying,
                        progress = state.progress, isFav = favIds.contains(s.id),
                        onToggleFav = { vm.toggleFav(s.id) }, onToggle = { vm.player.toggle() },
                        onOpen = { showPlayer = true }
                    )
                }
            }
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
                current?.let { s -> NowPlaying(s, vm, favIds.contains(s.id), onClose = { showPlayer = false }, onAdd = { showAddTo = s }) }
            }
        }
    }

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
                        item { Text("Aún no hay playlists — créala desde la tab Listas", style = Ds.Body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(playlists) { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.addToPlaylist(p.id, s); showAddTo = null }
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
            onClose = { showInfo = null }
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
    val state by vm.playerState.collectAsState()
    val favSongs by vm.favSongs.collectAsState()
    if (all.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MusicNote, null, Modifier.size(44.dp), tint = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Sin canciones aún", style = Ds.Body, color = cs.onSurfaceVariant)
                Text("Concede permiso de audio", style = Ds.Sub, color = cs.onSurfaceVariant)
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
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        QuickCard("Favoritos", "${favSongs.size}", Icons.Default.Favorite) { onTab(3) }
                    }
                    Box(Modifier.weight(1f)) {
                        QuickCard("Aleatorio", "Mezclar todo", Icons.Default.Shuffle) { vm.playShuffle() }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        QuickCard("Recientes", "Agregados", Icons.Default.History) { vm.setSort(SortMode.RECENT) }
                    }
                    Box(Modifier.weight(1f)) {
                        val artists by vm.artists.collectAsState()
                        QuickCard("Artistas", "${artists.size}", Icons.Default.Person) { onTab(1) }
                    }
                }
            }
        }
        item { SectionHeader("Tus temas", "${songs.size}") }
        if (songs.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Sin resultados", style = Ds.Body, color = cs.onSurfaceVariant)
                }
            }
        } else {
            items(songs, key = { it.id }) { s ->
                TrackRow(s, s.id == currentId && playing, favs.contains(s.id),
                    onTap = { vm.playSong(s.id) }, onFav = { vm.toggleFav(s.id) }, onLongPress = { onAdd(s) })
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
    songs: List<Song>, currentId: Long?, playing: Boolean, favs: Set<Long>, onAdd: (Song) -> Unit
) {
    val cs = MaterialTheme.colorScheme
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
                    onTap = { vm.playSong(s.id) }, onFav = { vm.toggleFav(s.id) }, onLongPress = { onAdd(s) })
            }
        } else {
            item { SectionHeader("Carpetas", "${folders.size}") }
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
                    Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            item { SectionHeader("Álbumes", "${albums.size}") }
            items(albums.take(50)) { a ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setAlbum(a.name) }.padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(Ds.RRow)).background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Album, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                        Text("${a.artist} · ${a.count}", style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            item { SectionHeader("Artistas", "${artists.size}") }
            items(artists.take(50)) { ar ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setArtist(ar.name) }.padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceVariant),
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
    }
}

// ── LISTAS ───────────────────────────────────────────────────────
@Composable
fun PlaylistsTab(
    playlists: List<com.melodia.app.data.local.PlaylistEntity>,
    onOpen: (Long) -> Unit, onCreate: () -> Unit, onDelete: (Long) -> Unit, onRename: (Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme
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
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Sin playlists — crea la primera", style = Ds.Body, color = cs.onSurfaceVariant)
            }
        }
        items(playlists, key = { it.id }) { p ->
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(Ds.RRow)).background(cs.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.QueueMusic, null, Modifier.size(24.dp), tint = cs.onSurfaceVariant) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(p.name, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Playlist", style = Ds.Sub, color = cs.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null, tint = cs.onSurfaceVariant) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menu = false; onRename(p.id) })
                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete(p.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetail(
    songs: List<com.melodia.app.data.local.PlaylistSongEntity>,
    onBack: () -> Unit, onPlay: (Int) -> Unit, onRemove: (Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 6.dp, 20.dp, 200.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = cs.onBackground) }
                Text("${songs.size} temas", style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (songs.isNotEmpty()) FilledTonalButton(onClick = { onPlay(0) }) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Text("Play")
                }
            }
        }
        itemsIndexed(songs, key = { _, e -> "${e.playlistId}-${e.songId}" }) { idx, e ->
            Row(Modifier.fillMaxWidth().clickable { onPlay(idx) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${idx + 1}", style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.title.ifBlank { "Tema" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = Ds.Body, color = cs.onBackground)
                    Text("${e.artist} · ${fmt(e.durationMs)}", style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { onRemove(e.songId) }) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant) }
            }
        }
    }
}

// ── FAVS ─────────────────────────────────────────────────────────
@Composable
fun FavsTab(
    favSongs: List<Song>, currentId: Long?, playing: Boolean, favs: Set<Long>,
    onPlayAt: (Int) -> Unit, onFav: (Long) -> Unit, onAdd: (Song) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    if (favSongs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Favorite, null, Modifier.size(44.dp), tint = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Sin favoritos", style = Ds.Body, color = cs.onSurfaceVariant)
                Text("Toca el corazón de un tema", style = Ds.Sub, color = cs.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp)) {
            itemsIndexed(favSongs, key = { _, s -> s.id }) { idx, s ->
                TrackRow(s, s.id == currentId && playing, true,
                    onTap = { onPlayAt(idx) }, onFav = { onFav(s.id) }, onLongPress = { onAdd(s) })
            }
        }
    }
}

// ── NOW PLAYING DS ───────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlaying(s: Song, vm: MainViewModel, isFav: Boolean, onClose: () -> Unit, onAdd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val state by vm.playerState.collectAsState()
    Column(Modifier.fillMaxSize().background(cs.background).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(28.dp), tint = cs.onBackground) }
            Spacer(Modifier.weight(1f))
            MicroLabel("Ahora suena")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.toggleFav(s.id) }) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) cs.primary else cs.onBackground)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.PlaylistAdd, null, tint = cs.onBackground) }
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = s.id, label = "nowplaying",
            transitionSpec = { (fadeIn() + slideInVertically { it / 4 } + scaleIn(initialScale = 0.94f)) togetherWith (fadeOut() + slideOutVertically { -it / 4 } + scaleOut(targetScale = 0.94f)) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = Ds.HeroShape, color = cs.surface, modifier = Modifier.size(300.dp)) {
            SongArt(
                albumId = s.albumId, size = 300.dp, shape = Ds.HeroShape,
                iconSize = 110.dp
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(s.title, style = Ds.Display, color = cs.onBackground, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE))
        Text("${s.artist} · ${s.album.ifBlank { "Sencillo" }}", style = Ds.Body, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        }
        Spacer(Modifier.height(16.dp))
        var dragging by remember { mutableStateOf(false) }
        var dragPos by remember { mutableStateOf(0f) }
        Slider(
            value = if (dragging) dragPos else state.progress,
            onValueChange = { dragging = true; dragPos = it },
            onValueChangeFinished = { vm.player.seekTo(dragPos); dragging = false }
        )
        Row(Modifier.fillMaxWidth()) {
            Text(fmt(state.positionMs), style = Ds.Sub, color = cs.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(fmt(state.durationMs), style = Ds.Sub, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { vm.player.toggleShuffle() }) {
                Icon(Icons.Default.Shuffle, null, tint = if (state.shuffle) cs.primary else cs.onSurfaceVariant)
            }
            IconButton(onClick = { vm.player.prev() }) { Icon(Icons.Default.SkipPrevious, null, Modifier.size(36.dp), tint = cs.onBackground) }
            Surface(shape = CircleShape, color = cs.primary, modifier = Modifier.size(Ds.PlayHero).then(pressClick({ vm.player.toggle() }, min = 0.88f))) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = state.isPlaying, label = "pp-hero") { p ->
                        Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = cs.surface, modifier = Modifier.size(36.dp))
                    }
                }
            }
            IconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, null, Modifier.size(36.dp), tint = cs.onBackground) }
            IconButton(onClick = { vm.player.cycleRepeat() }) {
                val icon = when (state.repeatMode) { 2 -> Icons.Default.RepeatOne; 1 -> Icons.Default.Repeat; else -> Icons.Default.Repeat }
                Icon(icon, null, tint = if (state.repeatMode != 0) cs.primary else cs.onSurfaceVariant)
            }
        }
    }
}

// ── Diálogos ─────────────────────────────────────────────────────
// ── Ficha del tema (long-press): info útil + acciones ────────────
@Composable
fun TrackInfoSheet(song: Song, isFav: Boolean, onPlay: () -> Unit, onFav: () -> Unit, onAdd: () -> Unit, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val folder = try { java.io.File(song.dataPath).parent?.substringAfterLast("/") ?: "—" } catch (_: Exception) { "—" }
    val added = try {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(song.dateAdded * 1000))
    } catch (_: Exception) { "—" }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Info del tema", color = cs.onBackground) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SongArt(
                        albumId = song.albumId, size = 52.dp,
                        shape = RoundedCornerShape(Ds.RRow), iconSize = 26.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = Ds.Body, color = cs.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = Ds.Sub, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                InfoRow("Duración", fmt(song.durationMs))
                InfoRow("Álbum", song.album.ifBlank { "—" })
                InfoRow("Carpeta", folder)
                InfoRow("Agregado", added)
                Text("Ruta", style = Ds.Micro, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                Text(song.dataPath.ifBlank { "—" }, style = Ds.Sub, color = cs.onBackground)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = onPlay, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Play")
                    }
                    OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlaylistAdd, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("A lista")
                    }
                    IconButton(onClick = onFav) {
                        Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) cs.primary else cs.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = Ds.Sub, color = cs.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = Ds.Body, color = cs.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

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
    var presetMenu by remember { mutableStateOf(false) }
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
                Text("Se aplica en tiempo real y se guarda", style = Ds.Sub, color = cs.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Listo") } }
    )
}

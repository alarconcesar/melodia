package com.melodia.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodia.app.data.*
import com.melodia.app.data.local.PlaylistEntity
import com.melodia.app.data.local.PlaylistSongEntity
import com.melodia.app.data.model.Song
import com.melodia.app.data.lyrics.LyricLine
import com.melodia.app.data.lyrics.LyricsRepository
import com.melodia.app.data.online.DownloadManager
import com.melodia.app.data.online.YtRepository
import com.melodia.app.data.online.YtTrack
import com.melodia.app.player.MusicPlayer
import com.melodia.app.player.PlayerState
import com.melodia.app.player.SleepTimer
import com.melodia.app.player.YT_STREAM_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode { TITLE, ARTIST, RECENT, LONGEST }

class MainViewModel(
    val player: MusicPlayer,
    private val repo: MusicRepository,
    private val lists: PlaylistRepository,
    private val stats: StatsRepository,
    val sleep: SleepTimer,
    private val yt: YtRepository,
    private val dl: DownloadManager,
    private val lyricsRepo: LyricsRepository,
    private val artRepo: com.melodia.app.data.art.ArtworkRepository,
    private val queueStore: com.melodia.app.player.QueueStore
) : ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _sort = MutableStateFlow(SortMode.TITLE)
    val sort: StateFlow<SortMode> = _sort
    val playerState: StateFlow<PlayerState> = player.state
    private val _currentId = MutableStateFlow<Long?>(null)
    private val _streamSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = combine(_songs, _currentId, _streamSong) { list, id, stream ->
        @Suppress("UNCHECKED_CAST") val songs = list as List<Song>
        if (id == YT_STREAM_ID) stream else songs.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // selection filters
    private val _folder = MutableStateFlow<String?>(null)
    val folder: StateFlow<String?> = _folder
    private val _album = MutableStateFlow<String?>(null)
    val album: StateFlow<String?> = _album
    private val _artist = MutableStateFlow<String?>(null)
    val artistF: StateFlow<String?> = _artist

    val filtered: StateFlow<List<Song>> = combine(_songs, _query, _sort, _folder, _album, _artist) { args ->
        @Suppress("UNCHECKED_CAST") val list = args[0] as List<Song>; val q = args[1] as String
        val s = args[2] as SortMode; val f = args[3] as String?; val al = args[4] as String?; val ar = args[5] as String?
        var out = list
        if (f != null) out = out.filter { try { java.io.File(it.dataPath).parent == f } catch (_: Exception) { false } }
        if (al != null) out = out.filter { (it.album.ifBlank { "Sin álbum" }) == al }
        if (ar != null) out = out.filter { (it.artist.ifBlank { "Desconocido" }) == ar }
        if (q.isNotBlank()) out = out.filter { it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true) }
        when {
            // Viendo un álbum: manda el orden del disco, no el alfabético
            al != null -> out.sortedWith(AlbumTrackOrder)
            else -> when (s) {
                SortMode.TITLE -> out.sortedBy { it.title.lowercase() }
                SortMode.ARTIST -> out.sortedBy { it.artist.lowercase() }
                SortMode.RECENT -> out.sortedByDescending { it.dateAdded }
                SortMode.LONGEST -> out.sortedByDescending { it.durationMs }
            }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<MusicFolder>> = songs.map { groupFolders(it) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val albums: StateFlow<List<Album>> = songs.map { groupAlbums(it) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val artists: StateFlow<List<Artist>> = songs.map { groupArtists(it) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = lists.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _openPlaylist = MutableStateFlow<Long?>(null)
    val openPlaylist: StateFlow<Long?> = _openPlaylist
    val openPlaylistSongs: StateFlow<List<PlaylistSongEntity>> = _openPlaylist.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else lists.playlistSongs(pid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** Todos los vínculos playlist→canción (para portadas y conteos sin abrir cada una). */
    val allPlaylistSongs: StateFlow<List<PlaylistSongEntity>> =
        lists.allPlaylistSongs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _favIds = MutableStateFlow<Set<Long>>(emptySet())
    val favIds: StateFlow<Set<Long>> = _favIds
    val favSongs: StateFlow<List<Song>> = combine(_songs, _favIds) { list, fav -> list.filter { fav.contains(it.id) } }
        .flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _reload = MutableStateFlow(0)

    /** false hasta la primera lectura real de la biblioteca (evita parpadeo de "vacío"). */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    // OJO: estos estados los toca el init más abajo. Un StateFlow entrega su valor
    // al suscriptor de inmediato y sin suspender, así que deben existir ANTES del
    // init o el constructor revienta con NPE (la app no abría).
    /** videoId -> progreso 0..1 de descarga */
    private val _dlProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val dlProgress: StateFlow<Map<String, Float>> = _dlProgress
    /** Historial de descargas completadas. */
    private val _dlHistory = MutableStateFlow<List<YtRepository.DlRecord>>(emptyList())
    val dlHistory: StateFlow<List<YtRepository.DlRecord>> = _dlHistory

    // La restauración de cola corre desde el init: estos estados deben existir
    // ANTES o el constructor revienta con NPE (mismo bug que la 4.3).
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queueSongs: StateFlow<List<Song>> = _queue

    init {
        viewModelScope.launch {
            _reload.flatMapLatest { repo.loadAllSongs() }.collect { list ->
                player.setQueue(list)
                _songs.value = list
                _loaded.value = true
                maybeRestoreQueue(list)
                enrichArtwork(list)
            }
        }
        viewModelScope.launch { lists.favorites().collect { f -> _favIds.value = f.map { it.songId }.toSet() } }
        // URL de stream vencida o rechazada: re-resolver y continuar sin cortar
        player.onStreamError = { pos -> refreshStreamAudio(pos) }
        // Persistir la cola: la lista (JSON) solo cuando cambia su composición;
        // índice/shuffle/repeat como claves sueltas para no reescribir 5k temas
        // en cada cambio de pista.
        viewModelScope.launch {
            _queue.map { q -> q.map { it.id } }.distinctUntilChanged().collect {
                val q = _queue.value
                if (q.isNotEmpty()) queueStore.saveSongs(q)
            }
        }
        viewModelScope.launch {
            combine(_queue, player.state) { q, st -> Triple(q.firstOrNull { it.id == st.currentId }?.id ?: -1L, st.shuffle, st.repeatMode) }
                .distinctUntilChanged()
                .collect { (currentId, shuffle, repeat) ->
                    val q = _queue.value
                    if (q.isEmpty()) return@collect
                    val idx = q.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
                    queueStore.saveState(idx, player.state.value.positionMs, shuffle, repeat)
                }
        }
        // Posición cada 5 s (clave suelta: no reescribe toda la cola)
        viewModelScope.launch {
            var last = 0L
            player.state.collect { st ->
                val now = System.currentTimeMillis()
                if (now - last > 5_000 && _queue.value.isNotEmpty()) {
                    last = now
                    queueStore.savePosition(st.positionMs)
                }
            }
        }
        // Sincronizar UI con el item real de ExoPlayer (next/prev/fin de tema)
        viewModelScope.launch {
            player.state.map { it.currentId }.distinctUntilChanged().collect { id ->
                if (id > 0 || id == YT_STREAM_ID) _currentId.value = id
                // Estadísticas: cada tema local que empieza suma una reproducción
                if (id > 0) {
                    val dur = songs.value.find { it.id == id }?.durationMs ?: 0L
                    viewModelScope.launch { stats.recordPlay(id, dur) }
                }
            }
        }
        viewModelScope.launch {
            try { _ytRecent.value = yt.getRecent() } catch (_: Exception) {}
            try { _ytHistory.value = yt.getHistory() } catch (_: Exception) {}
        }
        // Letras: cargar automáticamente al cambiar de tema
        viewModelScope.launch {
            currentSong.collect { s -> if (s != null) loadLyrics(s) }
        }
        // Motor de descargas: progreso + guardado con etiquetas incrustadas
        viewModelScope.launch {
            try { dl.progress.collect { _dlProgress.value = it } } catch (_: Exception) {}
        }
        dl.onFinished = { track, high, audio, part ->
            // Letra y carátula para incrustar en el archivo
            val lyricText = try {
                val l = withContext(Dispatchers.IO) {
                    lyricsRepo.get(track.artist, track.title, "", track.durationSec, "dl:${track.id}")
                }
                l?.let { plainOrSynced(it) }
            } catch (_: Exception) { null }
            val art = try {
                track.thumbUrl?.let { withContext(Dispatchers.IO) { yt.fetchBytes(it) } }
            } catch (_: Exception) { null }
            withContext(Dispatchers.IO) { yt.storeDownload(track, high, audio, part, lyricText, art, null) }
            try {
                yt.pushDlHistory(track, high)
                _dlHistory.value = yt.getDlHistory()
            } catch (_: Exception) {}
            reload()
            // Precalienta la caché de letra con la clave del tema ya guardado
            viewModelScope.launch { delayWarmLyric(track) }
        }
    }

    private var queueRestored = false

    /**
     * Al abrir la app deja la última cola preparada y en pausa (no arranca sola).
     * Los temas que ya no están en la biblioteca se descartan y el índice se
     * recoloca sobre el mismo tema actual.
     */
    private fun maybeRestoreQueue(library: List<Song>) {
        if (queueRestored) return
        // Sin biblioteca (permiso denegado, MediaStore aún cargando) no se toca
        // el snapshot: se reintenta en la próxima carga.
        if (library.isEmpty()) return
        queueRestored = true
        if (_queue.value.isNotEmpty()) return
        val snapshot = queueStore.load()
        val plan = com.melodia.app.player.QueueRestore.plan(snapshot, library)
        if (plan == null) {
            if (snapshot != null) queueStore.clear()
            return
        }
        _queue.value = plan.songs
        _currentId.value = plan.songs[plan.index].id
        player.restore(
            plan.songs.map { it.id }, plan.index, plan.positionMs,
            shuffle = snapshot?.shuffle ?: false,
            repeatMode = snapshot?.repeatMode ?: 0
        )
    }

    /** Busca el tema recién descargado y deja su letra lista sin internet. */
    private suspend fun delayWarmLyric(track: YtTrack) {
        try {
            kotlinx.coroutines.delay(2500)
            val s = songs.value.firstOrNull {
                it.title.equals(track.title, true) &&
                    (track.artist.isBlank() || it.artist.equals(track.artist, true))
            } ?: return
            val l = withContext(Dispatchers.IO) {
                lyricsRepo.get(s.artist, s.title, s.album, s.durationMs / 1000, "id:${s.id}")
            }
            if (l != null) loadLyrics(s, force = false)
        } catch (_: Exception) {}
    }

    /** Texto incrustable: sincronizado si hay, si no el plano. */
    private fun plainOrSynced(l: com.melodia.app.data.lyrics.Lyrics): String? {
        if (l.synced.isNotEmpty()) {
            return l.synced.joinToString("\n") { ln ->
                val s = (ln.ms / 1000).toInt()
                "[${"%02d".format(s / 60)}:${"%02d".format(s % 60)}]${ln.text}"
            }.take(60000).ifBlank { null } ?: l.plain
        }
        return l.plain
    }

    fun reload() { _reload.value++ }

    // ── Biblioteca: filtros y metadatos ──
    val minSecs: StateFlow<Int> = repo.minSecs
    val scanFolder: StateFlow<String?> = repo.scanFolder

    fun setMinSecs(s: Int) {
        repo.setMinSecs(s)
        reload()
    }

    fun setScanFolder(path: String?) {
        repo.setScanFolder(path)
        reload()
    }

    fun updateMetadata(s: Song, title: String, artist: String, album: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.updateMetadata(s.id, title, artist, album) }
            if (ok) reload()
            onDone(ok)
        }
    }

    fun setQuery(q: String) { _query.value = q }

    // ── Búsqueda rápida de YouTube desde la barra de Inicio ──
    // Una sola página y sin tocar el paginador de la pestaña Online.
    private val _homeYt = MutableStateFlow<List<com.melodia.app.data.online.YtSearchItem>>(emptyList())
    val homeYt: StateFlow<List<com.melodia.app.data.online.YtSearchItem>> = _homeYt
    private val _homeYtLoading = MutableStateFlow(false)
    val homeYtLoading: StateFlow<Boolean> = _homeYtLoading
    private var homeYtJob: kotlinx.coroutines.Job? = null

    fun searchHomeYt(q: String = _query.value) {
        val query = q.trim()
        homeYtJob?.cancel()
        if (query.length < 2) {
            _homeYt.value = emptyList()
            _homeYtLoading.value = false
            return
        }
        homeYtJob = viewModelScope.launch {
            _homeYtLoading.value = true
            try {
                _homeYt.value = withContext(Dispatchers.IO) { yt.searchFirstPage(query) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _homeYt.value = emptyList()
            } finally {
                _homeYtLoading.value = false
            }
        }
    }

    fun clearHomeYt() {
        homeYtJob?.cancel()
        _homeYt.value = emptyList()
        _homeYtLoading.value = false
    }
    fun setSort(s: SortMode) { _sort.value = s }
    fun setFolder(p: String?) { _folder.value = p; _album.value = null; _artistF2(null) }
    fun setAlbum(a: String?) { _album.value = a; _folder.value = null; _artistF2(null) }
    fun setArtist(a: String?) { _artistF2(a); _folder.value = null; _album.value = null }
    private fun _artistF2(a: String?) { _artist.value = a }
    fun clearFilters() { _folder.value = null; _album.value = null; _artist.value = null }
    fun openPl(pid: Long?) { _openPlaylist.value = pid }

    // ── Cola de reproducción visible ──
    private fun emitPlay(list: List<Song>, idx: Int) {
        if (list.isEmpty()) return
        // Tocar el tema que ya suena pausa/reanuda: antes se reiniciaba desde cero.
        if (_currentId.value == list[idx.coerceIn(0, list.size - 1)].id) {
            player.toggle()
            return
        }
        _queue.value = list
        _currentId.value = list[idx.coerceIn(0, list.size - 1)].id
        player.play(list.map { it.id }, idx.coerceIn(0, list.size - 1))
    }
    fun playQueueAt(idx: Int) {
        val q = _queue.value
        if (q.isEmpty() || idx !in q.indices) return
        if (q.any { it.id == YT_STREAM_ID }) return // stream: cola de 1, nada que saltar
        _currentId.value = q[idx].id
        player.play(q.map { it.id }, idx)
    }

    fun removeQueueAt(idx: Int) {
        val q = _queue.value
        if (q.isEmpty() || idx !in q.indices) return
        if (q.any { it.id == YT_STREAM_ID }) return
        if (q.size <= 1) return
        player.removeAt(idx)
        _queue.value = q.filterIndexed { i, _ -> i != idx }
    }

    /**
     * Reordena la cola (botones subir/bajar de la hoja). Solo se actualiza el
     * estado de la UI si el reproductor aceptó el movimiento: así nunca se ve
     * una cola distinta a la que está sonando.
     */
    fun moveQueueAt(from: Int, to: Int): Boolean {
        val q = _queue.value
        if (q.any { it.id == YT_STREAM_ID }) return false
        val moved = com.melodia.app.player.QueueOps.move(q, from, to) ?: return false
        if (!player.moveItem(from, to)) return false
        _queue.value = moved
        return true
    }

    /** Quita todo menos el tema actual (sigue sonando sin corte). */
    fun clearQueue() {
        val q = _queue.value
        if (q.any { it.id == YT_STREAM_ID }) return
        if (q.size <= 1) return
        if (!player.clearQueueKeepCurrent()) return
        val cur = player.state.value.currentId
        val idx = q.indexOfFirst { it.id == cur }
        _queue.value = com.melodia.app.player.QueueOps.keepOnly(q, idx)
    }

    /** Guarda la cola actual como playlist nueva, en el mismo orden. */
    fun saveQueueAsPlaylist(name: String, onDone: (Int) -> Unit = {}) {
        val songs = _queue.value.filter { it.id != YT_STREAM_ID }.distinctBy { it.id }
        if (name.isBlank() || songs.isEmpty()) { onDone(0); return }
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    val pid = lists.createPlaylist(name)
                    var n = 0
                    songs.forEach { s -> if (lists.addToPlaylist(pid, s)) n++ }
                    n
                }
            } catch (_: Exception) { 0 }
            onDone(saved)
        }
    }

    fun playSong(id: Long) {
        val list = filtered.value.ifEmpty { songs.value }
        val idx = list.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        emitPlay(list, idx)
    }
    /** Reproduce por id desde toda la biblioteca (historial, estadísticas). */
    fun playById(id: Long) {
        val list = songs.value
        val idx = list.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        emitPlay(list, idx)
    }
    fun playShuffle() {
        val list = filtered.value.ifEmpty { songs.value }
        if (list.isEmpty()) return
        emitPlay(list, (list.indices).random())
    }
    fun playListAt(list: List<Song>, idx: Int) = emitPlay(list, idx)
    fun playFavAt(idx: Int) = emitPlay(favSongs.value, idx)
    // ── Artista / álbum: reproducir y aleatorio propios ──
    private fun artistSongs(name: String) =
        songs.value.filter { (it.artist.ifBlank { "Desconocido" }) == name }
    private fun albumSongs(name: String) =
        songs.value.filter { (it.album.ifBlank { "Sin álbum" }) == name }.sortedWith(AlbumTrackOrder)
    fun playArtist(name: String) {
        val l = artistSongs(name)
        if (l.isNotEmpty()) emitPlay(l, 0)
    }
    fun shuffleArtist(name: String) {
        val l = artistSongs(name).shuffled()
        if (l.isNotEmpty()) emitPlay(l, 0)
    }
    fun playAlbum(name: String) {
        val l = albumSongs(name)
        if (l.isNotEmpty()) emitPlay(l, 0)
    }
    fun shuffleAlbum(name: String) {
        val l = albumSongs(name).shuffled()
        if (l.isNotEmpty()) emitPlay(l, 0)
    }
    fun playPlaylistEntities(e: List<PlaylistSongEntity>, idx: Int) {
        val byId = songs.value.associateBy { it.id }
        val list = e.mapNotNull { byId[it.songId] }
        if (list.isNotEmpty()) emitPlay(list, idx.coerceIn(0, list.size - 1))
    }
    fun shufflePlaylistEntities(e: List<PlaylistSongEntity>) {
        val byId = songs.value.associateBy { it.id }
        val list = e.mapNotNull { byId[it.songId] }.shuffled()
        if (list.isNotEmpty()) emitPlay(list, 0)
    }
    fun moveInPlaylist(pid: Long, sid: Long, up: Boolean) {
        viewModelScope.launch { lists.moveSong(pid, sid, up) }
    }

    // ── Tono y velocidad ──
    fun setPitch(p: Float) { player.setPitch(p) }
    fun setSpeed(s: Float) { player.setSpeed(s) }
    fun resetPitchSpeed() { player.resetPitchSpeed() }

    // ── A continuación / al final ──
    fun queueNext(id: Long, onDone: (Boolean) -> Unit = {}) {
        val s = songs.value.find { it.id == id } ?: run { onDone(false); return }
        if (!player.playNext(id)) { onDone(false); return }
        val q = _queue.value.toMutableList()
        val at = (player.state.value.currentIndex + 1).coerceIn(0, q.size)
        q.add(at, s)
        _queue.value = q
        onDone(true)
    }

    fun queueEnd(id: Long, onDone: (Boolean) -> Unit = {}) {
        val s = songs.value.find { it.id == id } ?: run { onDone(false); return }
        if (!player.addToEnd(id)) { onDone(false); return }
        _queue.value = _queue.value + s
        onDone(true)
    }

    // ── Fundido entre canciones ──
    fun toggleCrossfade() { player.setCrossfade(!player.state.value.crossfade) }
    fun setCrossfadeSecs(s: Int) { player.setCrossfadeSecs(s) }

    fun toggleFav(id: Long) { viewModelScope.launch { lists.toggleFav(id, _favIds.value.contains(id)) } }
    fun createPlaylist(name: String, onDone: (Long) -> Unit = {}) { viewModelScope.launch { val id = lists.createPlaylist(name); onDone(id) } }
    fun deletePlaylist(id: Long) { viewModelScope.launch { lists.deletePlaylist(id); if (_openPlaylist.value == id) _openPlaylist.value = null } }
    fun renamePlaylist(id: Long, name: String) { viewModelScope.launch { lists.rename(id, name) } }
    fun togglePinPlaylist(id: Long) { viewModelScope.launch { lists.togglePin(id) } }
    fun setPlaylistCover(id: Long, albumId: Long?) { viewModelScope.launch { lists.setCover(id, albumId) } }
    fun addToPlaylist(pid: Long, s: Song, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = lists.addToPlaylist(pid, s)
            onResult(ok)
        }
    }
    fun removeFromPlaylist(pid: Long, sid: Long) { viewModelScope.launch { lists.removeFromPlaylist(pid, sid) } }

    // ── Estadísticas e historial ──
    val statRows: StateFlow<List<com.melodia.app.data.local.StatEntity>> =
        stats.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Temas escuchados recientemente (para "Historial" en Inicio). */
    val recentPlayed: StateFlow<List<Song>> =
        combine(_songs, statRows) { list, st ->
            val last = st.associate { it.songId to it.lastPlayedAt }
            @Suppress("UNCHECKED_CAST")
            (list as List<Song>).filter { last.containsKey(it.id) }
                .sortedByDescending { last[it.id] ?: 0L }.take(10)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    data class StatsSummary(
        val plays: Int = 0, val minutes: Long = 0L,
        val topSongs: List<Pair<Song, Int>> = emptyList(),
        val topArtists: List<Pair<String, Int>> = emptyList()
    )

    val statsSummary: StateFlow<StatsSummary> =
        combine(_songs, statRows) { list, st ->
            @Suppress("UNCHECKED_CAST") val songs = list as List<Song>
            val byId = songs.associateBy { it.id }
            val plays = st.sumOf { it.plays }
            val minutes = st.sumOf { it.listenMs } / 60000
            val topSongs = st.sortedByDescending { it.plays }
                .mapNotNull { e -> byId[e.songId]?.let { it to e.plays } }.take(5)
            val artPlays = mutableMapOf<String, Int>()
            st.forEach { e ->
                val a = byId[e.songId]?.artist?.ifBlank { "Desconocido" } ?: return@forEach
                artPlays[a] = (artPlays[a] ?: 0) + e.plays
            }
            val topArtists = artPlays.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
            StatsSummary(plays, minutes, topSongs, topArtists)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsSummary())

    fun clearStats() { viewModelScope.launch { stats.clear() } }

    // ── Respaldo y restauración (JSON en Descargas/Melodia) ──
    fun backup(ctx: android.content.Context, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { lists.exportJson() }
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "melodia-respaldo.json")
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/Melodia")
                }
                val uri = ctx.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri == null) { onDone("No se pudo crear el archivo"); return@launch }
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
                onDone("Respaldo guardado en Descargas/Melodia")
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                onDone("No se pudo crear el respaldo")
            }
        }
    }

    fun restore(ctx: android.content.Context, uri: android.net.Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                val root = org.json.JSONObject(text)
                val lib = songs.value
                val byPath = lib.associateBy { it.dataPath }
                var pls = 0; var added = 0; var favs = 0
                val arr = root.optJSONArray("playlists") ?: org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.optString("name", "Lista").ifBlank { "Lista" }
                    val pid = lists.createPlaylist("$name (respaldo)")
                    pls++
                    val sarr = o.optJSONArray("songs") ?: org.json.JSONArray()
                    for (j in 0 until sarr.length()) {
                        val e = sarr.getJSONObject(j)
                        val match = byPath[e.optString("path")] ?: lib.find {
                            it.title.equals(e.optString("title"), true) &&
                                it.artist.equals(e.optString("artist"), true)
                        } ?: continue
                        if (lists.addToPlaylist(pid, match)) added++
                    }
                }
                val farr = root.optJSONArray("favs") ?: org.json.JSONArray()
                for (i in 0 until farr.length()) {
                    val id = farr.optLong(i, -1)
                    if (lib.any { it.id == id } && !_favIds.value.contains(id)) {
                        lists.toggleFav(id, false); favs++
                    }
                }
                onDone("Restaurado: $pls listas, $added temas, $favs favoritos")
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                onDone("Archivo inválido — no se pudo restaurar")
            }
        }
    }

    // ── Carátulas online para temas sin arte ──
    private var artJob: kotlinx.coroutines.Job? = null

    /** Clave para compartir una portada entre los temas del mismo álbum. */
    private fun albumKey(s: Song): String =
        if (s.album.isBlank()) "id:${s.id}"
        else "a:${s.album.lowercase()}|${s.artist.lowercase()}"

    /**
     * Carátulas que faltan: caché local, luego incrustadas en el archivo y al
     * final búsqueda en red. Todo corre en IO: antes el bucle llamaba a
     * MediaMetadataRetriever en el hilo principal y clavaba frames de 300 ms.
     */
    private fun enrichArtwork(list: List<Song>) {
        artJob?.cancel()
        artJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    var actual = list.associateBy { it.id }

                    // 1) caché local, al instante
                    val cached = list.mapNotNull { s ->
                        if (s.albumId > 0 || !s.artUrl.isNullOrBlank()) return@mapNotNull null
                        artRepo.cached(s.artist, s.title)?.let { s.id to it }
                    }.toMap()
                    if (cached.isNotEmpty()) {
                        val upd = _songs.value.map { s -> cached[s.id]?.let { s.copy(artUrl = it) } ?: s }
                        _songs.value = upd
                        actual = upd.associateBy { it.id }
                    }

                    // 2) carátula incrustada en el archivo (máx. 80 grupos de álbum)
                    val found = mutableMapOf<String, String>()
                    var emb = 0
                    for (s in list) {
                        if (emb >= 80) break
                        val cur = actual[s.id] ?: continue
                        if (cur.albumId > 0 || !cur.artUrl.isNullOrBlank()) continue
                        val group = albumKey(cur)
                        if (found.containsKey(group)) continue
                        val local = try { artRepo.embedded(cur) } catch (_: Exception) { null }
                        if (local != null) found[group] = local
                        emb++
                    }
                    if (found.isNotEmpty()) {
                        val upd = _songs.value.map { s ->
                            found[albumKey(s)]?.let { s.copy(artUrl = it) } ?: s
                        }
                        _songs.value = upd
                        actual = upd.associateBy { it.id }
                    }

                    // 3) red, solo lo que sigue sin arte (máx. 50 por carga)
                    var n = 0
                    for (s in list) {
                        if (n >= 50) break
                        val cur = actual[s.id] ?: continue
                        if (cur.albumId > 0 || !cur.artUrl.isNullOrBlank()) continue
                        if (cur.artist.isBlank() || cur.artist == "Desconocido") continue
                        val url = try { artRepo.fetch(cur.artist, cur.title) } catch (_: Exception) { null }
                        if (!url.isNullOrBlank()) {
                            val upd = _songs.value.map { x -> if (x.id == s.id) x.copy(artUrl = url) else x }
                            _songs.value = upd
                            actual = actual + (s.id to s.copy(artUrl = url))
                        }
                        n++
                        kotlinx.coroutines.delay(250)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // ── Online (YouTube: buscar, escuchar, descargar) ──
    private val _ytQuery = MutableStateFlow("")
    val ytQuery: StateFlow<String> = _ytQuery
    private val _ytResults = MutableStateFlow<List<com.melodia.app.data.online.YtSearchItem>>(emptyList())
    val ytResults: StateFlow<List<com.melodia.app.data.online.YtSearchItem>> = _ytResults
    /** Aviso efímero de la pestaña Online (p. ej. "playlist en cola"). */
    private val _ytMsg = MutableStateFlow<String?>(null)
    val ytMsg: StateFlow<String?> = _ytMsg
    private val _ytLoading = MutableStateFlow(false)
    val ytLoading: StateFlow<Boolean> = _ytLoading
    private val _ytError = MutableStateFlow<String?>(null)
    val ytError: StateFlow<String?> = _ytError
    /** false = reintentar la misma búsqueda no sirve (YouTube no la tiene). */
    private val _ytErrorRetry = MutableStateFlow(true)
    val ytErrorRetry: StateFlow<Boolean> = _ytErrorRetry
    /** videoId cuyo audio se está resolviendo para reproducir */
    private val _busyYt = MutableStateFlow<String?>(null)
    val busyYt: StateFlow<String?> = _busyYt
    /** tema online sonando ahora (para descargar desde el reproductor) */
    private val _currentTrack = MutableStateFlow<YtTrack?>(null)
    val currentTrack: StateFlow<YtTrack?> = _currentTrack
    private val _ytHasMore = MutableStateFlow(false)
    val ytHasMore: StateFlow<Boolean> = _ytHasMore
    private val _ytMoreLoading = MutableStateFlow(false)
    val ytMoreLoading: StateFlow<Boolean> = _ytMoreLoading
    private val _ytRecent = MutableStateFlow<List<String>>(emptyList())
    val ytRecent: StateFlow<List<String>> = _ytRecent
    private val _ytHistory = MutableStateFlow<List<YtTrack>>(emptyList())
    val ytHistory: StateFlow<List<YtTrack>> = _ytHistory
    /** Calidad de descarga preferida: true = alta, false = estándar (liviana). */
    private val _ytHighQuality = MutableStateFlow(true)
    val ytHighQuality: StateFlow<Boolean> = _ytHighQuality
    /** Ahorro de datos: el streaming online usa bitrate modesto (solo audio, sin video). */
    private val _dataSaver = MutableStateFlow(false)
    val dataSaver: StateFlow<Boolean> = _dataSaver
    fun setYtQuality(high: Boolean) { _ytHighQuality.value = high }
    fun setDataSaver(on: Boolean) { _dataSaver.value = on }
    fun refreshDlHistory() { try { _dlHistory.value = yt.getDlHistory() } catch (_: Exception) {} }
    fun clearDlHistory() { viewModelScope.launch { withContext(Dispatchers.IO) { yt.clearDlHistory() }; _dlHistory.value = emptyList() } }

    private fun errMsg(t: Throwable, fallback: String): String =
        com.melodia.app.data.online.YtErrors.message(t, fallback)

    fun setYtQuery(q: String) { _ytQuery.value = q }
    private var searchJob: kotlinx.coroutines.Job? = null
    fun searchYt(q: String = _ytQuery.value) {
        val query = q.trim()
        if (query.isBlank()) return
        searchJob?.cancel() // evita que una búsqueda anterior pise a la nueva
        searchJob = viewModelScope.launch {
            _ytLoading.value = true
            _ytError.value = null
            _ytErrorRetry.value = true
            try {
                _ytResults.value = yt.search(query)
                _ytHasMore.value = yt.hasMore()
                if (_ytResults.value.isEmpty()) {
                    _ytError.value = "Sin resultados — prueba con otras palabras"
                    _ytErrorRetry.value = false
                } else {
                    try {
                        yt.pushRecent(query)
                        _ytRecent.value = yt.getRecent()
                    } catch (_: Exception) {}
                    // Prefetch del primer resultado: el play más común arranca al instante
                    // (hijo de este job: se cancela si llega otra búsqueda)
                    val top = _ytResults.value.filterIsInstance<com.melodia.app.data.online.YtSearchItem.Track>()
                        .firstOrNull()
                    if (top != null) {
                        launch {
                            try { withContext(Dispatchers.IO) { yt.prefetchAudio(top.track.url, !_dataSaver.value) } } catch (_: Exception) {}
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytError.value = errMsg(t, "YouTube no respondió — reintenta en un momento")
                _ytErrorRetry.value = com.melodia.app.data.online.YtErrors.retryable(t)
            }
            _ytLoading.value = false
        }
    }

    fun loadMoreYt() {
        if (_ytMoreLoading.value || !_ytHasMore.value) return
        viewModelScope.launch {
            _ytMoreLoading.value = true
            try {
                val extra = withContext(Dispatchers.IO) { yt.more() }
                if (extra.isNotEmpty()) {
                    val known = _ytResults.value.map { it.key }.toSet()
                    _ytResults.value = _ytResults.value + extra.filter { !known.contains(it.key) }
                }
                _ytHasMore.value = yt.hasMore()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytHasMore.value = false
            }
            _ytMoreLoading.value = false
        }
    }

    private var refreshingStream = false

    /**
     * YouTube invalidó la URL de audio (403/404/410): se re-resuelve con
     * [YtRepository.bestAudio] y se reemplaza en el player desde [positionMs],
     * sin reiniciar el tema ni tocar la cola.
     */
    private fun refreshStreamAudio(positionMs: Long) {
        val track = _currentTrack.value ?: return
        if (refreshingStream) return
        refreshingStream = true
        viewModelScope.launch {
            try {
                val audio = withContext(Dispatchers.IO) {
                    yt.bestAudio(track.url, !_dataSaver.value, force = true)
                }
                player.swapStream(audio.url, positionMs)
            } catch (_: Exception) {
                _ytError.value = "No se pudo recuperar el audio. Reintenta en un momento."
            } finally {
                refreshingStream = false
            }
        }
    }

    fun playYt(track: YtTrack) {
        if (_busyYt.value != null) return
        viewModelScope.launch {
            _busyYt.value = track.id
            _ytError.value = null
            try {
                // Solo-audio real: nunca se toca el video; con ahorro usa bitrate modesto
                val audio = withContext(Dispatchers.IO) { yt.bestAudio(track.url, !_dataSaver.value) }
                _currentTrack.value = track
                val stream = Song(
                    id = YT_STREAM_ID, title = track.title,
                    artist = track.artist.ifBlank { "YouTube" },
                    album = "Online", albumId = -1L, durationMs = track.durationSec * 1000,
                    dataPath = audio.url, dateAdded = 0L, artUrl = track.thumbUrl
                )
                _streamSong.value = stream
                _queue.value = listOf(stream)
                _currentId.value = YT_STREAM_ID
                player.playStream(audio.url, track.title, track.artist, track.thumbUrl)
                try {
                    yt.pushHistory(track)
                    _ytHistory.value = yt.getHistory()
                } catch (_: Exception) {}
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytError.value = errMsg(t, "No se pudo reproducir — reintenta")
            }
            _busyYt.value = null
        }
    }

    // ── Letras sincronizadas ──
    sealed interface LyricsUi {
        data object Idle : LyricsUi
        data object Loading : LyricsUi
        data class Synced(val lines: List<LyricLine>) : LyricsUi
        data class Plain(val text: String) : LyricsUi
        data object None : LyricsUi
    }
    private val _lyrics = MutableStateFlow<LyricsUi>(LyricsUi.Idle)
    val lyrics: StateFlow<LyricsUi> = _lyrics
    private val lyricsMem = mutableMapOf<String, LyricsUi>()
    private var lyricsJob: kotlinx.coroutines.Job? = null

    fun loadLyrics(s: Song, force: Boolean = false) {
        val key = if (s.id == YT_STREAM_ID) "yt:${s.dataPath.hashCode()}" else "id:${s.id}"
        if (!force) lyricsMem[key]?.let { _lyrics.value = it; return }
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _lyrics.value = LyricsUi.Loading
            try {
                val dur = (s.durationMs / 1000).coerceAtLeast(0)
                val got = withContext(Dispatchers.IO) {
                    lyricsRepo.get(s.artist.ifBlank { "" }, s.title, s.album.ifBlank { "" }, dur, key)
                }
                val ui: LyricsUi = when {
                    got == null -> LyricsUi.None
                    got.instrumental -> LyricsUi.Plain("Tema instrumental — sin letra")
                    got.synced.size >= 3 -> LyricsUi.Synced(got.synced)
                    got.plain != null -> LyricsUi.Plain(got.plain)
                    else -> LyricsUi.None
                }
                lyricsMem[key] = ui
                if (lyricsMem.size > 40) lyricsMem.remove(lyricsMem.keys.first())
                _lyrics.value = ui
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _lyrics.value = lyricsMem[key] ?: LyricsUi.None
            }
        }
    }

    /** Descarga todos los resultados actuales (el motor los encola de 3 en 3). */
    fun downloadAllYt() {
        val list = _ytResults.value.filterIsInstance<com.melodia.app.data.online.YtSearchItem.Track>()
            .map { it.track }
            .filter { t -> dlJobs.value.none { it.track.id == t.id } }
        if (list.isEmpty()) return
        dl.enqueueAll(list, _ytHighQuality.value)
    }

    /** Descarga entera una playlist o álbum que salió en la búsqueda. */
    fun downloadYtCollection(col: com.melodia.app.data.online.YtSearchItem.Collection) {
        if (_busyYt.value != null) return
        viewModelScope.launch {
            _busyYt.value = col.key
            _ytMsg.value = null
            try {
                val (title, tracks) = withContext(Dispatchers.IO) { yt.playlistTracks(col.url) }
                if (tracks.isEmpty()) {
                    _ytMsg.value = "«${col.title}» no tiene temas visibles"
                } else {
                    dl.enqueueAll(tracks, _ytHighQuality.value)
                    _ytMsg.value = "«$title»: ${tracks.size} temas a la cola de descargas"
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytMsg.value = errMsg(t, "No se pudo leer la playlist")
            } finally {
                _busyYt.value = null
            }
        }
    }

    fun clearYtMsg() { _ytMsg.value = null }

    fun retryLyrics() { currentSong.value?.let { loadLyrics(it, force = true) } }

    // ── Descargas: cola con pausa, reintento e importación de playlists ──
    val dlJobs: StateFlow<List<com.melodia.app.data.online.DlJob>> = dl.jobs

    fun downloadYt(track: YtTrack, high: Boolean = _ytHighQuality.value) {
        dl.enqueue(track, high)
    }

    fun pauseDl(id: String) = dl.pause(id)
    fun resumeDl(id: String) = dl.resume(id)
    fun cancelDl(id: String) = dl.cancel(id)

    private val _ytImport = MutableStateFlow<String?>(null)
    val ytImport: StateFlow<String?> = _ytImport

    /** Pega un link de playlist de YouTube y encola todos sus temas. */
    fun importYtPlaylist(url: String) {
        val clean = url.trim()
        if (!clean.contains("list=", true)) {
            _ytImport.value = "Pega un link de playlist (lleva “list=” en la URL)"
            return
        }
        if (_ytImport.value == "…") return
        viewModelScope.launch {
            _ytImport.value = "…"
            try {
                val (title, tracks) = withContext(Dispatchers.IO) { yt.playlistTracks(clean) }
                if (tracks.isEmpty()) {
                    _ytImport.value = "La playlist no trae temas visibles"
                    return@launch
                }
                dl.enqueueAll(tracks, _ytHighQuality.value)
                _ytImport.value = "OK|${tracks.size}|$title"
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytImport.value = errMsg(t, "No se pudo leer la playlist")
            }
        }
    }

    fun clearYtImport() { _ytImport.value = null }
}

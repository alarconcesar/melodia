package com.melodia.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodia.app.data.*
import com.melodia.app.data.local.PlaylistEntity
import com.melodia.app.data.local.PlaylistSongEntity
import com.melodia.app.data.model.Song
import com.melodia.app.data.lyrics.LyricLine
import com.melodia.app.data.lyrics.LyricsRepository
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
    val sleep: SleepTimer,
    private val yt: YtRepository,
    private val lyricsRepo: LyricsRepository
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
        when (s) {
            SortMode.TITLE -> out.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> out.sortedBy { it.artist.lowercase() }
            SortMode.RECENT -> out.sortedByDescending { it.dateAdded }
            SortMode.LONGEST -> out.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<MusicFolder>> = songs.map { groupFolders(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val albums: StateFlow<List<Album>> = songs.map { groupAlbums(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val artists: StateFlow<List<Artist>> = songs.map { groupArtists(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = lists.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _openPlaylist = MutableStateFlow<Long?>(null)
    val openPlaylist: StateFlow<Long?> = _openPlaylist
    val openPlaylistSongs: StateFlow<List<PlaylistSongEntity>> = _openPlaylist.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else lists.playlistSongs(pid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _favIds = MutableStateFlow<Set<Long>>(emptySet())
    val favIds: StateFlow<Set<Long>> = _favIds
    val favSongs: StateFlow<List<Song>> = combine(_songs, _favIds) { list, fav -> list.filter { fav.contains(it.id) } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            _reload.flatMapLatest { repo.loadAllSongs() }.collect { list ->
                player.setQueue(list)
                _songs.value = list
            }
        }
        viewModelScope.launch { lists.favorites().collect { f -> _favIds.value = f.map { it.songId }.toSet() } }
        // Sincronizar UI con el item real de ExoPlayer (next/prev/fin de tema)
        viewModelScope.launch {
            player.state.map { it.currentId }.distinctUntilChanged().collect { id ->
                if (id > 0 || id == YT_STREAM_ID) _currentId.value = id
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
    }

    fun reload() { _reload.value++ }

    fun setQuery(q: String) { _query.value = q }
    fun setSort(s: SortMode) { _sort.value = s }
    fun setFolder(p: String?) { _folder.value = p; _album.value = null; _artistF2(null) }
    fun setAlbum(a: String?) { _album.value = a; _folder.value = null; _artistF2(null) }
    fun setArtist(a: String?) { _artistF2(a); _folder.value = null; _album.value = null }
    private fun _artistF2(a: String?) { _artist.value = a }
    fun clearFilters() { _folder.value = null; _album.value = null; _artist.value = null }
    fun openPl(pid: Long?) { _openPlaylist.value = pid }

    // ── Cola de reproducción visible ──
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queueSongs: StateFlow<List<Song>> = _queue

    private fun emitPlay(list: List<Song>, idx: Int) {
        if (list.isEmpty()) return
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

    fun playSong(id: Long) {
        val list = filtered.value.ifEmpty { songs.value }
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
    fun playPlaylistEntities(e: List<PlaylistSongEntity>, idx: Int) {
        val byId = songs.value.associateBy { it.id }
        val list = e.mapNotNull { byId[it.songId] }
        if (list.isNotEmpty()) emitPlay(list, idx.coerceIn(0, list.size - 1))
    }

    fun toggleFav(id: Long) { viewModelScope.launch { lists.toggleFav(id, _favIds.value.contains(id)) } }
    fun createPlaylist(name: String, onDone: (Long) -> Unit = {}) { viewModelScope.launch { val id = lists.createPlaylist(name); onDone(id) } }
    fun deletePlaylist(id: Long) { viewModelScope.launch { lists.deletePlaylist(id); if (_openPlaylist.value == id) _openPlaylist.value = null } }
    fun renamePlaylist(id: Long, name: String) { viewModelScope.launch { lists.rename(id, name) } }
    fun addToPlaylist(pid: Long, s: Song) { viewModelScope.launch { lists.addToPlaylist(pid, s) } }
    fun removeFromPlaylist(pid: Long, sid: Long) { viewModelScope.launch { lists.removeFromPlaylist(pid, sid) } }

    // ── Online (YouTube: buscar, escuchar, descargar) ──
    private val _ytQuery = MutableStateFlow("")
    val ytQuery: StateFlow<String> = _ytQuery
    private val _ytResults = MutableStateFlow<List<YtTrack>>(emptyList())
    val ytResults: StateFlow<List<YtTrack>> = _ytResults
    private val _ytLoading = MutableStateFlow(false)
    val ytLoading: StateFlow<Boolean> = _ytLoading
    private val _ytError = MutableStateFlow<String?>(null)
    val ytError: StateFlow<String?> = _ytError
    /** videoId -> progreso 0..1 de descarga */
    private val _dlProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val dlProgress: StateFlow<Map<String, Float>> = _dlProgress
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

    private fun errMsg(t: Throwable, fallback: String): String {
        val detail = "${t.javaClass.simpleName}${if (!t.message.isNullOrBlank()) ": ${t.message}" else ""}"
        return "$fallback\n($detail)"
    }

    fun setYtQuery(q: String) { _ytQuery.value = q }
    fun searchYt(q: String = _ytQuery.value) {
        val query = q.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _ytLoading.value = true
            _ytError.value = null
            try {
                _ytResults.value = yt.search(query)
                _ytHasMore.value = yt.hasMore()
                if (_ytResults.value.isEmpty()) _ytError.value = "Sin resultados — prueba con otras palabras"
                else {
                    try {
                        yt.pushRecent(query)
                        _ytRecent.value = yt.getRecent()
                    } catch (_: Exception) {}
                    // Prefetch del primer resultado: el play más común arranca al instante
                    val top = _ytResults.value.firstOrNull()
                    if (top != null) {
                        viewModelScope.launch {
                            try { withContext(Dispatchers.IO) { yt.prefetchAudio(top.url) } } catch (_: Exception) {}
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytError.value = errMsg(t, "Sin conexión o YouTube no responde — reintenta")
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
                    val known = _ytResults.value.map { it.id }.toSet()
                    _ytResults.value = _ytResults.value + extra.filter { !known.contains(it.id) }
                }
                _ytHasMore.value = yt.hasMore()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytHasMore.value = false
            }
            _ytMoreLoading.value = false
        }
    }

    fun playYt(track: YtTrack) {
        if (_busyYt.value != null) return
        viewModelScope.launch {
            _busyYt.value = track.id
            _ytError.value = null
            try {
                val audio = withContext(Dispatchers.IO) { yt.bestAudio(track.url) }
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
                    got.instrumental -> LyricsUi.Plain("🎵 Tema instrumental — sin letra")
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

    /** Descarga todos los resultados actuales (con pausa entre cada uno). */
    fun downloadAllYt() {
        viewModelScope.launch {
            val list = _ytResults.value.filter { !_dlProgress.value.containsKey(it.id) }
            for (t in list) {
                if (_dlProgress.value.size >= 3) {
                    // máx. 3 en paralelo: espera a que se libere uno
                    while (_dlProgress.value.size >= 3) kotlinx.coroutines.delay(1000)
                }
                downloadYt(t)
                kotlinx.coroutines.delay(400)
            }
        }
    }

    fun retryLyrics() { currentSong.value?.let { loadLyrics(it, force = true) } }

    fun downloadYt(track: YtTrack) {
        if (_dlProgress.value.containsKey(track.id)) return
        viewModelScope.launch {
            _dlProgress.value += track.id to 0f
            try {
                withContext(Dispatchers.IO) {
                    yt.download(track) { p -> _dlProgress.value += track.id to p }
                }
                reload()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytError.value = errMsg(t, "No se pudo descargar — reintenta")
            }
            // quitar el entry tras un respiro para que se vea el 100%
            kotlinx.coroutines.delay(600)
            _dlProgress.value -= track.id
        }
    }
}

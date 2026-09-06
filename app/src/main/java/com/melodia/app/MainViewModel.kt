package com.melodia.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodia.app.data.*
import com.melodia.app.data.local.PlaylistEntity
import com.melodia.app.data.local.PlaylistSongEntity
import com.melodia.app.data.model.Song
import com.melodia.app.player.MusicPlayer
import com.melodia.app.player.PlayerState
import com.melodia.app.player.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortMode { TITLE, ARTIST, RECENT, LONGEST }

class MainViewModel(
    val player: MusicPlayer,
    private val repo: MusicRepository,
    private val lists: PlaylistRepository,
    val sleep: SleepTimer
) : ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _sort = MutableStateFlow(SortMode.TITLE)
    val sort: StateFlow<SortMode> = _sort
    val playerState: StateFlow<PlayerState> = player.state
    private val _currentId = MutableStateFlow<Long?>(null)
    val currentSong: StateFlow<Song?> = combine(_songs, _currentId) { list, id -> list.find { it.id == id } }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
                if (id > 0) _currentId.value = id
            }
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

    private fun emitPlay(list: List<Song>, idx: Int) {
        if (list.isEmpty()) return
        _currentId.value = list[idx.coerceIn(0, list.size - 1)].id
        player.play(list.map { it.id }, idx.coerceIn(0, list.size - 1))
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
}

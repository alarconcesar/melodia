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
import kotlinx.coroutines.async
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
    private val queueStore: com.melodia.app.player.QueueStore,
    /** Dice si la red activa es medida (datos móviles): con listas grandes se confirma. */
    private val esRedMedida: () -> Boolean = { false }
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
    private val _album = MutableStateFlow<AlbumIdentity?>(null)
    val album: StateFlow<String?> = _album.map { it?.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _artist = MutableStateFlow<String?>(null)
    val artistF: StateFlow<String?> = _artist

    val filtered: StateFlow<List<Song>> = combine(_songs, _query, _sort, _folder, _album, _artist) { args ->
        @Suppress("UNCHECKED_CAST") val list = args[0] as List<Song>; val q = args[1] as String
        val s = args[2] as SortMode; val f = args[3] as String?; val al = args[4] as AlbumIdentity?; val ar = args[5] as String?
        var out = list
        if (f != null) out = out.filter { try { java.io.File(it.dataPath).parent == f } catch (_: Exception) { false } }
        if (al != null) out = songsForAlbum(out, al)
        if (ar != null) out = songsForArtist(out, ar)
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

    /**
     * Lista de Inicio: biblioteca completa + búsqueda, **sin** los filtros de
     * Explorar (carpeta/álbum/artista). Antes elegir una carpeta en Explorar
     * dejaba a Inicio mostrando solo esa carpeta, sin decir por qué.
     */
    val homeSongs: StateFlow<List<Song>> = combine(_songs, _query, _sort) { list, q, s ->
        var out = list
        if (q.isNotBlank()) {
            out = out.filter {
                it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
            }
        }
        when (s) {
            SortMode.TITLE -> out.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> out.sortedBy { it.artist.lowercase() }
            SortMode.RECENT -> out.sortedByDescending { it.dateAdded }
            SortMode.LONGEST -> out.sortedByDescending { it.durationMs }
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
    /** Orden de marcado (más reciente primero), no alfabético de biblioteca. */
    private val _favOrder = MutableStateFlow<List<Long>>(emptyList())
    val favSongs: StateFlow<List<Song>> = combine(_songs, _favOrder) { list, order ->
        @Suppress("UNCHECKED_CAST")
        val byId = (list as List<Song>).associateBy { it.id }
        order.mapNotNull { byId[it] }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    /** Aviso de una línea para la UI (toasts desde eventos del player). */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice
    fun clearNotice() { _notice.value = null }

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
                // Barrido con la lista real en mano (nunca con set vacío). Si la
                // biblioteca se encogió de golpe (SD desmontada), es parcial y
                // no se toca nada: si no, borraría las fichas de la SD.
                if (list.size > maxLibSeen) maxLibSeen = list.size
                if (maxLibSeen < 50 || list.size * 2 >= maxLibSeen) {
                    try {
                        repo.sweepDownloadMeta(list.mapNotNull {
                            try { java.io.File(it.dataPath).name } catch (_: Exception) { null }
                        }.toSet())
                    } catch (_: Exception) {}
                }
                maybeRestoreQueue(list)
                enrichArtwork(list)
            }
        }
        viewModelScope.launch {
            lists.favorites().collect { f ->
                _favIds.value = f.map { it.songId }.toSet()
                _favOrder.value = f.map { it.songId }
            }
        }
        // URL de stream vencida o rechazada: re-resolver y continuar sin cortar
        player.onStreamError = { pos -> refreshStreamAudio(pos) }
        // Persistir la cola: la lista (JSON) solo cuando cambia su composición;
        // índice/shuffle/repeat como claves sueltas para no reescribir 5k temas
        // en cada cambio de pista. Un stream NUNCA se persiste: antes pisaba la
        // cola local guardada y la reanudación quedaba inútil tras escuchar online.
        viewModelScope.launch {
            _queue.map { q -> q.map { it.id } }.distinctUntilChanged().collect {
                val q = _queue.value
                // Fuera del Main: armar el JSON de 5k temas (~1 MB) trababa la UI.
                if (q.isNotEmpty() && q.none { it.id == YT_STREAM_ID }) {
                    withContext(kotlinx.coroutines.Dispatchers.Default) { queueStore.saveSongs(q) }
                }
            }
        }
        viewModelScope.launch {
            combine(_queue, player.state) { q, st -> Triple(q.firstOrNull { it.id == st.currentId }?.id ?: -1L, st.shuffle, st.repeatMode) }
                .distinctUntilChanged()
                .collect { (currentId, shuffle, repeat) ->
                    val q = _queue.value
                    if (q.isEmpty() || q.any { it.id == YT_STREAM_ID }) return@collect
                    val idx = q.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
                    queueStore.saveState(idx, player.state.value.positionMs, shuffle, repeat)
                }
        }
        // Posición cada 5 s (clave suelta: no reescribe toda la cola).
        // Con stream no se guarda: pisaba la reanudación del tema local.
        viewModelScope.launch {
            var last = 0L
            player.state.collect { st ->
                val now = System.currentTimeMillis()
                if (now - last > 5_000 && _queue.value.isNotEmpty()
                    && _queue.value.none { it.id == YT_STREAM_ID }) {
                    last = now
                    queueStore.savePosition(st.positionMs)
                }
            }
        }
        // Sincronizar UI con el item real de ExoPlayer (next/prev/fin de tema)
        // Estadísticas por ESCUCHA real: se mide cuánto sonó el tema anterior
        // y solo cuenta si pasó el umbral (>=30 s o la mitad en temas cortos).
        var escuchandoId = -2L
        var escuchandoDesde = 0L
        var ultimaPos = 0L
        viewModelScope.launch {
            player.state.map { Triple(it.currentId, it.isPlaying, it.positionMs) }.distinctUntilChanged().collect { (id, playing, pos) ->
                val ahora = System.currentTimeMillis()
                var registrar: Triple<Long, Long, Long>? = null // id, ms, dur
                if (id != escuchandoId) {
                    // Cambio de tema: lo que sonó el anterior cuenta.
                    if (escuchandoId != -2L) {
                        val ms = ahora - escuchandoDesde
                        val dur = songs.value.find { it.id == escuchandoId }?.durationMs ?: 0L
                        registrar = Triple(escuchandoId, ms, dur)
                    }
                    escuchandoId = id
                    escuchandoDesde = ahora
                } else if (id == escuchandoId && !playing) {
                    // En pausa el reloj no corre: se congela para no sumar
                    // los minutos parado al cambiar de tema.
                    escuchandoDesde = ahora
                } else if (id == escuchandoId && id > 0 && pos < ultimaPos - 2_000) {
                    // Repeat-one: la posición volvió atrás = una vuelta entera.
                    // Un salto manual (seek, prev, doble-toque) también rebobina:
                    // no cuenta como escucha.
                    if (ahora - player.ultimoSaltoManualMs > 3_000) {
                        val dur = songs.value.find { it.id == id }?.durationMs ?: 0L
                        registrar = Triple(id, ahora - escuchandoDesde, dur)
                    }
                    escuchandoDesde = ahora
                }
                ultimaPos = pos
                registrar?.let { (previo, ms, dur) ->
                    if (previo > 0) viewModelScope.launch { stats.recordHeard(previo, ms, dur) }
                }
                if (id > 0 || id == YT_STREAM_ID) _currentId.value = id
                // Sin cola local visible no hay nada que reanudar después.
                if (id == YT_STREAM_ID) queueStore.savePosition(0L)
            }
        }
        // Tema local irreproducible (borrado/movido): el player ya saltó al
        // siguiente; aquí solo se avisa con palabras normales.
        player.onStreamDead = {
            _ytError.value = "No se pudo recuperar el audio. Reintenta en un momento."
        }
        player.onUnplayable = { id ->
            val nombre = songs.value.find { it.id == id }?.title?.take(40) ?: "Ese audio"
            _notice.value = "“$nombre” ya no está en el dispositivo — se saltó"
        }
        viewModelScope.launch {
            try { _ytRecent.value = yt.getRecent() } catch (_: Exception) {}
            try { _ytHistory.value = yt.getHistory() } catch (_: Exception) {}
            // Pendientes invisibles de una muerte anterior: se borran una vez.
            try { yt.cleanupStalePending() } catch (_: Exception) {}
            // Fichas de descargas cuyos archivos ya no existen. OJO: aquí
            // _songs casi siempre está vacía (la biblioteca carga después);
            // barrer con set vacío borraría TODAS las fichas. Solo con carga.
            try {
                if (_loaded.value) repo.sweepDownloadMeta(songs.value.mapNotNull {
                    try { java.io.File(it.dataPath).name } catch (_: Exception) { null } }.toSet())
            } catch (_: Exception) {}
        }
        // Descargas viejas sin artista: se reparan desde la fila, el archivo o
        // iTunes (máx. 25 consultas de red por arranque; converge sola).
        viewModelScope.launch {
            try {
                val n = withContext(Dispatchers.IO) { repo.repairDownloadMeta(yt, artRepo) }
                if (n > 0) {
                    _dlHistory.value = yt.getDlHistory()
                    reload()
                }
            } catch (_: Exception) {}
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
            kotlinx.coroutines.coroutineScope {
            // Letra y carátula se buscan EN PARALELO y con tope de tiempo: antes
            // eran dos esperas de red en fila y la descarga "terminaba" segundos
            // antes de aparecer en el historial.
            val artistaR = async {
                try { yt.resolveArtist(track, artRepo) }
                catch (_: Exception) { yt.artistFor(track) }
            }
            val letra = async {
                try {
                    val l = withContext(Dispatchers.IO) {
                        kotlinx.coroutines.withTimeoutOrNull(2_500L) {
                            lyricsRepo.get(track.artist, track.title, "", track.durationSec, "dl:${track.id}")
                        }
                    }
                    l?.let { plainOrSynced(it) }
                } catch (_: Exception) { null }
            }
            val arte = async {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(4_000L) { mejorMiniatura(track) }
                } catch (_: Exception) { null }
            }
            val lyricText = letra.await()
            val art = arte.await()
            val artista = artistaR.await()
            // Además de incrustarla en el m4a, queda como carátula local del
            // tema: si el etiquetado falla, la biblioteca igual muestra la suya.
            // Con el artista resuelto (el crudo no matchea las lecturas).
            try { withContext(Dispatchers.IO) { artRepo.saveLocal(artista, track.title, art) } } catch (_: Exception) {}
            withContext(Dispatchers.IO) {
                yt.storeDownload(track, high, audio, part, lyricText, art, null, artista)
            }
            // Primero el historial (es lo que mira el usuario), después la biblioteca.
            try {
                yt.pushDlHistory(track.copy(artist = artista), high)
                _dlHistory.value = yt.getDlHistory()
            } catch (_: Exception) {}
            reload()
            // Precalienta la caché de letra con la clave del tema ya guardado
            viewModelScope.launch { delayWarmLyric(track) }
            }
        }
    }

    /**
     * Carátula de la descarga en la mejor resolución disponible.
     *
     * La miniatura que trae un resultado de playlist suele ser pequeña (120 px)
     * y al usarla como portada se ve pixelada. Se prueban maxres/sd y se queda
     * con el candidato de mayor área; si nada llega a ~300x300 se usa igual la
     * mejor encontrada.
     */
    private suspend fun mejorMiniatura(track: YtTrack): ByteArray? {
        val candidatas = buildList {
            if (track.id.isNotBlank()) {
                add("https://i.ytimg.com/vi/${track.id}/maxresdefault.jpg")
                add("https://i.ytimg.com/vi/${track.id}/sddefault.jpg")
            }
            track.thumbUrl?.let { add(it) }
        }.distinct()
        var mejor: ByteArray? = null
        var mejorArea = 0
        for (url in candidatas) {
            val bytes = try {
                withContext(Dispatchers.IO) { yt.fetchBytes(url) }
            } catch (_: Exception) { null } ?: continue
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            try {
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            } catch (_: Exception) { continue }
            // Decodificado fallido (HTML de error 200, bytes rotos): se salta.
            // Antes outWidth=-1 daba área 1 y el HTML quedaba elegido.
            if (opts.outWidth <= 0 || opts.outHeight <= 0) continue
            val area = opts.outWidth * opts.outHeight
            if (area > mejorArea) {
                mejor = bytes
                mejorArea = area
            }
        }
        return mejor
    }

    private var queueRestored = false

    /**
     * Al abrir la app deja la última cola preparada y en pausa (no arranca sola).
     * Los temas que ya no están en la biblioteca se descartan y el índice se
     * recoloca sobre el mismo tema actual.
     */
    private suspend fun maybeRestoreQueue(library: List<Song>) {
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
            // Sin duración conocida no se calienta: ganaría el primer homónimo
            // aunque sea otra variante (la duración -1 salta el chequeo).
            if (track.durationSec <= 0) return
            // track ya trae el artista resuelto (canal o iTunes) y se exige
            // duración parecida: dos homónimos ya no calientan la letra ajena.
            val s = songs.value.firstOrNull {
                it.title.equals(track.title, true) &&
                    (track.artist.isBlank() || it.artist.equals(track.artist, true)) &&
                    kotlin.math.abs(it.durationMs / 1000 - track.durationSec) <= 3
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

    val excludedFolders: StateFlow<Set<String>> = repo.excludedFolders

    fun setExcludedFolders(paths: Set<String>) {
        repo.setExcludedFolders(paths)
        reload()
    }

    fun toggleExcludedFolder(path: String) {
        val cur = repo.excludedFolders.value.toMutableSet()
        if (!cur.remove(path)) cur.add(path)
        setExcludedFolders(cur)
    }

    fun updateMetadata(
        s: Song, title: String, artist: String, album: String,
        onConsent: (android.content.IntentSender) -> Unit = {},
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val r = withContext(Dispatchers.IO) { repo.updateMetadata(s.id, title, artist, album) }) {
                is MusicRepository.MetaResult.Ok -> { reload(); onDone(true) }
                is MusicRepository.MetaResult.NeedConsent -> onConsent(r.sender)
                is MusicRepository.MetaResult.Fail -> onDone(false)
            }
        }
    }

    fun setQuery(q: String) { _query.value = q }

    // ── Búsqueda rápida de YouTube desde la barra de Inicio ──
    // Una sola página y sin tocar el paginador de la pestaña Online.
    private val _homeYt = MutableStateFlow<List<com.melodia.app.data.online.YtSearchItem>>(emptyList())
    val homeYt: StateFlow<List<com.melodia.app.data.online.YtSearchItem>> = _homeYt
    private val _homeYtLoading = MutableStateFlow(false)
    val homeYtLoading: StateFlow<Boolean> = _homeYtLoading
    /** Consulta cuyos resultados ya están en [homeYt]; distinta de la actual = buscando. */
    private val _homeYtQuery = MutableStateFlow("")
    val homeYtQuery: StateFlow<String> = _homeYtQuery
    private var homeYtJob: kotlinx.coroutines.Job? = null
    private var homeYtGen = 0

    fun searchHomeYt(q: String = _query.value) {
        val query = q.trim()
        homeYtJob?.cancel()
        if (query.length < 2) {
            _homeYt.value = emptyList()
            _homeYtLoading.value = false
            _homeYtQuery.value = ""
            return
        }
        val gen = ++homeYtGen
        homeYtJob = viewModelScope.launch {
            _homeYtLoading.value = true
            try {
                val encontrados = withContext(Dispatchers.IO) { yt.searchFirstPage(query) }
                // Si otra búsqueda empezó mientras tanto, estos resultados son viejos.
                if (gen != homeYtGen) return@launch
                _homeYt.value = encontrados
                _homeYtQuery.value = query
                // Hijo del job: se cancela con la búsqueda y no mantiene el
                // spinner encendido con resultados ya visibles.
                val primeros = encontrados
                    .filterIsInstance<com.melodia.app.data.online.YtSearchItem.Track>()
                    .take(3)
                if (primeros.isNotEmpty()) {
                    launch {
                        for (t in primeros) {
                            try {
                                withContext(Dispatchers.IO) { yt.prefetchAudio(t.track.url, !_dataSaver.value) }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (gen != homeYtGen) return@launch
                _homeYt.value = emptyList()
                _homeYtQuery.value = query
            } finally {
                // El finally del job cancelado apagaba el loading del nuevo.
                if (gen == homeYtGen) _homeYtLoading.value = false
            }
        }
    }

    fun clearHomeYt() {
        homeYtJob?.cancel()
        homeYtGen++
        _homeYt.value = emptyList()
        _homeYtLoading.value = false
        _homeYtQuery.value = ""
    }
    fun setSort(s: SortMode) { _sort.value = s }
    fun setFolder(p: String?) { _folder.value = p; _album.value = null; _artistF2(null) }
    fun setAlbum(a: String?, artist: String? = null) { _album.value = a?.let { AlbumIdentity(it, artist) }; _folder.value = null; _artistF2(null) }
    fun setArtist(a: String?) { _artistF2(a); _folder.value = null; _album.value = null }
    private fun _artistF2(a: String?) { _artist.value = a }
    fun clearFilters() { _folder.value = null; _album.value = null; _artist.value = null }
    fun openPl(pid: Long?) { _openPlaylist.value = pid }

    // ── Cola de reproducción visible ──
    private fun emitPlay(list: List<Song>, idx: Int) {
        if (list.isEmpty()) return
        val at = idx.coerceIn(0, list.size - 1)
        // Tocar el tema que ya suena pausa/reanuda SOLO dentro del mismo
        // contexto: desde otra lista se reproduce ese contexto en vez de pausar.
        if (_currentId.value == list[at].id && _queue.value.map { it.id } == list.map { it.id }) {
            player.toggle()
            return
        }
        _queue.value = list
        _currentId.value = list[at].id
        player.play(list.map { it.id }, at)
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
        // Solo se filtra la UI si el player lo aceptó (con restore pendiente el
        // exo aún es null y el tema reaparecía al aplicar el restore).
        if (!player.removeAt(idx)) return
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

    private var guardandoCola = false
    /** Guarda la cola actual como playlist nueva, en el mismo orden. */
    fun saveQueueAsPlaylist(name: String, onDone: (Int) -> Unit = {}) {
        if (guardandoCola) return
        val songs = _queue.value.filter { it.id != YT_STREAM_ID }.distinctBy { it.id }
        if (name.isBlank() || songs.isEmpty()) { onDone(0); return }
        guardandoCola = true
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    val pid = lists.createPlaylist(name)
                    lists.addAllToPlaylist(pid, songs)
                }
            } catch (_: Exception) { 0 }
            guardandoCola = false
            onDone(saved)
        }
    }

    /**
     * Reproduce por id dentro de [list]. Cada pantalla pasa SU lista visible:
     * antes Inicio usaba `filtered` (con filtros de Explorar) y sonaba otra cosa.
     */
    fun playSong(id: Long, list: List<Song>? = null) {
        val base = list ?: filtered.value.ifEmpty { songs.value }
        val idx = base.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        emitPlay(base, idx)
    }
    /** Reproduce por id desde toda la biblioteca (historial, estadísticas). */
    fun playById(id: Long) {
        val list = songs.value
        val idx = list.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        emitPlay(list, idx)
    }
    fun playShuffle(list: List<Song>? = null) {
        val base = list ?: filtered.value.ifEmpty { songs.value }
        if (base.isEmpty()) return
        emitPlay(base, (base.indices).random())
    }
    fun playListAt(list: List<Song>, idx: Int) = emitPlay(list, idx)
    fun shuffleList(list: List<Song>) {
        if (list.isNotEmpty()) emitPlay(list.shuffled(), 0)
    }
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
    /** El tap suena por songId: la lista filtrada (sin huérfanos) tiene
     * otros índices que la vista, así que el idx crudo sonaba al vecino. */
    fun playPlaylistEntities(e: List<PlaylistSongEntity>, songId: Long) {
        val byId = songs.value.associateBy { it.id }
        val list = e.mapNotNull { byId[it.songId] }
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == songId }.takeIf { it >= 0 } ?: 0
        emitPlay(list, idx)
    }
    fun shufflePlaylistEntities(e: List<PlaylistSongEntity>) {
        val byId = songs.value.associateBy { it.id }
        val list = e.mapNotNull { byId[it.songId] }.shuffled()
        if (list.isNotEmpty()) emitPlay(list, 0)
    }
    fun moveInPlaylist(pid: Long, sid: Long, up: Boolean) {
        viewModelScope.launch {
            try { lists.moveSong(pid, sid, up) }
            catch (_: Exception) { _notice.value = "No se pudo mover el tema" }
        }
    }

    // ── Tono y velocidad ──
    fun setPitch(p: Float) { player.setPitch(p) }
    fun setSpeed(s: Float) { player.setSpeed(s) }
    fun resetPitchSpeed() { player.resetPitchSpeed() }

    // ── A continuación / al final ──
    fun queueNext(id: Long, onDone: (Boolean) -> Unit = {}) {
        val s = songs.value.find { it.id == id } ?: run { onDone(false); return }
        // Sobre un stream no se encola: ninguna otra operación de cola acepta
        // colas mixtas y el estado quedaba sin salida.
        if (_queue.value.any { it.id == YT_STREAM_ID }) { onDone(false); return }
        if (!player.playNext(id)) { onDone(false); return }
        // Detrás del actual visible (igual que el player): el índice de
        // timeline con shuffle es otro orden y divergía.
        val curId = player.state.value.currentId
        val at = _queue.value.indexOfFirst { it.id == curId }.takeIf { it >= 0 }?.plus(1)
            ?: _queue.value.size
        val q = _queue.value.toMutableList()
        q.add(at.coerceIn(0, q.size), s)
        _queue.value = q
        onDone(true)
    }

    fun queueEnd(id: Long, onDone: (Boolean) -> Unit = {}) {
        val s = songs.value.find { it.id == id } ?: run { onDone(false); return }
        if (_queue.value.any { it.id == YT_STREAM_ID }) { onDone(false); return }
        if (!player.addToEnd(id)) { onDone(false); return }
        // Va al final del timeline real (indexOf falla con duplicados).
        val at = (player.queueIds().size - 1).takeIf { it >= 0 } ?: _queue.value.size
        val q = _queue.value.toMutableList()
        q.add(at.coerceIn(0, q.size), s)
        _queue.value = q
        onDone(true)
    }

    // ── Fundido entre canciones ──
    fun toggleCrossfade() { player.setCrossfade(!player.state.value.crossfade) }
    fun setCrossfadeSecs(s: Int) { player.setCrossfadeSecs(s) }

    fun toggleFav(id: Long) { viewModelScope.launch { lists.toggleFavFresh(id) } }
    fun createPlaylist(name: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val limpio = name.trim().take(60)
            if (limpio.isBlank()) { onDone(-1); return@launch }
            val id = lists.createPlaylist(limpio)
            onDone(id)
        }
    }
    // ── Selección múltiple (borrar varios temas) ──
    private val _selecting = MutableStateFlow(false)
    val selecting: StateFlow<Boolean> = _selecting
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    private var visibleSelectionIds: List<Long> = emptyList()
    private val _selectionScope = MutableStateFlow<List<Long>>(emptyList())
    val selectionScope: StateFlow<List<Long>> = _selectionScope

    fun setVisibleSelectionScope(ids: List<Long>) { visibleSelectionIds = ids }

    fun startSelection(id: Long? = null) {
        // Se conserva la consulta y se congela la colección REAL que inició la
        // selección. Limpiarla aquí ampliaba «Todo» a canciones no mostradas.
        if (!_selecting.value) _selectionScope.value = visibleSelectionIds.toList()
        _selecting.value = true
        if (id != null && id != YT_STREAM_ID) _selected.value = _selected.value + id
    }

    fun toggleSelected(id: Long) {
        if (id == YT_STREAM_ID) return
        val cur = _selected.value
        _selected.value = if (cur.contains(id)) cur - id else cur + id
    }

    fun selectAll(ids: List<Long>) {
        _selected.value = ids.filter { it != YT_STREAM_ID }.toSet()
    }

    fun clearSelection() {
        _selecting.value = false
        _selected.value = emptySet()
        _selectionScope.value = emptyList()
    }

    /** Quita las marcas pero sigue en modo selección (botón "Nada"). */
    fun clearSelectionIds() { _selected.value = emptySet() }

    /** Borra los temas marcados; si el sistema pide confirmación, la entrega. */
    fun deleteSelected(onNeedsConfirm: (android.content.IntentSender) -> Unit = {}) {
        val ids = _selected.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val sender = withContext(Dispatchers.IO) { repo.deleteSongs(ids) }
            if (sender != null) {
                onNeedsConfirm(sender)
            } else {
                clearSelection()
                reload()
            }
        }
    }

    fun deletePlaylist(id: Long) { viewModelScope.launch { lists.deletePlaylist(id); if (_openPlaylist.value == id) _openPlaylist.value = null } }
    fun renamePlaylist(id: Long, name: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val limpio = name.trim().take(60)
            if (limpio.isBlank()) { onDone(false); return@launch }
            val duplicada = playlists.value.any {
                it.id != id && it.name.trim().equals(limpio, true)
            }
            if (duplicada) { onDone(false); return@launch }
            try { lists.rename(id, limpio); onDone(true) }
            catch (_: Exception) { onDone(false) }
        }
    }
    /** Aviso de una línea hacia la UI (sin tocar el flujo de toasts del player). */
    fun avisar(mensaje: String) { _notice.value = mensaje }
    fun togglePinPlaylist(id: Long) { viewModelScope.launch { lists.togglePin(id) } }
    fun setPlaylistCover(id: Long, albumId: Long?) { viewModelScope.launch { lists.setCover(id, albumId) } }
    fun addToPlaylist(pid: Long, s: Song, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            if (s.id == YT_STREAM_ID) { onResult(false); return@launch }
            val ok = lists.addToPlaylist(pid, s)
            onResult(ok)
        }
    }
    fun removeFromPlaylist(pid: Long, sid: Long) {
        viewModelScope.launch {
            try { lists.removeFromPlaylist(pid, sid) }
            catch (_: Exception) { _notice.value = "No se pudo quitar el tema" }
        }
    }

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
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsSummary())

    fun clearStats() { viewModelScope.launch { stats.clear() } }

    // ── Respaldo y restauración (JSON en Descargas/Melodia) ──
    fun backup(ctx: android.content.Context, onDone: (String) -> Unit) {
        var avisado = false
        fun fin(m: String) { if (!avisado) { avisado = true; try { onDone(m) } catch (_: Exception) {} } }
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { lists.exportJson(songs.value) }
                // API <29: sin MediaStore.Downloads, archivo directo en Descargas.
                if (android.os.Build.VERSION.SDK_INT < 29) {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            val dir = java.io.File(
                                android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_DOWNLOADS
                                ), "Melodia"
                            )
                            dir.mkdirs()
                            java.io.File(dir, "melodia-respaldo.json").writeText(json)
                            true
                        } catch (_: Exception) { false }
                    }
                    fin(if (ok) "Respaldo guardado en Descargas/Melodia" else "No se pudo crear el respaldo")
                    return@launch
                }
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "melodia-respaldo.json")
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/Melodia")
                }
                val uri = ctx.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri == null) { fin("No se pudo crear el archivo"); return@launch }
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
                fin("Respaldo guardado en Descargas/Melodia")
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                fin("No se pudo crear el respaldo")
            } finally {
                // Si cancelaron a mitad, el diálogo no queda pegado en "Trabajando…".
                fin("Respaldo cancelado")
            }
        }
    }

    fun restore(ctx: android.content.Context, uri: android.net.Uri, onDone: (String) -> Unit) {
        var avisado = false
        fun fin(m: String) { if (!avisado) { avisado = true; try { onDone(m) } catch (_: Exception) {} } }
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                val root = org.json.JSONObject(text)
                val lib = songs.value
                val byPath = lib.associateBy { it.dataPath }
                val existentes = playlists.value.associateBy {
                    it.name.removeSuffix(" (respaldo)").trim().lowercase()
                }
                var pls = 0; var added = 0; var favs = 0
                val arr = root.optJSONArray("playlists") ?: org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    val o = try { arr.optJSONObject(i) } catch (_: Exception) { null } ?: continue
                    val name = o.optString("name", "Lista").ifBlank { "Lista" }
                    // Si ya existe (misma restauración importada dos veces),
                    // se completa en vez de duplicar la lista.
                    val clave = name.removeSuffix(" (respaldo)").trim().lowercase()
                    var pid: Long? = existentes[clave]?.id
                    // Revalida: si la lista se borró a mitad del restore, el add
                    // dejaría filas huérfanas. Se recrea en ese caso.
                    if (pid != null && playlists.value.none { it.id == pid }) pid = null
                    val sarr = o.optJSONArray("songs") ?: org.json.JSONArray()
                    for (j in 0 until sarr.length()) {
                        val e = try { sarr.optJSONObject(j) } catch (_: Exception) { null } ?: continue
                        val match = byPath[e.optString("path")] ?: lib.find {
                            it.title.equals(e.optString("title"), true) &&
                                it.artist.equals(e.optString("artist"), true)
                        } ?: continue
                        // Perezoso: sin matches no queda lista vacía huérfana.
                        if (pid == null) pid = lists.createPlaylist("$name (respaldo)").also { pls++ }
                        if (lists.addToPlaylist(pid, match)) added++
                    }
                }
                val vistos = mutableSetOf<Long>()
                val byKey = lib.associateBy { lists.favKey(it.artist, it.title) }
                val byId = lib.associateBy { it.id }
                fun emparejar(raw: Any?): com.melodia.app.data.model.Song? {
                    // Clave "artista|título" (estable entre equipos) o id
                    // numérico (respaldo viejo / artista corregido después).
                    return when (raw) {
                        is String -> byKey[raw]
                        is Number -> byId[raw.toLong()]
                        else -> null
                    }
                }
                val farr = root.optJSONArray("favs") ?: org.json.JSONArray()
                // Títulos con clave: ancla para validar ids numéricos (los ids
                // de MediaStore colisionan entre teléfonos distintos).
                val titulosClave = HashSet<String>()
                for (i in 0 until farr.length()) {
                    val match = emparejar(farr.opt(i)) ?: continue
                    titulosClave.add(byId[match.id]?.title?.lowercase().orEmpty())
                    if (vistos.add(match.id) && !_favIds.value.contains(match.id)) {
                        lists.toggleFav(match.id, false); favs++
                    }
                }
                val iarr = root.optJSONArray("favIds") ?: org.json.JSONArray()
                for (i in 0 until iarr.length()) {
                    val raw = iarr.opt(i)
                    val cand = emparejar(raw) ?: (byId[iarr.optLong(i, -1)]?.takeIf {
                        // Sin claves (respaldo viejo): se acepta como antes.
                        // Con claves: solo si el título coincide (mismo teléfono,
                        // artista corregido después). Si no, es de otro equipo.
                        titulosClave.isEmpty() || it.title.lowercase() in titulosClave
                    }) ?: continue
                    if (vistos.add(cand.id) && !_favIds.value.contains(cand.id)) {
                        lists.toggleFav(cand.id, false); favs++
                    }
                }
                fin("Restaurado: $pls listas, $added temas, $favs favoritos")
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                fin("Archivo inválido — no se pudo restaurar")
            } finally {
                fin("Restauración cancelada")
            }
        }
    }

    // ── Carátulas online para temas sin arte ──
    private var artJob: kotlinx.coroutines.Job? = null
    /** Cursor rotativo: cada pasada arranca donde terminó la anterior. */
    private var artCursor = 0
    /** Máximo de temas visto: si la carga cae muy por debajo, es parcial (SD fuera). */
    private var maxLibSeen = 0

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
                if (list.isEmpty()) return@withContext
                val ordered = if (artCursor in list.indices) list.drop(artCursor) + list.take(artCursor) else list
                try {
                    var actual = list.associateBy { it.id }

                    // 1) caché local, al instante
                    val cached = list.mapNotNull { s ->
                        if (!s.artUrl.isNullOrBlank()) return@mapNotNull null
                        val esDescarga = s.dataPath.contains("/Melodia/")
                        if (s.albumId > 0 && !esDescarga) return@mapNotNull null
                        artRepo.cached(s.artist, s.title)?.let { s.id to it }
                    }.toMap()
                    if (cached.isNotEmpty()) {
                        // update{}: lectura-modificación atómica. Con = había
                        // carreras con reload() que tiraban carátulas ya puestas.
                        _songs.update { cur -> cur.map { s -> cached[s.id]?.let { s.copy(artUrl = it) } ?: s } }
                        actual = _songs.value.associateBy { it.id }
                    }

                    // 2) carátula incrustada en el archivo (máx. 80 grupos de álbum)
                    val found = mutableMapOf<String, String>()
                    var emb = 0
                    for (s in ordered) {
                        if (emb >= 200) break
                        val cur = actual[s.id] ?: continue
                        if (!cur.artUrl.isNullOrBlank()) continue
                        // Descargas de Melodia: cada archivo lleva su propia
                        // carátula incrustada. Antes se saltaban por tener
                        // albumId, así que todas compartían la portada del álbum
                        // "<unknown>" (la de la primera canción).
                        val esDescarga = cur.dataPath.contains("/Melodia/")
                        if (!esDescarga && cur.albumId > 0) continue
                        val group = if (esDescarga) "s:${cur.id}" else albumKey(cur)
                        if (found.containsKey(group)) continue
                        val local = try { artRepo.embedded(cur) } catch (_: Exception) { null }
                        if (local != null) found[group] = local
                        emb++
                    }
                    if (found.isNotEmpty()) {
                        // La clave de búsqueda es la misma de guardado: para
                        // descargas es "s:id", no la del álbum (nunca coincidía
                        // y la carátula incrustada jamás aparecía).
                        _songs.update { cur ->
                            cur.map { s ->
                                val k = if (s.dataPath.contains("/Melodia/")) "s:${s.id}" else albumKey(s)
                                found[k]?.let { s.copy(artUrl = it) } ?: s
                            }
                        }
                        actual = _songs.value.associateBy { it.id }
                    }

                    // 3) red, solo lo que sigue sin arte (máx. 50 por carga)
                    var n = 0
                    for (s in ordered) {
                        if (n >= 50) break
                        val cur = actual[s.id] ?: continue
                        if (cur.albumId > 0 || !cur.artUrl.isNullOrBlank()) continue
                        if (cur.artist.isBlank() || cur.artist.equals("Desconocido", true) ||
                            cur.artist.equals("YouTube", true) || cur.artist == "<unknown>"
                        ) continue
                        // La caché local no necesita cortesía de red: se aplica
                        // directo y sin el delay de 250 ms (eran 12.5 s por
                        // reload aunque todo estuviera cacheado).
                        artRepo.cached(cur.artist, cur.title)?.let { hit ->
                            _songs.update { cur2 -> cur2.map { x -> if (x.id == s.id) x.copy(artUrl = hit) else x } }
                            actual = actual + (s.id to s.copy(artUrl = hit))
                            n++
                            return@let
                        } ?: run {
                            val url = try { artRepo.fetch(cur.artist, cur.title) } catch (_: Exception) { null }
                            if (!url.isNullOrBlank()) {
                                _songs.update { cur2 -> cur2.map { x -> if (x.id == s.id) x.copy(artUrl = url) else x } }
                                actual = actual + (s.id to s.copy(artUrl = url))
                            }
                            n++
                            kotlinx.coroutines.delay(250)
                        }
                    }
                    // La próxima pasada arranca más adelante: toda la biblioteca
                    // converge en vez de pulir siempre los mismos 200.
                    artCursor = (artCursor + (emb + n).coerceAtLeast(1)) % list.size
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
    /** clave de la colección (playlist/álbum) que se está leyendo para descargar */
    private val _loadingCol = MutableStateFlow<String?>(null)
    val loadingCol: StateFlow<String?> = _loadingCol
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
    private var searchGen = 0
    fun searchYt(q: String = _ytQuery.value) {
        val query = q.trim()
        if (query.isBlank()) return
        if (query.length < 2) {
            _ytError.value = "Escribe al menos 2 letras para buscar"
            _ytErrorRetry.value = false
            return
        }
        searchJob?.cancel() // evita que una búsqueda anterior pise a la nueva
        val gen = ++searchGen
        searchJob = viewModelScope.launch {
            _ytLoading.value = true
            _ytError.value = null
            _ytErrorRetry.value = true
            try {
                val res = yt.search(query)
                // El fetch bloqueante no se interrumpe al cancelar: una búsqueda
                // vieja que termina después no debe pisar a la nueva.
                if (gen != searchGen) return@launch
                _ytResults.value = res
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
                    // Prefetch de los 3 primeros: al tocar el segundo o tercer
                    // resultado, el audio ya está resuelto y arranca al instante.
                    val primeros = _ytResults.value
                        .filterIsInstance<com.melodia.app.data.online.YtSearchItem.Track>()
                        .take(3)
                    if (primeros.isNotEmpty()) {
                        launch {
                            for (t in primeros) {
                                try {
                                    withContext(Dispatchers.IO) { yt.prefetchAudio(t.track.url, !_dataSaver.value) }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (gen != searchGen) return@launch
                _ytError.value = errMsg(t, "YouTube no respondió — reintenta en un momento")
                _ytErrorRetry.value = com.melodia.app.data.online.YtErrors.retryable(t)
            }
            if (gen == searchGen) _ytLoading.value = false
        }
    }

    fun loadMoreYt() {
        if (_ytMoreLoading.value || !_ytHasMore.value) return
        val gen = searchGen
        viewModelScope.launch {
            _ytMoreLoading.value = true
            try {
                val extra = withContext(Dispatchers.IO) { yt.more() }
                // Si hubo una búsqueda en medio, esta página es de la anterior.
                if (gen != searchGen) return@launch
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

    private var refreshJob: kotlinx.coroutines.Job? = null

    /**
     * YouTube invalidó la URL de audio (403/404/410): se re-resuelve con
     * [YtRepository.bestAudio] y se reemplaza en el player desde [positionMs],
     * sin reiniciar el tema ni tocar la cola.
     */
    private fun refreshStreamAudio(positionMs: Long) {
        val track = _currentTrack.value ?: return
        // Se cancela el anterior: el último error manda (el viejo podría
        // reemplazar el audio del tema nuevo al completarse).
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val audio = withContext(Dispatchers.IO) {
                    yt.bestAudio(track.url, !_dataSaver.value, force = true)
                }
                // ¿Sigue sonando el mismo stream? Si el usuario cambió de tema
                // (u otro refresh lo reemplazó), no se toca nada.
                if (_currentTrack.value?.url != track.url) return@launch
                try {
                    val cur = player.state.value.currentId
                    if (cur != YT_STREAM_ID) return@launch
                } catch (_: Exception) { return@launch }
                val durMs = if (track.durationSec > 3) track.durationSec * 1000L - 2000 else positionMs
                // autoPlay: el stream venía sonando; sin esto quedaba en pausa muda.
                player.swapStream(audio.url, minOf(positionMs, durMs).coerceAtLeast(0), autoPlay = true)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytError.value = "No se pudo recuperar el audio. Reintenta en un momento."
            }
        }
    }

    private var playYtJob: kotlinx.coroutines.Job? = null

    fun playYt(track: YtTrack) {
        // Cambiar de tema online mientras otro resuelve: se cancela el anterior en
        // vez de ignorar el toque (eso se sentía como que la app se trababa).
        // También se cancela un refresh de URL en vuelo (reemplazaría este tema).
        playYtJob?.cancel()
        refreshJob?.cancel()
        playYtJob = viewModelScope.launch {
            _busyYt.value = track.id
            _ytError.value = null
            // El tema elegido aparece de inmediato en el miniplayer, aunque el
            // audio tarde unos segundos en resolverse.
            val pendiente = Song(
                id = YT_STREAM_ID, title = track.title,
                artist = track.artist.ifBlank { "YouTube" },
                album = "Online", albumId = -1L, durationMs = track.durationSec * 1000,
                dataPath = "", dateAdded = 0L, artUrl = track.thumbUrl
            )
            _currentTrack.value = track
            _streamSong.value = pendiente
            // Cola previa a salvo: si el audio no resuelve, se restaura (si no,
            // la cola fantasma bloqueaba toda operación hasta sonar un local).
            val colaPrevia = _queue.value.filter { it.id != YT_STREAM_ID }
            val idPrevio = _currentId.value.takeIf { it != YT_STREAM_ID }
            _queue.value = listOf(pendiente)
            _currentId.value = YT_STREAM_ID
            // La letra que ya se resolvió con el pendiente se muda a la clave
            // del stream real (si no, se busca dos veces y parpadea Loading).
            val oldKey = "yt:${pendiente.dataPath.substringBefore("?").hashCode()}"
            try {
                // Solo-audio real: nunca se toca el video; con ahorro usa bitrate modesto
                val audio = withContext(Dispatchers.IO) { yt.bestAudio(track.url, !_dataSaver.value) }
                val stream = pendiente.copy(dataPath = audio.url)
                val newKey = "yt:${stream.dataPath.substringBefore("?").hashCode()}"
                lyricsMem[oldKey]?.let { lyricsMem[newKey] = it }
                _streamSong.value = stream
                _queue.value = listOf(stream)
                player.playStream(audio.url, track.title, track.artist, track.thumbUrl)
                try {
                    yt.pushHistory(track)
                    _ytHistory.value = yt.getHistory()
                } catch (_: Exception) {}
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytError.value = errMsg(t, "No se pudo reproducir — reintenta")
                // Se restaura lo que sonaba (la cola fantasma se descarta).
                _streamSong.value = null
                _queue.value = colaPrevia
                if (idPrevio != null) _currentId.value = idPrevio
            } finally {
                if (_busyYt.value == track.id) _busyYt.value = null
            }
        }
    }

    // ── Vista previa de una playlist/álbum de YouTube ──
    data class YtPreview(
        val title: String,
        val url: String,
        val loading: Boolean = false,
        val tracks: List<YtTrack> = emptyList(),
        val error: String? = null,
        val truncated: Boolean = false
    )

    private val _ytPreview = MutableStateFlow<YtPreview?>(null)
    val ytPreview: StateFlow<YtPreview?> = _ytPreview

    private var previewGen = 0
    fun openYtCollection(col: com.melodia.app.data.online.YtSearchItem.Collection) {
        _ytPreview.value = YtPreview(title = col.title, url = col.url, loading = true)
        val gen = ++previewGen
        viewModelScope.launch {
            try {
                val (title, tracks, recortada) = withContext(Dispatchers.IO) { yt.playlistTracks(col.url) }
                if (gen != previewGen) return@launch
                _ytPreview.value = YtPreview(
                    title = title.ifBlank { col.title }, url = col.url,
                    tracks = tracks, error = if (tracks.isEmpty()) "La lista no trae temas visibles" else null,
                    truncated = recortada
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (gen != previewGen) return@launch
                _ytPreview.value = YtPreview(
                    title = col.title, url = col.url,
                    error = errMsg(t, "No se pudo abrir la lista")
                )
            }
        }
    }

    fun closeYtPreview() { previewGen++; _ytPreview.value = null }

    /** Colección en espera de confirmación (datos móviles + lista grande). */
    data class PendingCol(val title: String, val tracks: List<YtTrack>, val high: Boolean, val truncated: Boolean)
    private val _pendingCol = MutableStateFlow<PendingCol?>(null)
    val pendingCol: StateFlow<PendingCol?> = _pendingCol
    fun confirmPendingCol() {
        val p = _pendingCol.value ?: return
        _pendingCol.value = null
        encolarColeccion(p.title, p.tracks, p.truncated)
    }
    fun clearPendingCol() { _pendingCol.value = null }

    /** true si conviene pedir confirmación (muchos temas + red medida). */
    private fun pideConfirmacion(n: Int): Boolean {
        if (n <= 25) return false
        return try { esRedMedida() } catch (_: Exception) { false }
    }

    private fun encolarColeccion(title: String, tracks: List<YtTrack>, truncated: Boolean) {
        dl.enqueueAll(tracks, _ytHighQuality.value)
        val recorte = if (truncated) " (lista recortada a ${tracks.size})" else ""
        _ytMsg.value = "«$title»: ${tracks.size} temas a la cola de descargas$recorte"
    }

    fun downloadYtPreview() {
        val p = _ytPreview.value ?: return
        if (p.tracks.isEmpty()) return
        _ytPreview.value = null
        if (pideConfirmacion(p.tracks.size)) {
            _pendingCol.value = PendingCol(p.title, p.tracks, _ytHighQuality.value, p.truncated)
            return
        }
        encolarColeccion(p.title, p.tracks, p.truncated)
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
    // LRU: el acierto reencola (antes el warm expulsaba la letra en pantalla).
    private val lyricsMem = object : LinkedHashMap<String, LyricsUi>(48, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsUi>?): Boolean = size > 40
    }
    private var lyricsJob: kotlinx.coroutines.Job? = null

    fun loadLyrics(s: Song, force: Boolean = false) {
        // En streams la URL lleva firma con expiración: se quita el query para
        // que la clave sea estable entre sesiones y reintentos.
        val key = if (s.id == YT_STREAM_ID) "yt:${s.dataPath.substringBefore("?").hashCode()}" else "id:${s.id}"
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
                lyricsMem[key] = ui // removeEldestEntry poda a 40 solo.
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
            .filter { t -> dlJobs.value.none { it.track.id == t.id && it.high == _ytHighQuality.value } }
        if (list.isEmpty()) return
        if (pideConfirmacion(list.size)) {
            _pendingCol.value = PendingCol("Resultados de la búsqueda", list, _ytHighQuality.value, false)
            return
        }
        dl.enqueueAll(list, _ytHighQuality.value)
    }

    // ── Pantalla de descargas (independiente, no modal) ──
    private val _showDownloads = MutableStateFlow(false)
    val showDownloads: StateFlow<Boolean> = _showDownloads
    fun openDownloads() { _showDownloads.value = true }
    fun closeDownloads() { _showDownloads.value = false }

    /** Reanuda todo lo pausado o en error. */
    fun resumeAllDl() {
        dl.jobs.value.forEach { j ->
            if (j.status == com.melodia.app.data.online.DlStatus.PAUSED ||
                j.status == com.melodia.app.data.online.DlStatus.ERROR
            ) dl.resume(j.track.id)
        }
    }

    /** Pausa todo lo que está en cola o bajando. */
    fun pauseAllDl() {
        dl.jobs.value.forEach { j ->
            if (j.status == com.melodia.app.data.online.DlStatus.ACTIVE ||
                j.status == com.melodia.app.data.online.DlStatus.QUEUED
            ) dl.pause(j.track.id)
        }
    }

    /** Descarga entera una playlist o álbum que salió en la búsqueda. */
    fun downloadYtCollection(col: com.melodia.app.data.online.YtSearchItem.Collection) {
        // Lock propio: antes usaba _busyYt y una resolución de audio en curso
        // bloqueaba la descarga de colecciones (y viceversa) sin decir nada.
        if (_loadingCol.value != null) {
            _ytMsg.value = "Ya se está leyendo otra lista, espera un momento"
            return
        }
        viewModelScope.launch {
            _loadingCol.value = col.key
            _ytMsg.value = null
            try {
                val (title, tracks, recortada) = withContext(Dispatchers.IO) { yt.playlistTracks(col.url) }
                if (tracks.isEmpty()) {
                    _ytMsg.value = "«${col.title}» no tiene temas visibles"
                } else if (pideConfirmacion(tracks.size)) {
                    _pendingCol.value = PendingCol(title.ifBlank { col.title }, tracks, _ytHighQuality.value, recortada)
                    _ytMsg.value = null
                } else {
                    encolarColeccion(title.ifBlank { col.title }, tracks, recortada)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytMsg.value = errMsg(t, "No se pudo leer la lista")
            } finally {
                _loadingCol.value = null
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


    /** Re-escucha un tema del historial (la url firmada puede haber vencido). */
    fun playHistoryRecord(r: YtRepository.DlRecord) {
        if (r.url.isBlank()) {
            _notice.value = "Esa descarga ya no tiene enlace: busca el tema en Online"
            return
        }
        playYt(YtTrack(r.id.ifBlank { "hist-${r.at}" }, r.title, r.artist, -1, null, r.url))
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
            _ytImport.value = "Pega el enlace de una lista (debe incluir “list=” en la URL)"
            return
        }
        if (_ytImport.value == "…") return
        val igen = ++importGen
        viewModelScope.launch {
            _ytImport.value = "…"
            try {
                val (title, tracks, recortada) = withContext(Dispatchers.IO) { yt.playlistTracks(clean) }
                if (igen != importGen) return@launch
                if (tracks.isEmpty()) {
                    _ytImport.value = "La lista no trae temas visibles"
                    return@launch
                }
                if (pideConfirmacion(tracks.size)) {
                    _pendingCol.value = PendingCol(title, tracks, _ytHighQuality.value, recortada)
                    _ytImport.value = null
                    return@launch
                }
                dl.enqueueAll(tracks, _ytHighQuality.value)
                _ytImport.value = "OK|${tracks.size}|$title" + if (recortada) "|CORTADA" else ""
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ytImport.value = errMsg(t, "No se pudo leer la lista")
            }
        }
    }

    private var importGen = 0
    fun clearYtImport() { importGen++; _ytImport.value = null }
}

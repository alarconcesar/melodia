# Melodia vs el estado del arte open source — octubre 2026

Análisis de 6 reproductores Android open source para detectar qué mejorar en Melodia
(4.4.1, 39 archivos Kotlin, 8.078 líneas). Todo lo que se afirma de otro proyecto
está citado con `archivo:línea` del código clonado el 26-oct-2026.

> Método: `git clone --depth 1` de los repos, lectura estática de código y Gradle.
> No se compilaron ni ejecutaron las apps ajenas. Las cifras de stack salen de sus
> `build.gradle`/`libs.versions.toml`. Nuestro código se auditó con la misma vara.

## Repos analizados

| Proyecto | Stars | Stack | Licencia | Por qué importa |
|---|---|---|---|---|
| **OxygenCobalt/Auxio** | 4.390 | Kotlin 2.4, Media3 1.11, Hilt, Room 2.8, Coil3 | GPL-3.0 | Referencia de ingeniería para reproductor **local**: indexado incremental, cola persistida, ReplayGain |
| **FoedusProgramme/Gramophone** | 2.363 | Kotlin 2.3, Media3 1.10 (desde fuente), Views + islas Compose | GPL-3.0 | `MediaLibraryService`, reanudación, letras LRC/TTML, notificación decorada |
| **zyrouge/symphony** | 1.721 | Kotlin, Compose 1.7, MediaPlayer propio | AGPL-3.0 | UX ligera: gestos, orden/rejilla persistidos, cola serializada |
| **OuterTune/OuterTune** | 5.442 | Kotlin 2.4, Compose 1.11, Media3 1.10, Hilt, Room 2.8 | GPL-3.0 | **El más parecido**: local + YouTube Music, cola persistente, InnerTube |
| **maxrave-dev/SimpMusic** | 12.384 | KMP, Compose MP, Media3 1.11, Koin, WorkManager, Ktor | GPL-3.0 | YouTube Music, caché de URLs en Room, Android Auto, descargas Media3 |
| **RetroMusicPlayer** | 5.369 | Kotlin, Media3 1.6, Koin, Vistas | GPL-3.0 | Catálogo de features que un usuario local espera (M3U, tag editor, 7 widgets, Auto) |

Metrolist (13.8k) e InnerTune (6.1k) quedaron fuera del deep dive: mismo nicho que
OuterTune/SimpMusic y sin técnicas distintas en el muestreo.

## TL;DR

1. **No pedimos foco de audio ni wake mode.** ExoPlayer se construye sin
   `setAudioAttributes(..., handleAudioFocus=true)` y sin `setWakeMode`
   (`MelodiaPlaybackService.kt:59-67`). Todos los pares lo hacen explícito. Es el
   bug de estabilidad más barato de arreglar.
2. **La cola no sobrevive al proceso.** `queueIds` y `_queue` viven en memoria
   (`MusicPlayer.kt:41`, `MainViewModel.kt:225`). Los 6 proyectos persisten cola,
   índice, posición y shuffle; varios implementan `onPlaybackResumption`.
3. **Las descargas mueren con la app.** Coroutines + `Semaphore(3)` + `.part` en
   `cacheDir` (`DownloadManager.kt:41-43`). Auxio no descarga; SimpMusic y
   OuterTune usan `DownloadService` de Media3, que sobrevive y reintenta.
4. **La URL de audio caduca y no lo sabemos.** Caché en RAM con TTL fijo de 5 h
   (`YtRepository.kt:121-148`) vs. `expiresInSeconds` real del servidor y
   re-resolución ante 403 (SimpMusic, OuterTune). Explica fallos intermitentes al
   reanudar tras una pausa larga.
5. **Biblioteca: sin caché ni incrementalidad.** `loadAllSongs()` consulta
   MediaStore entero en cada `reload()` (`MusicRepository.kt:47-106`). Auxio cachea
   por `(uri, modifiedMs)` en Room y reindexa con `ContentObserver`.
6. **La UI concentra demasiado.** `MelodiaScreen.kt` 2.539 líneas y
   `MainViewModel` ~740: Gramophone y OuterTune separan pantalla/ViewModel y cola.
   Consecuencia: cada feature nueva toca un archivo gigante.
7. **Features ausentes que sí son estándar:** Android Auto, multi-selección,
   M3U, tag editor completo, orden/rejilla persistidos, cola persistente.

## Comparativa por área

| Área | Melodia hoy | Estado del arte (evidencia) |
|---|---|---|
| Servicio | `MediaSessionService` + notificación **manual** (`MelodiaPlaybackService.kt:132-215`) | `MediaLibraryService` (Gramophone `:164`, OuterTune `MusicService.kt:263`, SimpMusic `SimpleMediaService.kt:34`) y notificación que **decora** el provider de Media3 (`MeiZuLyricsMediaNotificationProvider.kt:69-74`) |
| Foco de audio | Sin `setAudioAttributes`/`WAKE_MODE` | `setAudioAttributes(..., handleAudioFocus=true)` + `WAKE_MODE_LOCAL`/`NETWORK` (Auxio `ExoPlaybackStateHolder.kt:681-688`; Gramophone `:430`; OuterTune `MusicService.kt:231-244`) |
| Cola | En memoria, reconstruida al reproducir | Tablas `queue` + `queue_song_map` con `shuffledIndex` (OuterTune `QueueEntity.kt`, `QueueSongMap.kt`), heap + mapping (Auxio `PersistenceDatabase.kt`), JSON con poda (Symphony `RadioQueue.kt:147`) |
| Reanudación | No implementada | `onPlaybackResumption` en Gramophone `:1339`, OuterTune `MediaLibrarySessionCallback.kt:98`, Auxio lo opone a propósito (ver “no copiar”) |
| Biblioteca | Query completa a MediaStore por arranque | Caché Room por `uri+modifiedMs` + lotes de 500 + 8 hilos + `ContentObserver` (Auxio `DBCache.kt:43-50`, `MediaStore.kt:142-148`) |
| Carátulas | iTunes por artista+título (`ArtworkRepository.kt`) | Thumbnail de MediaStore según tamaño + fallback embebido + proveedor `content://` (Gramophone `CoilArtPipeline.kt:74-93`; Auxio `CoverProvider.kt`) |
| Online | NewPipeExtractor 0.26.5 (último release) | NewPipe + InnerTube para metadata; validación HEAD; `ResolvingDataSource`; URL cacheada en Room con expiración real (SimpMusic `StreamRepositoryImpl.kt:233-293`) |
| Descargas | Coroutines propias, `.part` en caché | `DownloadService` de Media3 + `PlatformScheduler` (SimpMusic `MusicDownloadService.kt:26-38`), descarga con `range` y reanudación (OuterTune `DownloadUtil.kt:130-141`) |
| Auto | No | `CarAppService` + `MediaLibraryService` (SimpMusic `SimpMusicCarAppService.kt`), árbol MediaBrowser (Auxio, Retro `AutoMusicProvider.kt`) |
| Tests | 22 JVM/Robolectric | Auxio 112 en módulo de tags y 0 en app; OuterTune 0 en app; SimpMusic 17. **Estamos por encima de la media en la capa de app** |
| Perf | Sin baseline profile ni benchmark | Auxio/Gramophone sin baseline tampoco; ninguno lo tiene como prioridad |

## Qué copiar de cada uno

### Auxio (local, ingeniería)
- **`Progression` interpolado**: la posición se calcula con `elapsedRealtime()` desde
  un snapshot y la UI hace tick a 100 ms sin tocar el player
  (`PlaybackStateHolder.kt:298-348`, `PlaybackViewModel.kt:183-197`). Nuestro
  `playerState.progress` se recalcula consultando ExoPlayer cada 500 ms
  (`MusicPlayer.kt:85`): seekbar menos fluida y más JNI.
- **Cola derivada del player** con `MediaItem.localConfiguration.tag = Song`
  (`ExoPlaybackStateHolder.kt:138-151`) en vez de mantener `queueIds` en paralelo:
  una sola fuente de verdad.
- **Identidad estable** por hash de metadatos (`Music.kt:60-96`), no `_ID` de
  MediaStore (que se recicla al reescanear). Es prerrequisito de una cola persistente.
- **Caché de indexado** por `(uri, modifiedMs)` + escritura en lotes + invalidación
  por revisión (`DBCache.kt:125-129`, `MusicRepository.kt:394-425`).
- **Broadcast `ACTION_OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION`**
  (`ExoPlaybackStateHolder.kt:468-488`): permite que EQs externos y ReplayGain se
  enganchen a nuestra sesión.
- **ReplayGain como `AudioProcessor`** en ambos renderers (`ReplayGainAudioProcessor.kt:150-200`).

### Gramophone (servicio y letras)
- **`MediaLibraryService` con looper propio** (`GramophonePlaybackService.kt:164,201-202`)
  y `setWakeMode(C.WAKE_MODE_LOCAL)` (`:430`).
- **Cola atómica por comando custom + `Binder`/`BundleListRetriever`**
  (`GramophoneExtensions.kt:316-329`, `MediaItemList.kt:26-33`): evita
  `TransactionTooLargeException` con listas de miles de temas.
- **`onPlaybackResumption`** que restaura cola, índice y posición (`:1339-1400`).
- **Notificación que decora el provider** en vez de reimplementarlo
  (`MeiZuLyricsMediaNotificationProvider.kt:69-74`): menos código y arreglos gratis
  con cada update de Media3.
- **Scheduler de letras sin polling**: un `postDelayed` a la próxima marca
  (`GramophonePlaybackService.kt:1802-1825`). Nuestra letra sincronizada avanza por
  tick de 500 ms.
- **Predictive back** real en el player (`PlayerBottomSheet.kt:221-271`).

### OuterTune (el espejo más cercano)
- **Una entidad para local y online** (`isLocal`, `localPath`, `dateDownload` —
  `SongEntity.kt:28-40`): cola mixta sin ramas especiales. Hoy Melodia usa el
  pseudo-id `YT_STREAM_ID` y reemplaza la cola entera para un stream
  (`MainViewModel.kt:622-640`).
- **Cola + mapping de shuffle persistidos** y guardado debounced 5 s con `Mutex`
  (`QueueBoard.kt:846-875`), `@Transaction` para no dejar huérfanos
  (`DatabaseDao.kt:330-365`).
- **`ResolvingDataSource`** que decide archivo/stream/caché en un solo punto
  (`MusicService.kt:658-766`).
- **Caché de URL con expiración del servidor** y re-resolución por chunk
  (`MusicService.kt:700-704`, `765`).
- **Fallback de clientes con validación previa** (`YTPlayerUtils.kt:125-186`).
- **Espera de red + tope de 3 errores consecutivos** antes de saltar
  (`MusicService.kt:875-902`): evita quemar la cola cuando YouTube falla.

### SimpMusic (online, descargas, Auto)
- **Metadata por InnerTube + URL por extractor, emparejando por `itag`**
  (`YouTube.kt:1543-1668`): lo mejor de ambos mundos; nosotros solo NewPipe.
- **Tabla `new_format` con `expiredTime`** (`NewFormatEntity.kt:10-30`) y
  `updateFormat()` perezoso (`StreamRepositoryImpl.kt:47-84`).
- **`DownloadService` de Media3 con `PlatformScheduler` y reintentos**
  (`MusicDownloadService.kt:26-38`): reemplazo natural de nuestro `DownloadManager`.
- **Android Auto mínimo**: `MediaLibraryService` + `CarAppService` + manifiesto
  (`androidApp/src/main/AndroidManifest.xml:278-288`) + un callback que sirve el
  árbol (`SimpleMediaSessionCallback.kt:231-364`). ~300 líneas para NowPlaying,
  biblioteca, búsqueda y cola.
- **`orderByAudioTrack` sobre `xtags`** (`AudioTrackOrder.kt:24-52`): evita el audio
  en el idioma equivocado en videos con pistas dobladas.

### RetroMusicPlayer / symphony (features y UX)
- **Multi-selección accionable** (`AbsMultiSelectAdapter.kt:51-113`; en Compose el
  patrón de `Queue.kt:58-100`).
- **M3U** (`M3UWriter.kt:23-70`) y backup ZIP (`BackupHelper.kt:70-91`).
- **Tag editor multi-formato** con jaudiotagger (`TagWriter.kt:53-130`): Melodia
  solo escribe etiquetas MP4 (`Mp4Tagger.kt`).
- **Orden con reverse + rejilla persistidos por pantalla** (`MediaSortBar.kt:41`,
  `Settings.kt:126-205`).
- **Ficha de archivo** con bitrate/frecuencia/formato (`SongDetailDialog.kt:45`).
- **Cola serializada con poda de ids muertos** (`RadioQueue.kt:147-188`).

## Lo que NO conviene copiar

- **La sesión legacy de Auxio** (`MediaBrowserServiceCompat` + `MediaSessionCompat` +
  notificación manual): nosotros ya estamos en Media3; volver atrás es deuda.
- **`Musikr` + taglib C++ + fork vendorizado de Media3** (Auxio): miles de líneas y
  builds nativos para un beneficio marginal.
- **God objects**: `QueueBoard.kt` 942 líneas y `MusicService.kt` 1.161 en OuterTune,
  con `runBlocking` en el hilo del `DataSource`. No repetir el patrón al persistir
  nuestra cola.
- **`offscreenPageLimit = 99999`** (Gramophone) y su paginación falsa
  (`drop/take` sobre la lista completa): no escala.
- **20 descargas paralelas** (SimpMusic): throttling de googlevideo; 3-4 es sensato.
- **26 módulos + KMP + Koin** para una app de 8k líneas: sobra.
- **Telemetría con `isSendDefaultPii = true`** (SimpMusic): no en una app con
  historial de escucha.
- **~20 estilos de Now Playing** (Retro): deuda de mantenimiento y rompe el DS propio.

## Nuestra brecha, con evidencia local

| # | Brecha | Evidencia | Impacto usuario |
|---|---|---|---|
| 1 | Sin foco de audio / wake mode / MediaButtonReceiver | `MelodiaPlaybackService.kt:59-67`; manifest sin receiver | Llamadas y auriculares no pausan; playback puede cortarse en Doze |
| 2 | Cola no persistente | `MusicPlayer.kt:41`, `MainViewModel.kt:225` | “Maté la app y perdí la cola” |
| 3 | URL de stream con TTL fijo | `YtRepository.kt:121-148` | “No suena tras una pausa larga” (403) |
| 4 | Descargas en proceso + cacheDir | `DownloadManager.kt:41-43` | Descarga cancelada si Android mata la app; `.part` evictable |
| 5 | Biblioteca sin caché | `MusicRepository.kt:47-106` | Arranque lento y re-escaneo completo en cada cambio |
| 6 | Notificación manual | `MelodiaPlaybackService.kt:132-215` | Más código y desfase con el sistema/Auto |
| 7 | Sin Android Auto | manifest | Público que no puede usar la app en el coche |
| 8 | Pantallas monolíticas | `MelodiaScreen.kt` 2.539 líneas | Cada feature nueva es más lenta y riesgosa |
| 9 | Sin multi-selección / M3U / tag completo | UI actual | Fricción para gestionar biblioteca grande |
| 10 | Stack atrasado | Media3 1.2.1 vs 1.11.1; Compose 2024.01 vs 2026.09 | Faltan APIs (resumption, DownloadService, preload) y fixes |

## Plan propuesto

> **Estado (26-oct-2026):** Fase 0 implementada y publicada en 4.5 (foco de audio,
> wake mode, MediaButtonReceiver, progreso interpolado, TTL real de la URL y
> re-resolución ante 403/404/410). Fases 1-3 pendientes de decisión.

### Fase 0 — Quick wins (1-2 días, bajo riesgo)
| Cambio | Esfuerzo | Referencia |
|---|---|---|
| `setAudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MUSIC, handleAudioFocus=true)` + `setWakeMode` | S | Auxio `:681-688`, Gramophone `:430` |
| `<receiver MediaButtonReceiver>` en manifest | S | Media3 docs |
| Seekbar con progreso interpolado (tick 100 ms, sin consultar ExoPlayer) | S | Auxio `PlaybackStateHolder.kt:298-348` |
| Validar URL de audio (HEAD) y re-resolver una vez ante 403/410 | S | SimpMusic `Ytmusic.kt:1358` |
| Broadcast de sesión `AudioEffect` | S | Auxio `:468-488` |

### Fase 1 — Continuidad (3-5 días)
| Cambio | Esfuerzo | Referencia |
|---|---|---|
| Persistir cola/índice/posición/repeat con debounce 5 s + poda de ids | M | OuterTune `QueueBoard.kt:846-875`, Symphony `RadioQueue.kt:147` |
| `onPlaybackResumption` en el servicio | M | Gramophone `:1339`, OuterTune `:98` |
| Descargas con `DownloadService` de Media3 (o WorkManager) y `.part` fuera de caché | M | SimpMusic `MusicDownloadService.kt:26-38` |
| Caché de URL con `expiresInSeconds` en Room | S-M | SimpMusic `StreamRepositoryImpl.kt:233-293` |

### Fase 2 — Producto (1-2 semanas)
| Cambio | Esfuerzo | Referencia |
|---|---|---|
| Multi-selección con acciones (playlist, cola, favorito, borrar) | S-M | Retro `AbsMultiSelectAdapter.kt:51-113` |
| Orden con reverse + rejilla persistidos | S | Symphony `MediaSortBar.kt:41` |
| Import/export M3U + backup | M | Retro `M3UWriter.kt:23-70` |
| Android Auto (`MediaLibraryService` + `CarAppService`) | M | SimpMusic `SimpMusicCarAppService.kt` |
| Tag editor multi-formato | L | Retro `TagWriter.kt:53-130` |

### Fase 3 — Plataforma (track aparte)
| Cambio | Esfuerzo | Nota |
|---|---|---|
| Media3 1.2.1 → 1.11.x | L | Habilita `MediaLibraryService`, `DownloadService`, preload, resumption |
| Kotlin 1.9 → 2.x + Compose BOM 2026 + Coil 3 | L | Compose compiler pasa a plugin de Kotlin; migración planificada |
| Baseline profile + macrobenchmark de arranque | M | Nadie en el nicho lo tiene: ventaja medible |
| Partir `MelodiaScreen`/`MainViewModel` por pantalla | L | Hacerlo junto con Fase 2, no antes |

## Decisiones pendientes (para César)

1. ¿Atacamos primero **estabilidad** (Fase 0+1) o **features** (Fase 2)?
2. ¿Android Auto entra en el próximo release o se deja para después de persistir la cola?
3. ¿La cola debe restaurarse **en pausa** al abrir la app o solo cuando el sistema lo pida?
4. ¿Migración de stack (Fase 3) ahora o cuando el producto esté cerrado?

---

### Anexo: reproducir el análisis
```bash
cd ~/research/melodia-oss
# clones shallow del 26-oct-2026
# Auxio, Gramophone, symphony, OuterTune, SimpMusic, RetroMusicPlayer
```
Notas crudas y metadatos: `~/research/melodia-oss/NOTAS-PROPIAS.md`.

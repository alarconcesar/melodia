# Arquitectura — Melodia

## Capas

```
UI (Compose + Melodia DS)
 └─ MainViewModel (androidx ViewModel, retenido en rotación)
     ├─ MusicPlayer ── ExoPlayer (+ MediaSession, + AudioEffect vía EQ)
     ├─ MusicRepository ── MediaStore (Flow)
     ├─ PlaylistRepository ── Room
     └─ SleepTimer
DI manual: AppContainer (Application)
```

## Decisiones clave

1. **Sin Hilt (manual DI).** Tres builds "compila pero abre en negro"
   se curaron quitando Hilt + Room-generado + `MediaSessionService` del
   manifest + tema AppCompat. Reintroducir Hilt solo incremental y probado
   en dispositivo. Orden de des stripping que funcionó:
   Splash → Service → Room → Hilt.
2. **Room con KSP, 1 DAO por archivo.** KSP + varios DAOs en un archivo
   emite `error.NonExistentClass` y rompe en runtime aunque compile.
3. **Tema AppCompat obligatorio** (`Theme.AppCompat.DayNight.NoActionBar`
   + dep `appcompat`) o `setContent` muere en silencio.
4. **ExoPlayer y MediaSession siempre en try/catch**, incluido `updateState()`.
5. **Columnas MediaStore con `getColumnIndex()`** (no `OrThrow`): en algunos
   equipos faltan columnas y la query revienta. Se filtra `< 30s` (tonos).
   Se normaliza `"<unknown>"` → "Desconocido" y título vacío → nombre de archivo.
6. **Sincronía player→UI.** ExoPlayer avanza solo (next/prev/fin); el VM
   observa `player.state.currentId` (`distinctUntilChanged`) y actualiza el
   tema actual. Sin esto, suena uno y se muestra otro.
7. **Reload de biblioteca.** La query corre al crear el VM; si el permiso
   llega después, `reload()` re-dispara el Flow. `onResume` recarga si la
   lista está vacía y hay permiso.

## Base de datos (v1)

`melodia.db`: `playlists(id, name, createdAt)`,
`playlist_songs(playlistId, songId, position, snapshot…)`,
`favorites(songId, addedAt)`. `fallbackToDestructiveMigration()`.

## Notificaciones / fondo

Canal `melodia_playback` (API platform, no compat — el compat crasheaba
en algunos API 26–33). MediaSession viva sin `MediaSessionService`
(a propósito: registrar el servicio tumbaba el arranque).
Pendiente: servicio foreground persistente.

## Back

`BackHandler` por capas en `MelodiaScreen`:
player → detalle de playlist → filtro → salir.
Los `AlertDialog` ya consumen back solos.

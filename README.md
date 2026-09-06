# 🎵 Melodia — Reproductor local premium para Android

Reproductor de música local con diseño minimalista moderno (**Melodia DS**),
construido 100% con Kotlin + Jetpack Compose + Media3. Sin tracking, sin anuncios,
sin Hilt: arranque estable con DI manual.

![minSdk 26](https://img.shields.io/badge/minSdk-26-green)
![targetSdk 34](https://img.shields.io/badge/targetSdk-34-green)
![Kotlin 1.9.22](https://img.shields.io/badge/Kotlin-1.9.22-blue)
![Compose BOM 2024.01.00](https://img.shields.io/badge/Compose_BOM-2024.01.00-blue)

## ✨ Funciones

| Área | Detalle |
|---|---|
| 🎵 Biblioteca | Canciones desde MediaStore, buscar global, ordenar (título/artista/recientes/duración) |
| 📁 Explorar | Carpetas reales del dispositivo, álbumes, artistas |
| 📃 Playlists | Crear / renombrar / eliminar, añadir temas (tap largo), detalle con play |
| ❤️ Favoritos | Corazón único por fila, tab dedicada |
| 🎚️ Ecualizador | 5 bandas + Bass Boost + presets del sistema, en vivo y persistido |
| ⏱️ Sleep timer | 5–60 min con chip visible |
| 📄 Ficha del tema | Long-press: duración, álbum, carpeta, fecha y **ruta completa** + acciones |
| ↩️ Back por capas | Player → detalle → filtro → salir |
| 🌗 Tema | Claro / oscuro / sistema, dark-first verdoso |
| ▶️ Player | Play/pause/next/prev, shuffle, repeat (off/all/one), seek con preview, MediaSession |

## 🧱 Stack (versiones fijadas y probadas)

- Gradle 8.5 · AGP 8.2.2 · Kotlin 1.9.22 · Compose compiler 1.5.8
- Compose BOM 2024.01.00 · Material 3
- Media3 ExoPlayer + Session 1.2.1 · Room 2.6.1 (KSP) · Coil 2.5.0
- DI manual (`AppContainer`) — decisión consciente: Hilt + Room + KSP daban
  pantallas negras en arranque (ver `docs/ARCHITECTURE.md`).

## 📁 Estructura

```
app/src/main/
├── AndroidManifest.xml
├── java/com/melodia/app/
│   ├── MelodiaApp.kt            # Application + canal de notificación
│   ├── MainActivity.kt          # Permisos, VM retenida, setContent
│   ├── AppContainer.kt          # DI manual
│   ├── MainViewModel.kt         # Estado UI + sincronía con ExoPlayer
│   ├── data/
│   │   ├── MusicRepository.kt   # MediaStore (normaliza <unknown>)
│   │   ├── PlaylistRepository.kt
│   │   ├── LibraryHelpers.kt    # carpetas / álbumes / artistas
│   │   ├── model/Song.kt
│   │   └── local/               # Room: 1 DAO por archivo
│   │       ├── MelodiaDatabase.kt
│   │       ├── PlaylistEntity.kt / PlaylistSongEntity.kt / FavoriteEntity.kt
│   │       └── PlaylistDao.kt / PlaylistSongDao.kt / FavoriteDao.kt
│   ├── player/
│   │   ├── MusicPlayer.kt       # ExoPlayer + MediaSession (try/catch)
│   │   ├── EqualizerManager.kt  # AudioEffect: EQ + BassBoost
│   │   └── SleepTimer.kt
│   └── ui/
│       ├── theme/Theme.kt       # Melodia DS: tokens
│       ├── ds/Components.kt     # TrackRow, Hero, QuickCard, PillNav, MiniPlayer…
│       └── screens/MelodiaScreen.kt
├── res/values/ (themes AppCompat — obligatorio, ver docs)
docs/
├── DESIGN_SYSTEM.md
└── ARCHITECTURE.md
```

## 🚀 Compilar

```bash
export ANDROID_HOME=$HOME/Android   # SDK con platforms;android-34 + build-tools;34.0.0
cd melodia
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ El tema **debe** heredar de `Theme.AppCompat.*` y existir la dependencia
> `androidx.appcompat:appcompat` o la app abre en negro (Compose + Material3
> lo requieren). No usar `android:Theme.*`.

## 🗺️ Roadmap

- [ ] Servicio foreground + notificación persistente
- [ ] Cola visible (ver / reordenar / limpiar)
- [ ] Carátulas reales (MediaStore art) + widget + Android Auto
- [ ] Letras embebidas/LRC · velocidad/pitch · estadísticas

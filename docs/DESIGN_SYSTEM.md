# Melodia DS v1 — Sistema de diseño

Familiar-Spotify, minimalista moderno, **dark-first**.
Inspiración: fintech oscuro (fondo negro verdoso, cards redondeadas,
verde neón, pill-nav flotante, titulares grandes + secundarios atenuados).

> Vive en código: `ui/theme/Theme.kt` (tokens) + `ui/ds/Components.kt`.

## 🎨 Color

| Token | Dark | Light |
|---|---|---|
| `background` | `#0A0B0A` | `#F4F6F4` |
| `surface` (cards) | `#131413` | `#FFFFFF` |
| `surfaceVariant` (elevada) | `#1A1B1A` | `#E8ECE8` |
| `onBackground` / `onSurface` | `#F2F4F2` | `#10140F` |
| `onSurfaceVariant` (secundario) | `#A0A5A0` | `#5F6B62` |
| `outline` (bordes cristal) | `#262826` | `#D8DED8` |
| `primary` (acento) | `#2EE56B` | `#0E9F52` |
| `onPrimary` | `#06110A` | `#FFFFFF` |
| Favorito | verde acento (un solo ♥ por fila) | igual |

Fondo: degradado vertical `surface → surface → background` + glow verde
superior (11%) + glow inferior (5%). Sutil, `rgba ~6-11%`, sin gradientes full-color.

## ✏️ Tipografía (`Ds`)

| Estilo | Spec |
|---|---|
| `Display` | 28sp SemiBold, títulos hero / Now Playing |
| `Title` | 17sp SemiBold, secciones y cards |
| `Body` | 15sp Normal, títulos de tema |
| `Sub` | 12sp Normal, artista · duración |
| `Micro` | 11sp Medium **UPPERCASE** + tracking, etiquetas ("AHORA SUENA") |

## ⭕ Radios

`RHero 24dp` · `RCard 16dp` · `RRow 12dp` · chip `10dp` · pill = `CircleShape`.

## 🧩 Componentes

- **TrackRow** — art 56dp, título + artista·duración, **un corazón**,
  tap = play, long-press = ficha, **deslizar** → derecha encola al final /
  izquierda alterna favorito (franja de acción que se dibuja solo en lo
  descubierto, icono con alfa en fase de dibujo: cero recomposición al arrastrar).
  Barras de EQ animadas si suena.
- **HeroCard** — "Seguir escuchando", FAB verde 72dp, borde hairline.
- **QuickCard** — grid 2 col (Favoritos / Aleatorio / Recientes / Artistas).
- **MelodiaPillNav** — barra flotante translúcida (78%) + borde + sombra,
  5 tabs fijos (layout estable, sin saltos). **Sin punto indicador**: el
  estado seleccionado ya lo dicen el fondo pill + acento + escala del icono.
- **MiniPlayerDs** — flotante, **apilado con la nav en una misma columna**
  (`Column` anclada abajo): miniplayer, 10dp de aire y la pill-nav. Antes su
  margen se calculaba midiendo la nav con `onGloballyPositioned` y en algunos
  estados se solapaban; apilados el orden lo garantiza el layout. Al aparecer,
  el mini crece con `expandVertically` en vez de saltar encima de la nav.
- **NowPlaying** (rediseño 5.3, ajuste 5.6) — la carátula ocupa **todo el aire
  disponible**: vive en un `BoxWithConstraints` con `weight(1f)` y mide
  `min(ancho, alto)` del área (clamp 170–430dp). Antes era `alto×0.34` y dejaba
  un hueco muerto entre el título y los controles. Plana y sin sombra,
  con fondo teñido a 160 px + degradado. Columna: barra superior (cerrar ·
  `N de M` en Micro · ⋮), carátula, título en `Display` a 2 líneas, artista ·
  álbum, espaciador flexible, barra de avance, transporte y **fila de acciones**
  (Favorito · Letra · Cola) como píldoras planas. El corazón ya no vive en la
  barra superior y "Ver letra" dejó de ser un link suelto: la letra y la cola
  están a un toque, no en el menú.
- **SeekBar** — pista propia de 4dp con relleno del acento y perilla de 12dp;
  toque para saltar y arrastre horizontal. Reemplaza el `Slider` de Material
  (grueso, con sombra y sin relación con el resto del DS).
- **Cabecera** — compacta (logo 34dp + saludo + Melodia) y el buscador global
  **solo aparece en Inicio/Explorar** (o si hay una búsqueda activa); el resto
  de secciones gana ~60dp de contenido.
- **Estado vacío de Inicio** — icono en círculo + botón *Conceder permiso de
  audio* (lanzador real) + *Abrir ajustes de la app*: nunca solo texto.
- **DsBackground** — degradado + glows (fondo por defecto; el player usa el
  tinte de la carátula).
- **SettingsScreen** — pantalla propia (entra desde la derecha, con `BackHandler`):
  Apariencia (tema claro/oscuro/sistema + acento en línea), Reproducción
  (fundido, EQ, temporizador), Biblioteca (carpetas, estadísticas, respaldo),
  Datos (calidad y ahorro) e Información (versión y privacidad). Reemplaza el
  menú de 8 opciones; el encabezado queda con un solo engranaje.
- **HomeSkeleton** — silueta con pulso suave mientras se lee MediaStore
  (`vm.loaded`), para no mostrar "biblioteca vacía" durante el arranque.
- **Carrusel "Tus álbumes"** en Inicio — tarjetas de 138dp ordenadas por
  cantidad de temas; tocar aplica el filtro de álbum en Explorar.
- **QueueSheet** — diálogo ancho (94% de la pantalla) con conteo y duración
  total, chips de aleatorio/repetir, filas con ↑ ↓ ✕ (cajas de 30dp, no
  `IconButton`, cuyo mínimo de 48dp ahoga el título), "Vaciar" (deja sonando el
  tema actual) y "Guardar lista" (playlist nueva en el mismo orden).
- **TrackInfoSheet / EqSheet** — diálogos con scroll, colores explícitos.

## 📏 Reglas duras (aprende de nuestros bugs)

1. **Todo `Text()` lleva `color` explícito.** La raíz es `Box + Column`
   (no `Surface`), así que el color por defecto es negro → invisible en dark.
2. **Botones en posición fija**: nada se monta/desmonta para mostrarse;
   la pill-nav siempre tiene los 4 items.
3. **Glass = translucidez + borde hairline + sombra.** No hay blur real
   en Compose estándar; sobre fondo plano la alfa sola no se nota.
4. **Listas con `contentPadding` inferior 200dp**: las barras flotantes
   nunca tapan contenido.
5. Overlays full-screen consumen toques (`clickable` sin indicación)
   para que nada atraviese a la lista.

## ✂️ Pase minimal (4.7)

El objetivo fue sacar cromo, no apagar la app. Regla: **si un elemento no da
información ni acción, sobra**.

- Fondo plano: sin glows ni degradados ambientales. El aire y la tipografía
  hacen el trabajo; el vidrio se reserva a la nav y al miniplayer.
- Nav: sin borde ni contadores (los números bajo cada pestaña eran ruido).
  Icono 20dp, etiqueta 12sp, selección por color + escala 1.06 con `tween`
  (sin rebote).
- Filas: el corazón solo aparece si el tema es favorito. Deslizar a la
  izquierda sigue marcando y el long-press abre la ficha.
- Home: fuera las 4 QuickCards (duplicaban pestañas). Quedan hero, escuchado
  recientemente, álbumes y temas.
- HeroCard: sin borde ni "toca para escuchar"; título, subtítulo y FAB.
- Buscador: campo plano (contenedor `surface`, sin caja); borde solo al enfocar.
- Chips: sin borde, radio 10dp, texto 12sp.
- Empty state: un icono, una acción primaria y un link discreto.

## 🌐 Online (rediseño 4.8)

La pestaña tenía tres filas de chips antes de llegar al buscador. Ahora:

- **Una jerarquía**: título + ⋮, buscador, resultados. Nada más.
- El menú ⋮ concentra Descargas, Importar playlist, Ahorro de datos y Calidad
  alta (con check). Los ajustes no compiten con la búsqueda.
- Buscador único: lupa a la izquierda (también busca), limpiar a la derecha,
  spinner embebido mientras carga. Sin botón cuadrado aparte.
- Resultados como **filas planas** (arte 48dp, título 1 línea, artista ·
  duración, descarga fantasma) en vez de tarjetas dentro de tarjetas.
- La búsqueda es **mixta** (`YtSearchItem`): YouTube devuelve videos y
  playlists/álbumes en la misma página. Las colecciones se pintan con la
  miniatura, la insignia de lista y "N temas · Playlist o álbum"; tocarlas
  descarga la colección entera (con aviso efímero bajo el buscador). Se filtran
  los `list=RD…` porque son mixes automáticos, no listas reales.
- Cabecera de resultados: conteo + "Descargar todo" como texto.
- Importar playlist y calidad pasan a diálogos; el estado vacío es una línea.

## Rendimiento (pase 4.9)

Reglas nacidas de medir el jank, no de la intuición:

- **Nada de blur en vivo sobre contenido que se mueve.** Haze re-blurea en cada
  frame de scroll; el fondo del reproductor a `blur(60dp)` era un RenderEffect de
  pantalla completa. Se usan velos translúcidos (`surface` al 95-97%) y el
  gradiente sobre una miniatura de 160 px.
- **Observa lo mínimo en la raíz.** `MelodiaScreen` solo escucha `isPlaying`
  (`map{}.distinctUntilChanged()`); el `PlayerState` completo se observa dentro
  del miniplayer y del reproductor. Antes el tick de 500 ms recomponía las 5
  pestañas y la lista entera.
- **Sin animaciones infinitas decorativas.** El marquee del título animaba en
  cada frame con el reproductor abierto; ahora es una línea con elipsis.
- **Trabajo pesado fuera del hilo principal.** Filtrado y agrupado de la
  biblioteca (`filtered`, `folders`, `albums`, `artists`, `favSongs`) van con
  `flowOn(Dispatchers.Default)`.
- **Datos estables para Compose.** `Song` es `@Immutable`; las filas que no
  cambian se saltan al recomponer.
- **Latido adaptativo.** `MusicPlayer` emite solo si algo cambió y baja a 1.5 s
  en pausa (antes 500 ms siempre).
- **Medición en la app.** Ajustes → Rendimiento muestra fps, % de frames lentos
  y el peor frame de los últimos 15 s (`perf/FrameMonitor`, Choreographer).

## Pulido 5.2

- **Un solo estado vacío** (`EmptyState` en `Components.kt`): icono en círculo al
  10%, título, subtítulo y acción opcional. Favs, Listas, Inicio y Online lo
  comparten; antes había tres tamaños y tres grises distintos.
- **Inicio**: ritmo vertical uniforme (10dp por sección) y "Sin resultados"
  ahora explica qué pasó y sugiere cambiar la palabra, en vez de una línea gris.
- **Online**: fuera el título "Online" (repetía la pestaña ya seleccionada); el
  menú ⋮ vive junto al buscador, en la misma fila. Se gana una línea de aire.
- **Ajustes**: tarjetas sin borde hairline (el resto de la app ya no usa bordes).
- **Reproductor**: carátula sin sombra; el fondo teñido ya la separa.
- **Inicio** dejó de observar `playerState` sin usarlo (recomponía la pantalla
  dos veces por segundo mientras sonaba algo).

## Búsqueda global con YouTube (5.9)

La barra de la cabecera (Inicio/Explorar) filtra la biblioteca al instante y, si
el texto tiene 2+ caracteres y dejas de teclear 650 ms, busca también en YouTube:

- Resultados locales primero (`Coincidencias`, `Tus temas`).
- Debajo, sección **"En YouTube"** con hasta 8 resultados mixtos (temas y
  playlists/álbumes) y acción "Ver en Online", que lleva la consulta a esa
  pestaña y dispara la búsqueda completa con paginación.
- La búsqueda rápida (`searchFirstPage`) **no toca el paginador** de Online:
  comprobado con sonda real (Online conserva su página 2 después).

## Ficha del tema y selección múltiple (6.2)

- **Info del tema** se rehizo: cabecera con arte + título + artista · álbum,
  metadatos cortos (duración, carpeta, agregado), ruta en gris, dos botones
  primarios de 44dp ("Reproducir", "A lista"), fila de acciones con icono y
  etiqueta (Favorito · Editar · Compartir · Elegir) y "Siguiente"/"Al final".
  Los botones van con alto fijo: antes `weight` dentro del scroll los estiraba
  a media pantalla y partía el texto ("Siguien / te").
- **Selección múltiple**: `LocalSelection` (CompositionLocal) comparte
  `selecting/selected/toggle` con cualquier lista; `TrackRow` muestra check en
  vez de corazón y el toque marca en vez de reproducir. La barra vive en la
  raíz ("N seleccionados · Todo · Borrar · Cancelar") y reemplaza al buscador.
- Borrado: `MusicRepository.deleteSongs` intenta borrar directo; si el archivo
  es de otra app (Android 10+) devuelve el `IntentSender` de
  `MediaStore.createDeleteRequest` y la raíz lo lanza con
  `StartIntentSenderForResult`.

## Movimiento (7.7)

Regla general: se anima **transform, alpha o color**, nunca tamaño/padding, y el
tween vive en el composable más pequeño posible. Nada de animar contenedores
grandes durante el scroll.

| Dónde | Qué hace | Tiempo |
| --- | --- | --- |
| `TrackRow` | se hunde a 0.985 al presionar (spring 1200/0.8) | ~120 ms |
| `TrackRow` · corazón | late al marcar: 0.7 → 1 con spring rebotado | ~350 ms |
| `TrackRow` · título activo | el color pasa a acento con `animateColorAsState` | 200 ms |
| `HeroCard` | se hunde a 0.985 al tocar; el botón de play a 0.9 | ~120 ms |
| Pestañas | alpha 0→1 y 8 dp de subida sobre el contenedor ya compuesto | 170/210 ms |
| `SeekBar` | la perilla escala 1.55× mientras arrastras | spring |
| `NpAction` | fondo y tono animados al activarse; press a 0.95 | 180 ms |
| Descargas | progreso suavizado con tween lineal 420 ms | — |
| Carátulas | `crossfade(180)` de Coil al cargar | 180 ms |

Cuidados:

- `pressClick` usa 0.96 por defecto (un botón que se hunde 10 % se siente roto).
- El cambio de pestaña anima **una sola** composición (no `AnimatedContent`, que
  compone las dos pantallas).
- La primera composición no se anima: en el arranque manda el esqueleto.

## Cola (7.8)

Dejó de ser un diálogo flotante al 94 %: ahora es **pantalla completa**, con la
misma jerarquía que el reproductor.

- Barra superior: chevron para cerrar, "Cola" y el conteo con duración total.
- **Sonando ahora**: tarjeta con carátula, título, artista y barras de
  ecualizador. Es el único lugar donde aparece el tema actual.
- Chips de aleatorio y repetición.
- **A continuación · N**: lista que arranca en el tema siguiente al que suena
  (lo anterior ya está en la tarjeta; repetirlo confundía). Cada fila lleva
  barra de acento, carátula de 40 dp y tres acciones (subir, bajar, quitar).
- Si no queda nada después: "No hay más temas después de este."
- Pie fijo con "Vaciar" y "Guardar lista", separado por una línea de 1 dp.

## Auditoría 7.9 (bugs corregidos)

- "Todo" selecciona lo visible en la pestaña actual (antes `filtered` global:
  en Inicio borraba lo de la carpeta de Explorar).
- Denegar el borrado ya no limpia la selección ni finge éxito (solo RESULT_OK
  limpia; si se deniega se recarga para mostrar los archivos que siguen ahí).
- Sin migración destructiva ciega: backup del sqlite + MIGRATION_1_3
  idempotente + validación de esquema (si la v1 no calza, se parte de cero
  con respaldo, no con crash).
- Cola del player: `play()` y el restore filtran ids sin item (remapean el
  índice) y respetan el shuffle; `removeAt` devuelve Boolean y la UI solo
  filtra si el player aceptó; `pending` es cola (no se pierde "A continuación"
  en frío); next/prev/seek aplican el restore pendiente.
- SleepTimer con guarda `minutes<=0`, expiración solo si no se canceló y
  fundido en job propio que se aborta al cancelar (antes pausaba igual).
- Tema local muerto: el player salta al siguiente (máx. 5) y la UI avisa.
- Descargas: extracción con cupo (3), ACTIVE persiste (reabrir retoma),
  `cancel()` no borra el .part de un DONE en copia, DONE queda "Guardada" 8 s,
  403 re-resuelve el enlace una vez (`DlUrlExpiredException`), la cancelación
  no se reintenta, fileName lleva el id (sin colisiones), historial dedupica,
  poda de dlart/lyrics y barrido de prefs huérfanas.
- Stats por escucha real (>=30 s o mitad) con sentencia atómica; favoritos del
  respaldo con clave estable artista|título.
- UI: `rememberSaveable` en flags de pantallas/diálogos (rotación),
  `playerState.map` recordado, SeekBar en un solo gesto + semántica,
  letra que no roba scroll, cola que acompaña al tema actual, keys estables en
  Descargas/PlaylistDetail, `FrameMonitor.stop()`, widget con fallback a la app.

## Auditoría 7.11 (segunda pasada + regresiones de la 7.10)

- El barrido de fichas corría con la biblioteca vacía y las borraba todas
  (raíz del "todo Desconocido" en algunos equipos): solo barre con carga real
  y con la lista en mano tras cada recarga.
- El temporizador nunca pausaba: `job = null` antes de `onExpire()` dejaba
  `isActive()` en false al final del fundido. Ahora `consumeExpire()` de un
  solo uso distingue "se cumplió" de "se canceló".
- Tope de skips real: el salto por error ya no resetea el contador (flag
  `expectErrorTransition`); `play()`/`playStream()` lo resetean.
- Notificación por `musicPlayer.toggle/next/prev` (no Exo directo): ya no deja
  el fade del timer vivo. Reanudación del sistema con posición clampada.
- 403 con tope (2 reintentos frescos por descarga, contador por llamada, no
  global); portada generada una vez y reusada en ambas fichas.
- `fetchArtist` sin pista exige igualdad exacta de título (no contención).
- Timeouts de red cortos donde hay `withTimeoutOrNull` (el hilo no se queda
  colgado 8-20 s tras abandonar).
- `searchGen`/`pager` bajo el mismo lock; `favKey` con espacios colapsados y
  respaldo con ids + claves; `recordHeard` exige 30 s sin duración.
- Cola: posiciones por timeline real (`currentTimelineIndex`/`restoreIndex`),
  nunca `indexOf` (diverge con duplicados).
- Stats: la pausa congela el reloj, repeat-one cuenta por vueltas (tope =
  duración), los streams también cuentan.
- UI: follow de cola sobre la lista recortada (vuelve arriba), SeekBar no
  roba el scroll vertical, `FrameMonitor.stop()` a prueba de frames en vuelo,
  "Todo" usa la pestaña donde empezó la selección.

136 tests verdes.

## Auditoría 7.12 (tercera pasada)

- `swapStream` verifica que el stream siga siendo el actual (id + timeline):
  un refresh lento ya no reemplaza el audio del tema nuevo. El refresh se
  cancela al cambiar de tema.
- `pendingOps` con tope (50): sin servicio ya no se acumulan lambdas.
- Apagar el crossfade a mitad de rampa restaura el volumen (el tema nuevo ya
  no queda mudo); saltar sin crossfade corta restos de fundido.
- `timelineIndexOf` con duplicados: sin shuffle el índice visible es el real;
  con shuffle se busca la k-ésima ocurrencia.
- EQ con >5 bandas: las no manejadas quedan en flat (no heredan presets).
- Shuffle/repeat externos (Auto/Bluetooth) actualizan el estado al instante.
- Trozo corto aceptado como éxito: se verifica `pos > to` o se reintenta.
- Reintento fresco parte de cero (otra variante = otro tamaño).
- `bestAudio(estándar)` prefiere m4a sin recortar la lista (adiós webm sin tags).
- Persistencia de cola hasta 100; re-enqueue en ventana DONE.
- `fetchBytes` con cota + content-type; `mejorMiniatura` descarta no-imágenes.
- 429 → ReCaptcha (mensaje honesto); ENOSPC con error propio (sin reintentos).
- `playlistTracks` avisa si recorta; confirmación con datos móviles para
  listas grandes (estimado en MB, diálogo en Online).
- `streamToFileResume` intenta Range antes de reescribir.
- Reparación cubre DATA cuando RELATIVE_PATH es nulo (SD en algunos OEM).
- `recordHeard` exige 30 s sin duración; `favKey` con espacios colapsados y
  respaldo con ids + claves; renombrar con trim, tope y sin duplicados.
- `enrichArtwork` sin delays en caché + arranque rotativo (toda la biblioteca
  converge); stats en Default; `QueueStore.load` en IO; posición no pisada por
  streams; letras de stream con clave estable + migración pendiente→resuelto.
- `delayWarmLyric` con artista resuelto + filtro de duración; `dataPath` con
  fallback; Favoritos en orden de añadido; restore que reutiliza listas;
  operaciones de playlist con feedback; setters con debounce.
- UI: back cierra hojas antes que el player; colección y play con locks
  separados; tap en letra sin duración avisa; compartir avisa si falla;
  ficha con salto a artista/álbum; letra que arranca arriba; diálogo de
  calidad que sobrevive a la rotación.

137 tests verdes.

## Auditoría 7.13 (cuarta pasada)

- `swapStream` verificado por id + timeline; refresh cancelable al cambiar.
- `pendingOps` con tope; next/prev/seek/shuffle/repeat se encolan en frío.
- Salto a prueba de cancelación (una sola vez) y sin dip en bordes.
- Rebobinar/prev no cuentan como vuelta de repeat-one (ventana de 3 s).
- `remove/move/clear` aplican el restore pendiente; pitch/speed persisten.
- Stream muerto tras reintentos avisa (antes silencio).
- Headless (Auto/BT) no se corta al abrir la app.
- Trozo corto verificado; reintento fresco desde cero; webm sin tags fuera;
  persistencia hasta 100; fetch acotado; 429→ReCaptcha; ENOSPC propio;
  aviso de recorte; confirmación con datos (preview, colección, import,
  todo); Range al reanudar; repair cubre SD; sweep con guard de SD.
- Stats: umbral 30 s sin duración; favs con ids+claves; rename saneado;
  arte sin delays + cursor rotativo; stats en Default; load() en IO;
  posición a salvo; letras estables + migración; warm con duración;
  dataPath con fallback; favs en orden; restore sin duplicados; debounce.
- UI: diálogo de datos en raíz (cubre Home), truncado visible, roles de
  accesibilidad, teclado que se esconde, renameId fantasma cerrado, letra
  con insets, ficha con "Encolar" + salto a artista/álbum.

138 tests verdes.

## Auditoría 7.14 (quinta pasada)

- Cálculos/toggles en frío se encolan (next/prev/seek/shuffle/repeat).
- Salto a prueba de cancelación + sin dip en bordes; rewind no cuenta stats.
- Restore en borrado/reorden; pitch/speed persistidos; stream muerto avisa.
- Headless (Auto/BT) no se corta al abrir.
- ACTIVE con cupo en mano; resume trunca variante cambiada; LIKE exacto;
  una sola ficha; LRU real; VEVO también en colecciones.
- clear/close limpian su generación; guard en guardado de cola; toggleFav
  serializado con lectura fresca; guardado de cola en lote.
- Repair con cursor persistido; saveLocal con artista resuelto; gen en
  previa y ver-más; estimado único; VPN con red subyacente.
- Letras: warm con la misma clave; Plain con scroll por tema; aviso sin
  duración; historial re-escuchable con url.
- Ficha con Encolar + artista/álbum saneados; PillNav/MiniPlayer/HeroCard/
  NpAction/InfoAction/SectionHeader/EmptyState con rol y etiqueta;
  % exacto en texto; diálogo de datos solo en raíz; sheet con recorte.
- Teclado escondido al ocultar la barra; renameId fantasma cerrado;
  restore $name validado; sweep con guard de SD.

138 tests verdes.

## Auditoría 7.15 (sexta pasada)

- Pausa sincrónica antes de cancelar (reanudar-todo tras pausar-todo funciona).
- Encolar cambia de calidad en el sitio (antes se ignoraba en silencio).
- Descargar-todo filtra por id+calidad.
- Refresh recorta a variante más corta; zombi START_STICKY se detiene.
- Notificación sin spam en BUFFERING; carátula local en fondo con caché.
- Widget con barra viva cada 2 s + escalado cacheado.
- crossfadeSecs con clamp; restore valida ids contra títulos;
  playlistTracks devuelve parcial; fileName por bytes;
  Online exige 2 letras; Synced con key; buscador solo donde filtra;
  renameId se cierra; % exacto en texto.

138 tests verdes.

## Auditoría 7.16 (séptima pasada)

- playYt fallido restaura la cola previa (sin cola fantasma).
- Export en una sola lectura; restore revalida el pid.
- Selección limpia el filtro; warm sin duración no calienta; letras LRU.
- Reintentar reconstruye el contenedor (fin del bucle).
- Bind muerto se reintenta cada 10 s; error-salto solo si puede avanzar.
- Re-encolar DONE en ventana de 8 s funciona; cambio de calidad sin huérfanos.
- Fila online con estado "resolviendo"; historial cierra Descargas.
- Preset duplicado avisa y sanea separadores.

139 tests verdes.

## Auditoría 7.17 (octava pasada)

- Factory usa el constructor completo (confirmación de datos móviles viva).
- Salto fantasma al buscar/apagar/pausar: eliminado (salto en campo).
- Restore sobrevive a biblioteca tardía; salto manual solo si salta.
- Pitch/speed se reaplican al reconectar; presets guardan bandas.
- Playlist sin posiciones duplicadas; restore tolerante y perezoso.
- Cola en IO; letra sin duración exige título; sweep en una escritura.
- Título en blanco no se escribe; miniplayer alterna favoritos.
- Detalle álbum en orden de disco; calidad/ahorro persisten.
- NO_EFFECTS apaga pulsación; switch avisa reinicio; IME Buscar.
- Backup/restore con callback garantizado; artScope se cancela;
  Handler por lifecycleScope; docstring honesto; sleep con etiqueta.

140 tests verdes.

## Auditoría 7.19 (novena pasada)

- Artistas sin duplicados por mayúsculas; álbum usa arte válido.
- Repair rota por lo hecho; fetch con expulsión; fetchArtist cacheado.
- Arte validado contra el título; junk cubierto en fase 3.
- Moov de 64 bits etiquetado bien.
- Worker nuevo a salvo del viejo; calidad resetea barra; jobs con id;
  huérfanos barridos; pausa suelta registro en el acto.
- Eliminar lista con confirm; artista vacío legible; stats en horas.
- Letra plana perezosa; play-all suena lo visible; conteo sin Guardada;
  error acotado; reverb fuera; respaldo en API <29; swipe sin parpadeo.

141 tests verdes.

## Auditoría 7.22 (décima pasada)

- Overlays vacíos se cierran solos; import no queda huérfano.
- Back de detalle/filtros solo en su pestaña; detalle fantasma se cierra.
- Sub-diálogos sobreviven a rotación.
- Índices al ejecutar; shuffle/repeat con restore; swap retoma solo.
- Frío re-invoca; borrado/reorden en frío mutan restore.
- UI tras actual visible; streamDead una vez; playlist por songId.
- Corazón en stream avisa; fila online con %; AddTo con keys.

141 tests verdes.

## Ficha de info 7.23

- Hoja inferior con grabber y scrim (antes: AlertDialog centrado y apretado).
- Cabecera: arte 76dp + título 2 líneas + artista · álbum.
- Píldoras de un vistazo: duración · formato · tamaño.
- Detalles en filas etiqueta/valor; artista y álbum navegables con chevron.
- Ruta con acción "Copiar ruta".
- Acciones: Reproducir / A lista + una fila de iconos
  (Favorito · Siguiente · Al final · Editar · Compartir).
- "Elegir varios" y "Excluir carpeta" como texto discreto al pie.
- Fuera el botón "Encolar" duplicado de "Siguiente".
- `normalizeSongMeta`: limpia el sufijo [videoId] y rescata el artista del
  título cuando quedó en "Desconocido" (consistente en lista y ficha).

## Selección múltiple 7.24

- Barra flotante que entra deslizándose (fade 160 + slide 220); contador en
  cápsula con AnimatedContent vertical; acciones icono+etiqueta
  (Todo/Nada · Borrar · Cerrar) con estados deshabilitados visibles.
- Fila marcada: fondo acento al 10 % (tween 180), check con spring desde
  0.6 + halo, crossfade 160 en la ranura derecha (check ⇄ corazón).
- Haptics al marcar (LongPress como "tic").

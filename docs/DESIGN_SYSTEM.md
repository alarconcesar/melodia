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
- **MiniPlayerDs** — flotante, apoyado **justo encima** de la nav: su margen
  inferior se calcula del alto real de la nav (`onGloballyPositioned`), nunca
  la tapa ni en gestos ni en navegación de 3 botones.
- **NowPlaying** — `BoxWithConstraints`: la carátula escala con la pantalla
  (`min(ancho−56dp, alto×0.34)` clamp 170–330dp) y un espaciador con `weight(1f)`
  absorbe el sobrante → los controles nunca se cortan. Fondo teñido con la
  carátula (alpha 0.16 + degradado + `blur(60dp)` en Android 12+).
  Fila superior: cerrar · etiqueta centrada · ♥ · **⋮** (cola, tono y
  velocidad, modo auto, descargar, añadir a playlist).
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


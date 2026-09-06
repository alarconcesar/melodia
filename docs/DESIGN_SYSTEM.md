# Melodia DS v1 — Sistema de diseño

Familiar-Spotify, minimalista moderno, **dark-first**.
Inspiración: fintech oscuro (fondo negro verdoso, cards redondeadas,
verde neón, pill-nav flotante, titulares grandes + secundarios atenuados).

> Vive en código: `ui/theme/Theme.kt` (tokens) + `ui/ds/Components.kt`.

## 🎨 Color

| Token | Dark | Light |
|---|---|---|
| `background` | `#0A0D0B` | `#F4F6F4` |
| `surface` (cards) | `#131715` | `#FFFFFF` |
| `surfaceVariant` (elevada) | `#1B211E` | `#E8ECE8` |
| `onBackground` / `onSurface` | `#F2F5F2` | `#10140F` |
| `onSurfaceVariant` (secundario) | `#9AA5A0` | `#5F6B62` |
| `outline` (bordes cristal) | `#2A332D` | `#D8DED8` |
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

`RHero 28dp` · `RCard 20dp` · `RRow 16dp` · `RChip 14dp` · pill = `CircleShape`.

## 🧩 Componentes

- **TrackRow** — art 56dp, título + artista·duración, **un corazón**,
  tap = play, long-press = ficha. Barras de EQ animadas si suena.
- **HeroCard** — "Seguir escuchando", FAB verde 72dp, borde hairline.
- **QuickCard** — grid 2 col (Favoritos / Aleatorio / Recientes / Artistas).
- **MelodiaPillNav** — barra flotante translúcida (80%) + borde + sombra,
  4 tabs fijos (layout estable, sin saltos).
- **MiniPlayerDs** — flotante sobre la nav, progreso de 2dp.
- **DsBackground** — degradado + glows (único fondo de la app).
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

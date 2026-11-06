# Melodia — contexto inicial de marketing

Revisión: 2026-11-06 · Rama revisada: `marketing-landing-20261105` · Android 7.33 / código 100.
Documento operativo inicial, no plan anual ni auditoría legal. Permite construir la landing ahora; las decisiones abiertas no bloquean el diseño.

## Producto y oferta

Melodia es un reproductor para Android que organiza y reproduce los archivos de audio del teléfono. Incluye listas, favoritos, cola, letras y ajustes de sonido; también ofrece búsqueda, reproducción y descarga de contenido de terceros, con límites de disponibilidad y autorización.

- **Canal definitivo elegido por César:** código abierto en el repositorio de GitHub `alarconcesar/melodia`, landing en GitHub Pages y APK en GitHub Releases. **No se publicará en Google Play.** El repositorio ya es público con licencia GPLv3; la web oficial es https://alarconcesar.github.io/melodia/ y el APK firmado se distribuye en Releases. El VPS queda como preview.
- **Artefacto revisado:** `app/build/outputs/apk/release/app-release.apk`. `aapt2 dump badging` devolvió `com.melodia.app`, 7.33 / 100, minSdk 24, targetSdk 36; `apksigner verify --print-certs` terminó con código 0. No se compiló ni se probó en un teléfono en esta revisión.
- Funciona sin cuenta de Melodia y sin anuncios. No convertir esto en una garantía de precio futuro, acceso ilimitado o ausencia de conexiones externas.
- No incluye derechos sobre música, letras o carátulas; tampoco promete un catálogo propio.

## Audiencia inicial y trabajo por resolver

**Hipótesis de audiencia, aún sin entrevistas ni métricas:** personas hispanohablantes con Android y música guardada en el teléfono que quieren volver a escucharla, ordenar su colección y ajustar el sonido sin crear otra cuenta.

- Principal: quien ya tiene archivos y carpetas y necesita encontrar un álbum, armar una lista y decidir qué sigue.
- Secundaria: quien valora letras disponibles y ajustes de sonido en una interfaz cuidada.
- Situaciones: trayectos sin conexión, escucha cotidiana con audífonos, organizar una colección local.
- Objeciones: instalar un APK fuera de la tienda, permisos de audio, compatibilidad, disponibilidad de letras y conexiones a terceros.
- No dirigir la campaña a menores ni posicionarla como una forma de eludir pagos, restricciones o derechos de otros servicios.

## Posicionamiento, promesa y beneficios

**Categoría:** reproductor de música para Android, centrado en la biblioteca personal.
**Promesa prioritaria por pedido de César:** descarga música gratis y escúchala sin conexión; la gratuidad corresponde a la función de Melodia, no a derechos sobre cualquier canción. Usar contenido propio o autorizado. Biblioteca, listas y sonido respaldan esa promesa.
**Diferencia a demostrar, no superioridad a proclamar:** control de la colección local + organización + ajustes de sonido en una experiencia visual coherente.

| Beneficio para quien escucha | Copy utilizable | Límite |
| --- | --- | --- |
| Volver a su colección sin depender del streaming | «Tus archivos, también sin conexión.» | Solo la reproducción de archivos locales; no toda la app. |
| Encontrar y ordenar lo que quiere oír | «De tu carpeta a tu próxima lista.» | El permiso de audio y los filtros de biblioteca afectan lo visible. |
| Decidir lo que sigue | «Prepara la cola. Guarda tu lista.» | La gestión de cola revisada es local; no prometer mezclar streams y archivos libremente. |
| Personalizar la escucha | «Ajusta el sonido a tu gusto.» | EQ de cinco bandas y graves según compatibilidad del equipo. |
| Seguir una canción por su letra | «La letra, cuando está disponible.» | LRCLIB; sincronización solo si existen tiempos. |
| Escuchar sin registrarse | «Sin anuncios. Sin cuenta de Melodia.» | No equivale a privacidad absoluta ni ausencia de servicios externos. |

## Voz y propuesta central

Español latino, cercano y directo. Hablar de canciones, listas, carpetas y sonido. Evitar jerga, superlativos, «revolucionario», «mejor que» y promesas de rendimiento sin mediciones.

- **Titular y subtexto vigentes:** fuente única en `web/index.html` (`#hero-title` y `.hero-description`). La descarga gratuita se comunica desde la primera pantalla; no relegarla a FAQ ni confundirla con que solo el APK sea gratis.
- **Único CTA de conversión:** «Descargar para Android». Puede repetirse, con el mismo destino.
- **Microcopy junto al CTA:** «Beta pública PREVIEW · APK firmado · Android 7.0 o posterior».
- **Respaldo breve:** «Sin anuncios. Sin cuenta de Melodia.»
- No añadir correo, formulario de captación, lista de espera ni insignia de Google Play.

## Respuestas de apoyo (la FAQ publicada tiene su fuente en `web/index.html`)

### 1. ¿Qué necesito para instalar Melodia?

Un teléfono con Android 7.0 o posterior. Esta beta pública PREVIEW se descarga como APK firmado, fuera de Google Play. Android puede pedirte permiso para instalar desde el navegador. Si ya usas una versión de pruebas con otra firma, puede requerirse desinstalarla; respalda tus listas antes porque podrías perder datos internos.

### 2. ¿Necesito una cuenta o voy a ver anuncios?

No necesitas crear una cuenta de Melodia y la app no integra anuncios. Algunas funciones consultan servicios externos, que tienen sus propias condiciones.

### 3. ¿Puedo escuchar sin internet? ¿Mis datos se quedan en el teléfono?

Puedes reproducir los archivos guardados en tu teléfono sin conexión. Listas, favoritos y ajustes se guardan localmente, pero letras, carátulas, metadatos y búsquedas pueden enviar consultas a terceros, incluso automáticamente cuando hay red. Android también puede respaldar datos de la app.

### 4. ¿Puedo organizar la música que ya tengo?

Sí. Puedes explorar por canciones, carpetas, álbumes y artistas, marcar favoritos, crear listas y preparar la cola local. Para mostrar tus archivos, Melodia necesita el permiso de acceso al audio de Android.

### 5. ¿Todas las canciones tienen letras y ecualizador?

Las letras provienen de LRCLIB y no están disponibles para todas las canciones; solo se sincronizan cuando incluyen tiempos. El ecualizador ofrece cinco bandas y refuerzo de graves, según la compatibilidad del dispositivo.

### 6. ¿Puedo importar listas o descargar música de otros servicios?

Hay funciones para consultar listas de YouTube y encolar descargas. Úsalas solo con contenido propio o cuya descarga esté autorizada, respetando derechos y condiciones del servicio. Dependen de terceros y pueden fallar o cambiar. La importación revisada no es una migración de Spotify ni una sincronización de cuentas; las colecciones se limitan a un máximo de 100 temas.

## Embudo orgánico inicial: TikTok / Reels → landing → descarga

**Primera iteración propuesta, no campañas ya ejecutadas:** tres videos verticales cortos, una tarea demostrable por pieza, reutilizados en TikTok y Reels. Audio propio o autorizado y grabación real de Android; si se muestra el demo del sitio, rotularlo «Demostración web».

| Pieza | Gancho | Prueba visual | Salida |
| --- | --- | --- | --- |
| Biblioteca local | «¿Todavía guardas música en tu Android?» | Abrir una carpeta y reproducir un archivo local, sin conexión. | «Prueba Melodia. Enlace en el perfil.» |
| Cola y listas | «Decide qué sigue, no empieces de cero.» | Añadir canciones, reordenar la cola y guardarla como lista. | Mismo enlace a la landing. |
| Sonido y letras | «Tu canción, con el sonido que eliges.» | Ajustar EQ en un equipo compatible; letra solo si está disponible y autorizada. | Mismo enlace a la landing. |

- **Landing:** conectar el mismo beneficio del video con el hero, prueba interactiva, beneficios, FAQ y un solo CTA de descarga. No competir con registro o suscripción.
- **Tras descargar:** explicar instalación del APK, permiso de audio y primera canción local. Descargar no demuestra que se instaló o reprodujo.
- **Medición propuesta, no implementada aquí:** vistas y clics disponibles en cada red; visitas a la landing y clics de descarga por origen/pieza si se habilita medición. Parámetros sugeridos: `utm_source=tiktok|instagram`, `utm_medium=organic_social`, `utm_campaign=melodia_preview`, `utm_content=biblioteca|cola|sonido`.
- Comparar piezas con datos reales; no fijar tasas, CAC, usuarios o metas ficticias. Sin analítica de activación verificada, validar la primera escucha con pruebas voluntarias y no publicar tasas de instalación.
- César aprueba contenido y derechos; el agente prepara piezas y copy dentro del alcance autorizado. No se crearon publicaciones ni tareas externas. No hay plan anual, pauta, email marketing ni monetización definida.

## Evidencia de implementación y límites de claims

Las rutas `K/…` significan `app/src/main/java/com/melodia/app/…`; números = líneas revisadas. Es verificación estática, salvo el APK indicado arriba; no certifica funcionamiento en todos los equipos.

| Claim / capacidad | Evidencia de código |
| --- | --- |
| Versión y compatibilidad mínima | `app/build.gradle.kts:12–18`; APK existente confirmado con `aapt2`. |
| Archivos locales y reproducción desde su ruta | `K/data/MusicRepository.kt:139–171,184–196`; `K/player/MusicPlayer.kt:205–232,250–261`. |
| Listas, favoritos y persistencia local | `K/data/PlaylistRepository.kt:8–24,44–65`; `K/player/QueueStore.kt:49–99`. |
| Reordenar cola local y guardarla como lista | `K/MainViewModel.kt:593–645`; las guardas excluyen streams. |
| Sin cuenta de Melodia ni anuncios | `K/ui/screens/SettingsScreen.kt:529–543`; entrada directa a `MelodiaScreen` en `K/MainActivity.kt:53–75`; dependencias en `app/build.gradle.kts:82–103`, sin SDK publicitario declarado. No es una auditoría de privacidad de terceros. |
| Letras disponibles, no universales | `K/data/lyrics/LyricsRepository.kt:21–30,58–69,127–134`; `K/MainViewModel.kt:1433–1451`. |
| Cinco bandas y graves sujetos al dispositivo | `K/player/EqualizerManager.kt:18–26,28–54`. |
| Segundo plano, controles y temporizador | `app/src/main/AndroidManifest.xml:7–11,19–25`; `K/player/MelodiaPlaybackService.kt:95–118,277–280`; `K/player/SleepTimer.kt:12–24`. |
| YouTube e importación real, no migración universal | `K/data/online/YtRepository.kt:55–56,87–101,345–347`; `K/MainViewModel.kt:1557–1584` encola descargas. |
| Pausa y reintento de descargas | `K/data/online/DownloadManager.kt:22–39,62–70,135–148`. No prometer que continúan si Android mata el proceso. |
| Conexiones externas y respaldos | `K/MainViewModel.kt:512–539`; `K/data/lyrics/LyricsRepository.kt:58–69`; `K/data/art/ArtworkRepository.kt:64–68,108–111`; manifest `:3,18`; detalle en `docs/store/PRIVACY.md:18–48`. |
| Verde de marca | `K/ui/theme/Theme.kt:24–27`: `#2EE56B`. |

**No publicar:** reseñas, estrellas, usuarios, descargas, premios o comparativas inventadas; «100 % privado», «ningún dato sale», «nunca se conecta», «cero consumo de datos» para toda la app; calidad lossless/Hi-Res, formatos universales, batería o fluidez garantizadas; «descarga cualquier canción gratis», catálogo licenciado, integración oficial de terceros o aprobación de Play. No afirmar que una cláusula de uso responsable resuelve derechos.

## Contradicciones y decisiones abiertas, sin bloquear la landing

- `README.md:4–8` dice «sin tracking» y minSdk 26 / targetSdk 34: no usarlo como fuente vigente. El código y el APK indican 24 / 36 y existen consultas externas.
- `docs/store/RELEASE.md:3,48–49` describe 7.32 / 99; el código y APK revisados son 7.33 / 100. Sus líneas 61–69 se contradicen sobre una política embebida; no llevar esa afirmación al copy.
- `docs/store/STORE_LISTING.md:3,69–75` es borrador de tienda, no publicación. La aprobación de contacto, licencias, cuestionarios y pruebas físicas permanece pendiente; no sustituirla con claims. No añadir correo por instrucción expresa de César.
- La vista previa se sirve en `https://vnic-cesar.tailb6ee43.ts.net/melodia/`, con el APK release 7.33/100 y su ficha de integridad en `descargas/version.json`. El padre verificó por HTTPS la página, los archivos y la descarga. El dominio comercial, el lanzamiento y las campañas siguen pendientes de aprobación; la vista previa conserva `noindex`.
- El demo funcional con audio original es una **demostración web**, no una captura ni una versión Android ejecutándose en el navegador. Si aún no existen capturas reales, no fabricarlas.

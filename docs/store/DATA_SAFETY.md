# Melodia — Preparación de Seguridad de los datos y declaraciones

Revisión documental: 5 de noviembre de 2026. Base leída: `1f91a7e`, versión 7.27. Revalidar contra el artefacto candidato después de los cambios de SDK, recursos o dependencias.

**Estado: BORRADOR INCOMPLETO. No enviado ni validado en Play Console. No constituye una declaración de «no recopila» o «no comparte».** El inventario procede de lectura de código; no incluye una captura de tráfico del dispositivo, una auditoría de las dependencias transitivas ni confirmaciones de los destinatarios.

## 1. Reglas para decidir las respuestas

- Google Play entiende por recogida la transmisión fuera del dispositivo, también mediante bibliotecas o SDK y aunque el destino sea un tercero en vez del desarrollador. La falta de un servidor propio no es una exención.[1]
- El tratamiento exclusivamente local puede quedar fuera de la recogida declarable. Hay que separar ese caso de las consultas externas derivadas de la misma biblioteca o actividad.[1]
- El tratamiento temporal también debe responderse en el formulario. Solo corresponde indicar ese carácter si se puede justificar que los datos se usan en memoria durante lo necesario para la solicitud. **La retención de YouTube, LRCLIB, Apple y sus infraestructuras no está verificada**; una respuesta rápida o una URL temporal no demuestran que no haya registros.[1]
- «Compartir» tiene excepciones concretas, entre ellas proveedores que actúan por instrucciones del desarrollador y determinadas acciones iniciadas por el usuario o avisos y consentimientos destacados. No asumir que cualquier servicio externo es un encargado ni que toda consulta automática satisface una excepción. Documentar una justificación por flujo, no una excepción global.[1]
- Declarar una recogida como opcional exige un control real disponible para todos los usuarios. No abrir la sección en línea no evita las consultas de carátulas, reparación o letras. El ahorro de datos no las desactiva. El modo avión del sistema no demuestra una opción de exclusión dentro de la app.[1]
- La política de privacidad debe abarcar el tratamiento completo, no solo lo que finalmente aparezca en la ficha de Seguridad de los datos.[2]

## 2. Inventario comprobado en código

Las rutas siguientes son relativas a `app/src/main/java/com/melodia/app/`, salvo indicación contraria. Las líneas pueden desplazarse; se indican símbolos para volver a localizar la evidencia.

| Flujo | Datos y finalidad observados | Destino o almacenamiento | Evidencia |
| --- | --- | --- | --- |
| Biblioteca local | Audios, identificadores, rutas, título, artista, álbum y duración para biblioteca y reproducción | MediaStore y memoria; listas/favoritos en Room | `data/MusicRepository.kt`, `data/PlaylistRepository.kt`, `AppContainer.kt` |
| Organización e historial | Listas, favoritos, estadísticas de escucha, posición y cola; preferencias de sonido y apariencia | `melodia.db` y preferencias privadas | `MainViewModel.kt` (`statsSummary`, `maybeRestoreQueue`), `player/QueueStore.kt` |
| Búsqueda y audio de YouTube | Consulta, identificadores y enlaces de vídeos/listas; solicitudes para resultados, audio y miniaturas | YouTube y su distribución; búsquedas e historiales también en preferencias `melodia_yt` | `data/online/YtRepository.kt`, `YtDownloader.kt`, `MainViewModel.kt` (`searchYt`, `playYt`) |
| Búsqueda de Inicio | El texto de al menos dos caracteres se consulta también en YouTube tras una pausa, no solo en el índice local | YouTube | `ui/screens/MelodiaScreen.kt` (`HomeTab`), `MainViewModel.kt` (`searchHomeYt`) |
| Resolución anticipada | Solicitudes para resolver el enlace de audio de los primeros resultados sin pulsar reproducir | YouTube/infraestructura implicada; caché de enlaces en memoria | `MainViewModel.kt` (`searchHomeYt`, `searchYt`), `YtRepository.kt` (`prefetchAudio`, `bestAudio`) |
| Carátulas automáticas | Artista y título de canciones locales que siguen sin arte; solicitudes de imágenes resultantes | iTunes Search de Apple y servidores de imágenes; preferencias `melodia_art` y archivos `filesDir/art` | `MainViewModel.kt` (`enrichArtwork`), `data/art/ArtworkRepository.kt` (`fetch`, `embedded`) |
| Reparación al inicio | Título de descargas antiguas con artista sin resolver, después de intentar fila/etiqueta local | iTunes Search de Apple; fichas locales `melodia_dl_meta` | `MainViewModel.kt` (inicialización), `YtRepository.kt` (`repairDownloadMeta`), `ArtworkRepository.kt` (`fetchArtist`) |
| Letras automáticas | Título, artista y, según el método, álbum y duración; búsqueda alternativa con texto derivado | LRCLIB; caché privada `cacheDir/lyrics` | `MainViewModel.kt` (observador de `currentSong`, `loadLyrics`), `data/lyrics/LyricsRepository.kt` |
| Finalización de descargas | Consulta de letra, miniaturas y artista; audio y etiquetas guardadas | LRCLIB, YouTube/miniaturas, posible iTunes; Música/Melodia, fichas e historial local | `MainViewModel.kt` (`dl.onFinished`, `delayWarmLyric`), `YtRepository.kt` (`storeDownload`, `resolveArtist`) |
| Cola de descarga | Enlaces e identificadores, título, artista, miniatura, calidad y progreso; archivos parciales | Preferencias `melodia_dlq` y `cacheDir/dl`; solicitudes al destino del audio | `data/online/DownloadManager.kt` (`persist`, `readQueue`, `pause`, `cancel`) |
| Conexiones de red | IP de salida visible al receptor, cabeceras de cliente y solicitudes de recursos | Cada servicio y su infraestructura | `ArtworkRepository.kt`, `LyricsRepository.kt`, `YtDownloader.kt`, `YtRepository.kt` (`openConn`); rutas de imágenes/audio a comprobar en tráfico |
| Diagnóstico | Excepciones y traza; podrían contener rutas u otros detalles de la operación | Archivo privado `last_crash.txt`, pantalla de error y registros locales; copia manual opcional | `MelodiaApp.kt`, `CrashScreen.kt`, `MainActivity.kt`; no se identificó envío automático al desarrollador |
| Rendimiento | Mediciones de fotogramas mostradas en ajustes | Estado local de la aplicación; no se identificó telemetría remota | `perf/FrameMonitor.kt`, `ui/screens/SettingsScreen.kt` |
| Exportación voluntaria | Listas, rutas, metadatos y favoritos, sin copiar audio | JSON en Descargas/Melodia; posible copia/sincronización posterior por herramientas del usuario | `MainViewModel.kt` (`backup`, `restore`), `PlaylistRepository.kt` (`exportJson`) |
| Respaldo de Android | Datos internos potencialmente incluidos en copia o transferencia | Servicio de respaldo/transferencia del sistema, según configuración y fabricante | `app/src/main/AndroidManifest.xml`: `allowBackup=true`, sin reglas específicas declaradas en el manifiesto leído; contrastar alcance real con Android.[4] |

El código propio revisado no identifica cuentas de Melodia, publicidad, un identificador publicitario ni un servicio propio de analítica. Esto no certifica los tratamientos internos de los servicios externos, del sistema o de todas las dependencias.

## 3. Categorías candidatas: no son casillas finales

Los nombres de categorías y finalidades deben cotejarse con el formulario vigente. Las categorías siguientes orientan la revisión; no deben copiarse como declaración cerrada.[1]

| Dato/flujo | Categoría a evaluar | Finalidad observada o propuesta | Recogida/compartición y decisión pendiente |
| --- | --- | --- | --- |
| Texto enviado a YouTube desde ambos buscadores | Actividad de la aplicación → Historial de búsqueda en la aplicación | Funcionalidades de la aplicación | Hay transmisión; evaluar y declarar el dato correspondiente. Compartición o excepción requieren justificación por flujo, incluida la búsqueda automática de Inicio |
| Selección de vídeos/listas y solicitudes de audio, resolución anticipada | Actividad de la aplicación → Interacciones con la aplicación u Otras acciones, según el dato concreto | Funcionalidades de la aplicación | Separar elección de reproducción de resolución automática; verificar campos de NewPipeExtractor y uso de cada destinatario |
| Títulos, artistas, álbumes o duración derivados de biblioteca/reproducción | Evaluar Archivos y documentos cuando sea información sobre archivos; actividad de la aplicación cuando revele acciones; otro contenido solo si su definición corresponde | Funcionalidades de la aplicación | No confundir un título transmitido con una subida del archivo de música; justificar categoría con los campos realmente enviados a Apple/LRCLIB |
| Archivos de audio locales | Archivos de audio → Archivos de música, si se transmitieran como tales | Reproducción local | No se observó subida del audio local en los flujos revisados. Separar lectura local, metadatos enviados y respaldo del sistema; no convertirlo en «ningún dato sale» |
| IP y cabeceras técnicas | Evaluar según uso real; no clasificar automáticamente como ubicación ni como identificador persistente | Atención de solicitudes; usos posteriores desconocidos | Si algún receptor deriva ubicación aproximada o genera/usa identificadores, evaluar la categoría correspondiente. Obtener evidencia; IP visible no equivale a un permiso de geolocalización |
| Listas, favoritos, cola y estadísticas locales | Actividad de la aplicación u otras categorías solo si salen por un flujo declarable | Funcionalidades de la aplicación | Localidad observada no cubre por sí sola exportaciones, respaldo Android ni metadatos derivados enviados |
| Trazas y mediciones locales | Información y rendimiento de la aplicación → Registros de fallos/Diagnósticos si se transmitieran | Diagnóstico local | No se observó envío automático. Evaluar copia voluntaria, respaldo y destinatarios antes de decidir alcance o excepción |
| Respaldo JSON y copia de Android | Depende del contenido y mecanismo concreto | Exportación, restauración y transferencia | No declarar «no aplica» por defecto: verificar qué sale, quién puede leerlo y qué excepciones del formulario son justificables |

Para cada dato que corresponda declarar, registrar por separado: recogida, compartición o excepción motivada, finalidad, opcional/obligatorio, tratamiento temporal o no, cifrado y posibilidad de solicitar eliminación. No asignar fines publicitarios o de análisis propios sin evidencia; tampoco negar los posibles usos de terceros sin verificarlos.[1]

## 4. Conservación y borrado: límites de lo comprobado

| Ubicación | Conservación observada o conocida | Borrado y limitaciones |
| --- | --- | --- |
| Room y preferencias internas | Sin caducidad general; históricos con límites por cantidad en algunas funciones | Controles disponibles de listas, favoritos, estadísticas e historial de descargas; para todo el estado interno, Borrar datos de Android o desinstalar |
| Letras/carátulas y archivos parciales | Poda por cantidad o limpieza de caché; los parciales pueden persistir para reanudar | Cancelación de descarga y controles de almacenamiento del sistema; limpiar caché no borra todo el estado |
| Audios en Música/Melodia | Hasta borrado del archivo | No desaparecen necesariamente con desinstalación o borrado interno; comprobar permisos y confirmaciones de Android |
| JSON en Descargas/Melodia | Hasta borrado del archivo y de sus posibles copias | Eliminar aparte; no tiene cifrado propio ni representa todos los datos internos |
| Informe de fallo local | Puede sustituirse por otro y limpiarse al arrancar correctamente o desde diagnóstico | Revisar antes de copiar/compartir; no prometer envío ni eliminación remotos inexistentes |
| Copias/transferencia de Android | Depende de configuración, versión, fabricante y servicio; alcance aún no probado | Gestionar en el sistema; desinstalar no demuestra eliminación de respaldos, y restaurar puede recuperar datos.[4] |
| YouTube, LRCLIB, Apple y distribución | **Retención y usos posteriores desconocidos en esta revisión** | No hay mecanismo de la app comprobado para borrar sus registros; investigar sus políticas y derechos. No prometer borrado remoto desde Melodia |

La app no permite crear cuentas de Melodia en el código revisado. La obligación específica de una ruta de eliminación de cuentas se evalúa según esa función; no significa que el borrado de datos sea irrelevante ni que deba marcarse «no aplica» en todas las preguntas.[2]

## 5. Respuestas y declaraciones que todavía requieren aprobación

| Pregunta o requisito | Estado honesto | Evidencia/decisión necesaria para cerrar |
| --- | --- | --- |
| ¿Recoge o comparte datos? | Sin respuesta global final; existen transmisiones a terceros | Resolver categorías y excepciones por flujo; aprobar con titular, no responder «no» por ausencia de servidor propio |
| ¿Se tratan temporalmente? | No justificado para los receptores externos | Confirmar conservación en memoria y ausencia de registros persistentes cuando se pretenda esa respuesta |
| ¿Son opcionales? | No demostrado para las consultas automáticas | Revisar controles reales para todos los usuarios; decidir si se requiere un control o aviso adicional |
| Cifrado en tránsito | HTTPS explícito en iTunes y LRCLIB; resto sin validación exhaustiva | Capturar/inspeccionar destinos, redirecciones y enlaces dinámicos de audio e imágenes del artefacto final; no marcar «todo cifrado» solo por ejemplos |
| Solicitudes de eliminación | Borrado local descrito; borrado externo sin comprobar | Resolver contacto/mecanismo real, instrucciones para respaldos y datos de terceros; no inventar un servicio de eliminación |
| Aviso destacado y consentimiento | La sección Privacidad informa de servicios y consultas automáticas, pero es un resumen | Evaluar si hace falta aviso/consentimiento antes de la transmisión según expectativas y datos; documentar decisión y secuencia. Tener permiso de audio no equivale por sí solo a consentimiento para cualquier envío externo.[2] |
| Política completa dentro de la app | Pendiente | `SettingsAboutContent` solo muestra resumen y fuentes; no contiene este texto completo ni enlace público. No dar por cumplido el acceso completo |
| Contacto público y de privacidad | **Pendiente por instrucción expresa del titular: no añadir ningún correo** | Proporcionar y aprobar un punto de contacto real o mecanismo de consultas antes del envío; la política exige ese elemento. No se inventó correo, URL ni marcador en `PRIVACY.md`.[2] |
| Responsable y mercados | Sin validar | Confirmar identidad del titular, mercados, roles y bases de tratamiento aplicables; revisar derechos y transferencias internacionales sin inventar garantías |
| URL de privacidad | No publicada en este trabajo | Cuando se autorice, disponer de URL pública activa, sin inicio de sesión ni bloqueo geográfico; el archivo del repositorio no equivale a esa URL.[2] |
| Anuncios | No se identificó integración de anuncios en el código/dependencias directas revisados | Revalidar el artefacto final y sus dependencias antes de contestar la declaración |
| Acceso a la aplicación | Sin cuenta de Melodia observada | Documentar permisos y acceso a funciones para revisión; no crear credenciales ficticias |
| Servicio en primer plano multimedia | Declarado en manifiesto | Preparar justificación y evidencia real de reproducción iniciada por el usuario y controles; completar lo que pida la consola, no marcar como enviado |
| Clasificación IARC y audiencia | No completadas, sin edad anticipada | Revisar contenido externo y contestar cuestionario; aprobar audiencia objetivo por separado.[8] |
| Licencias de software | Revisión pendiente | Inventario de dependencias directas/transitivas, incluida NewPipeExtractor; validar compatibilidad, atribuciones, avisos y obligaciones de distribución |
| Derechos de audio, letras, carátulas, marcas y condiciones de servicios | **Revisión pendiente; punto 1 sin resolver** | Comprobar autorizaciones, usos permitidos y términos de YouTube, LRCLIB y Apple. Mantener YouTube no implica conformidad; la redacción responsable no sustituye licencias.[6] |
| Capturas y recursos de tienda | Revisión pendiente | Comprobar derechos y coherencia con versión real; sin datos privados ni contenido usado sin autorización.[6] |

## 6. Puerta de cierre

- [ ] Reproducir y registrar cada flujo de red en el artefacto candidato, con biblioteca y contenido de prueba autorizados.
- [ ] Auditar NewPipeExtractor, carga de imágenes, reproductor y dependencias transitivas; identificar destinos y campos reales, sin atribuir al dispositivo el modelo ficticio del agente de usuario del código.
- [ ] Revisar políticas de destinatarios y documentar lo que siga siendo desconocido; no deducir retención de la caché local.
- [ ] Resolver categorías y decisiones de recogida/compartición, opcionalidad y tratamiento temporal con evidencia.
- [ ] Probar borrado, confirmaciones, cachés, exportación, respaldo Android y restauración mediante `QA_DEVICE_CHECKLIST.md`.
- [ ] Resolver contacto real, revisión del responsable, acceso completo a privacidad y URL pública cuando se autoricen.
- [ ] Resolver avisos/consentimientos aplicables, licencias/derechos y decisiones de audiencia sin cerrar por esta edición el punto 1.
- [ ] Con aprobación del titular y solo con autorización de envío, trasladar las respuestas a Play Console y conservar la versión aprobada y sus pruebas.

**Ninguna casilla anterior está cerrada por crear estos documentos. No se ha publicado una web, compilado la app, completado un formulario ni enviado una versión a Google Play en esta tarea.**

## Fuentes

[1] https://support.google.com/googleplay/android-developer/answer/10787469?hl=es — Seguridad de los datos de Google Play
[2] https://support.google.com/googleplay/android-developer/answer/10144311?hl=es — Política de datos de usuario
[4] https://developer.android.com/identity/data/autobackup?hl=es-419 — Copias de seguridad automáticas en Android
[6] https://support.google.com/googleplay/android-developer/answer/9888072?hl=es — Propiedad intelectual en Google Play
[8] https://support.google.com/googleplay/android-developer/answer/9859655?hl=es — Clasificación del contenido en Google Play

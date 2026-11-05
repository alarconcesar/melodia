# Melodia — Guion de pruebas en dispositivos físicos

Revisión: 5 de noviembre de 2026. **Guion preparado; pruebas físicas NO EJECUTADAS por esta revisión.** No hay dispositivos, resultados de instalación ni capturas que permitan marcar un caso como aprobado. La lectura de código no sustituye esta ejecución.

## 1. Preparación y evidencia

- Usar el artefacto candidato que entregue el responsable de compilación, idealmente la instalación derivada del AAB de distribución y su firma prevista. Un resultado en la variante de depuración no acredita el artefacto de distribución. No compilar ni publicar como parte de este guion documental.
- Registrar versión/número de versión, revisión del código, identificador de paquete, huella de firma y SHA-256 del artefacto utilizado; aportar sus valores reales, no estimados. Confirmar en el artefacto su versión mínima de Android y su objetivo API 36 antes de evaluar esa migración.
- Registrar por teléfono: marca/modelo, versión de Android/API, compilación del sistema, memoria disponible, restricciones de batería, conexión, tamaño/fuente de pantalla y navegación por gestos o botones. No almacenar identificadores privados innecesarios.
- Preparar audios propios o autorizados: con/sin carátula y etiquetas, dos álbumes homónimos de artistas distintos, duplicados en cola, nombre con acentos, archivo movido/dañado y al menos un archivo creado por otra app. Para red, usar contenido con autorización suficiente; una prueba técnica no resuelve derechos ni condiciones del servicio.
- Antes de casos destructivos, respaldar fuera del alcance de borrado los datos de prueba que se quieran conservar y documentar exactamente qué se puede borrar. No usar la biblioteca personal como material desechable.
- Conservar capturas o vídeo, hora, pasos, resultado real y registro técnico mínimo, ocultando notificaciones, rutas y otros datos personales. Para red, observar destinos y solicitudes con herramientas autorizadas sin interceptar tráfico de otras apps ni extraer credenciales; si no se puede comprobar el contenido cifrado, registrar esa limitación en vez de inferirlo.

Estados válidos: **No ejecutado**, Aprobado, Fallido, Bloqueado o No aplica con motivo. Cada ejecución debe registrar caso, dispositivo/API, artefacto, fecha, persona, esperado, observado, evidencia y referencia del fallo. «Sin evidencia» no significa aprobado.

## 2. Matriz mínima de Android

Una fila representa una familia que debe cubrirse o justificarse expresamente; no asegura que todas sus versiones estén probadas. Añadir una ejecución por cada teléfono/versión real. Repetir los casos comunes en cada familia y los específicos donde correspondan.

| Android / API | Foco de esa familia | Estado | Evidencia |
| --- | --- | --- | --- |
| Android 7–7.1 / 24–25 | Límite mínimo declarado en la base; lectura/escritura heredadas; no invocar clases exclusivas de API nuevas | No ejecutado | Sin dispositivo ni resultado |
| Android 8–9 / 26–28 | Canal de notificación, servicio en primer plano y escritura de descargas/respaldo con permiso heredado | No ejecutado | Sin dispositivo ni resultado |
| Android 10 / 29 | Almacenamiento por ámbito, permisos recuperables al editar/borrar archivos ajenos; caso Q18 especialmente sensible | No ejecutado | Sin dispositivo ni resultado |
| Android 11–12L / 30–32 | Confirmaciones de MediaStore, retirada de permisos y reanudación del servicio | No ejecutado | Sin dispositivo ni resultado |
| Android 13 / 33 | `READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, concesión/denegación independientes y controles multimedia | No ejecutado | Sin dispositivo ni resultado |
| Android 14 / 34 | Servicio de tipo `mediaPlayback`, permisos declarados y comportamiento de segundo plano | No ejecutado | Sin dispositivo ni resultado |
| Android 15 / 35 | Foco de audio, servicio y controles desde segundo plano; bordes y barras del sistema | No ejecutado | Sin dispositivo ni resultado |
| Android 16 / 36 | Artefacto con objetivo API 36; permisos, servicio, notificación, atrás predictivo y adaptación de pantalla | No ejecutado | Sin dispositivo ni resultado |

Priorizar un teléfono físico API 36, otro API 33/34 y API 29 por los cambios de permisos/almacenamiento, sin convertir esa prioridad en aprobación de las familias faltantes. Usar además más de un fabricante para restricciones de batería y Bluetooth. Si se soportan pantallas grandes, añadir tablet o plegable físico API 36: la plataforma cambia la aplicación de restricciones de orientación/tamaño a partir de 600 dp.[7]

## 3. Casos de ejecución

Todos los casos parten de **No ejecutado**. «Todas» significa todas las familias soportadas de la matriz, no una afirmación de que hayan sido ejecutadas.

| ID | Ámbito | Pasos en dispositivo físico | Criterio de fallo | Estado |
| --- | --- | --- | --- | --- |
| Q01 | Todas | Instalar limpio el candidato; abrir, cerrar y abrir; identificar versión y permisos solicitados | No instala en una versión declarada compatible, cierre inesperado, pantalla en blanco o versión/firma distinta de la registrada | No ejecutado |
| Q02 | Todas | Actualizar una instalación previa con firma compatible y listas/favoritos/cola preparados; comprobar estado antes/después | Pérdida o duplicación no consentida; migración corrupta; reemplazo de firma que obligue a borrar datos presentado como actualización normal | No ejecutado |
| Q03 | 24–32 y 33–36 | Denegar lectura de biblioteca; volver a abrir; conceder desde ajustes y regresar | Cierre, bucle bloqueante, biblioteca leída sin autorización o no recuperable tras conceder; ausencia de explicación útil | No ejecutado |
| Q04 | 24–28 | Denegar y conceder escritura; intentar guardar audio autorizado y exportar respaldo | Éxito falso, archivo incompleto o pérdida de datos; operación que exige escritura sin manejar denegación | No ejecutado |
| Q05 | 33–36 | Conceder audio y denegar notificaciones; invertir permisos en otra instalación; probar reproducción local/en línea | Permiso de notificación usado como condición de lectura, cierre, controles incoherentes o servicio inválido; valorar exenciones multimedia, no exigir que desaparezca toda notificación.[5] | No ejecutado |
| Q06 | 26–36 | Con notificaciones permitidas, reproducir, bloquear pantalla, pausar/saltar/buscar desde notificación y bloqueo; repetir con canal desactivado | Acciones equivocadas, sesión duplicada, estado/título incoherente, servicio sin notificación cuando el sistema la exige o cierre | No ejecutado |
| Q07 | Todas | Reproducir local y en línea; apagar pantalla y dejar en segundo plano al menos 15 minutos; repetir con ahorro de batería | Corte sin explicación bajo condiciones normales, reanudación imposible o notificación estancada; documentar por separado restricciones explícitas del fabricante | No ejecutado |
| Q08 | Todas, énfasis 35–36 | Desde app visible, iniciar reproducción; salir y controlarla desde notificación, widget y auricular con servicio frío/caliente | Excepción de arranque/foco, botones mudos, audio duplicado o reproducción sin acción del usuario | No ejecutado |
| Q09 | Todas con Bluetooth | Emparejar auricular desde Android; conectar/desconectar durante audio; pulsar reproducir/pausa/siguiente; repetir con app fuera de pantalla | Salida inesperada a altavoz a volumen no controlado, pausa/reanudación errónea, cola distinta o petición innecesaria de acceso a datos Bluetooth | No ejecutado |
| Q10 | Todas con telefonía | Recibir y atender una llamada durante audio; probar rechazo y finalización; repetir con otra app que tome foco | Música invasiva durante llamada/foco exclusivo, volumen que no vuelve o reproducción inesperada tras una pausa manual | No ejecutado |
| Q11 | Todas | Pausar manualmente; cerrar pantalla, cambiar de app, desconectar auricular y recuperar foco | Reanuda sin intención o pierde el estado de pausa | No ejecutado |
| Q12 | Todas | Preparar cola/posición; provocar muerte de proceso por el sistema en condiciones controladas; reabrir y pulsar reproducir/siguiente | Cola corrupta, pérdida no explicada, controles sin efecto, audio duplicado o reproducción automática al abrir | No ejecutado |
| Q13 | Todas | Forzar detención desde ajustes, después abrir manualmente; diferenciar de quitar la tarea de recientes | Se promete continuidad durante detención forzada, reaparece sin acción permitida o no recupera estado al abrir; documentar semántica real del sistema | No ejecutado |
| Q14 | Todas | Reiniciar el teléfono con reproducción activa y otra vez en pausa; abrir app y probar control físico/widget | Arranque automático no solicitado tras reiniciar, cola rota, estado reproducir/pausa incoherente o botón sin respuesta | No ejecutado |
| Q15 | Todas | En cola local con duplicados, insertar siguiente/final, mover/quitar, repetir y usar aleatorio; cerrar/reabrir | Suena otra ocurrencia, se borra una distinta, aparece un tema retirado o divergen UI y audio | No ejecutado |
| Q16 | Todas | Mover/borrar un archivo desde otra app y desmontar almacenamiento extraíble si existe; recargar biblioteca | Cierre, bloqueo, barrido destructivo de listas/fichas ajenas o canción ausente presentada como reproducible sin aviso | No ejecutado |
| Q17 | Todas | Borrar un audio de prueba propio de Melodia; cancelar la confirmación de la app; repetir aceptando | Borra sin confirmación prevista, declara éxito al cancelar, elimina otro archivo o deja estado imposible de actualizar | No ejecutado |
| Q18 | 29 | Editar y borrar un audio de otra app: provocar permiso recuperable; denegar y aceptar en ejecuciones separadas, verificando archivo real | No aparece vía válida de consentimiento, falla por API no disponible, se altera sin permiso o UI/listas eliminan algo que sigue en disco | No ejecutado |
| Q19 | 30–36 | Borrado/edición de archivo ajeno y lote mixto propio/ajeno; denegar y aceptar el diálogo del sistema | Limpia estado de elementos no autorizados, pide de nuevo borrar lo ya eliminado, informa éxito falso o toca archivos fuera de selección | No ejecutado |
| Q20 | 36 con objetivo 36 | Probar un lote grande de borrado con datos desechables, incluidos más de 2000 URI si la selección lo permite | La petición excede el límite del sistema sin división/manejo útil; no debe declararse éxito sin comprobar archivos. El límite de 2000 por petición está documentado para objetivo API 36 o superior.[9] | No ejecutado |
| Q21 | Todas | Descargar audio autorizado; alternar cola, pausa, reanudar, cancelar y reintento; repetir sin espacio y con pérdida de red | Corrupción, archivo ajeno sobrescrito, parciales sin control, progreso falso o error de almacenamiento presentado como éxito | No ejecutado |
| Q22 | Todas | Con descargas activas/pausadas, terminar proceso y reiniciar teléfono; volver a abrir sin tocar reanudar | Pierde entradas, reinicia transferencia sin intención, restaura como activa sin trabajo real o corrompe el parcial; lo previsto es recuperar en pausa, no garantizar descarga con proceso muerto | No ejecutado |
| Q23 | Todas | Tras descarga completa, comprobar archivo, biblioteca, etiquetas, carátula e historial; repetir nombre similar y duplicado | Audio incompleto, colisión de archivo, artista/álbum incorrectos fijados sin control o finalización falsa | No ejecutado |
| Q24 | Todas | Importar/listar colección autorizada que exceda el límite de la app; cancelar y confirmar cada ruta disponible | No advierte recorte antes de la acción, descarga elementos distintos o ignora cancelación | No ejecutado |
| Q25 | Todas | Descargar colección en red medida: comparar aceptar/rechazar confirmación; repetir rutas desde Inicio/sección en línea y una descarga individual | El diálogo prometido no aparece, sigue tras cancelar o se interpreta ese diálogo como consentimiento global de privacidad | No ejecutado |
| Q26 | Todas | Biblioteca con metadatos de prueba y sin carátulas: observar primera carga/recarga y reparación al abrir con red, sin entrar en sección en línea | Destinos/datos no inventariados, subida del audio local no descrita o avisos/consentimientos exigibles ausentes; registrar las consultas automáticas reales de Apple | No ejecutado |
| Q27 | Todas | Reproducir canción local sin abrir Letras; cambiar canción; restaurar cola y observar solicitudes de LRCLIB | Envía campos no descritos, hace petición pese a una exclusión que la UI prometa o existe falta de aviso/consentimiento necesario; no considerar ausencia de ventana Letras como ausencia de envío | No ejecutado |
| Q28 | Todas | Escribir consulta de prueba en Inicio, esperar; comparar con búsqueda en sección en línea; no pulsar resultados y observar resolución anticipada | Presenta búsqueda como solo local, envía antes del control/consentimiento exigible, o tráfico real contradice inventario; registrar consultas y enlaces anticipados | No ejecutado |
| Q29 | Todas | Sin red, reproducir local, usar carátulas/letras cacheadas y volver a conectar; activar ahorro de datos | Audio local depende obligatoriamente de red, bloqueo por error externo o ahorro de datos se presenta falsamente como desconexión total | No ejecutado |
| Q30 | Todas | Observar destinos/protocolos de letras, carátulas, audio y redirecciones, incluida carga de imágenes | Tráfico de datos por transporte inseguro, destino no documentado o imposibilidad de sostener la respuesta propuesta de cifrado; sin visibilidad suficiente, marcar Bloqueado, no Aprobado | No ejecutado |
| Q31 | Todas | Borrar historial de descargas, estadísticas, favoritos y una lista; reiniciar app; comprobar audio y resto de datos | Datos borrados reaparecen sin restauración, se borra audio al quitar solo historial/lista o se promete borrar registros remotos | No ejecutado |
| Q32 | Todas | Borrar solo caché; después Borrar datos de Android en otra ejecución preparada; verificar archivos compartidos y estado interno | Se confunden ambos alcances, sobreviven datos internos sin explicación o se borran audios/JSON externos sin acción correspondiente | No ejecutado |
| Q33 | Todas | Exportar JSON y revisar contenido con datos ficticios; restaurar dos veces; eliminar JSON aparte | Respaldo se anuncia como cifrado/completo sin serlo, incluye audio inesperado, pierde datos silenciosamente o duplica masivamente listas | No ejecutado |
| Q34 | Donde haya respaldo/transferencia Android | Configurar respaldo real con datos ficticios; desinstalar/reinstalar o transferir en entorno de prueba; registrar qué vuelve y retirar copia de prueba de forma autorizada | Política/inventario niegan la copia o prometen eliminación que no ocurre; si no se puede ejecutar respaldo/restauración, registrar Bloqueado | No ejecutado |
| Q35 | Todas | Retirar permisos tras uso y regresar; revisar accesos a biblioteca, notificaciones y estado de datos | Acceso no autorizado, cierre, bucle de solicitudes o afirmación de que retirar permisos borra datos de terceros | No ejecutado |
| Q36 | Todas | Abrir Privacidad desde Ajustes; comprobar texto completo o enlace real, disponibilidad, destinatarios, automatismos y borrado | Solo hay resumen cuando se requiere política completa, URL inexistente, marcador o contacto ficticio, información contradictoria o ausencia de vía real de consultas; el cierre depende de resolver esos pendientes | No ejecutado |
| Q37 | Todas | Revisar secuencia de primer uso y primera transmisión de cada flujo con los avisos/consentimientos que se hayan aprobado; denegar y volver a aceptar si existe ese control | Transmisión antes del consentimiento requerido, consentimiento no revocable donde proceda o política usada como único aviso cuando se exige uno destacado | No ejecutado |
| Q38 | Todas, énfasis 35–36 | Rotar, aumentar fuente, abrir teclado, usar gestos/botones atrás en cola, reproductor, letras, ajustes y diálogos; probar bordes del sistema | Controles cortados/inaccesibles, toques bloqueados, salida de app en vez de cerrar capa o pérdida de estado; en API 36 verificar atrás predictivo real.[7] | No ejecutado |
| Q39 | Todas; pantalla grande API 36 si aplica | Probar TalkBack, objetivos táctiles, modo claro/oscuro, pantalla dividida y ventana redimensionada | Acción sin nombre, control no operable o contenido esencial inaccesible tras cambio de tamaño/orientación.[7] | No ejecutado |
| Q40 | Todas | Probar ecualizador, graves, temporizador, velocidad/tono y cambio de salida; pausar durante fundido y reabrir | Silencio persistente, volumen inesperado, temporizador que no pausa, estado falso o fallo en equipo sin efecto compatible | No ejecutado |

## 4. Observaciones de código que NO son resultados de dispositivo

| Hallazgo | Evidencia y riesgo | Tratamiento |
| --- | --- | --- |
| Borrado ajeno en Android 10 requiere revisión prioritaria | En la base leída, `data/MusicRepository.kt`, `deleteSongs`, recoge permisos recuperables y llama a `MediaStore.createDeleteRequest` con puerta `SDK_INT >= 29`, capturando el fallo y devolviendo `null`. La API oficial se añadió en **API 30**, no 29; el comentario del código que dice 29 no es correcto.[9] | Posible ausencia de consentimiento/éxito aparente en API 29. No corregido en esta tarea. Reproducir Q18 y revisar implementación/estado antes de declarar aprobación |
| Política completa y contacto pendientes | `SettingsAboutContent` ofrece resumen de servicios, no el texto íntegro ni URL; no se ha autorizado añadir correo | Q36 no puede cerrarse por esta documentación. Resolver mecanismo real y acceso completo antes del envío |
| Descargas dependen del proceso | `DownloadManager` usa un ámbito de corrutinas y restaura cola en pausa | Q22 verifica recuperación honesta; no exigir ni anunciar transferencia continua tras muerte del proceso |
| Consultas de datos no solo manuales | `MainViewModel` inicia enriquecimiento, reparación, letras y resolución anticipada | Q26–Q30/Q37 deben alimentar `DATA_SAFETY.md`; no cerrar por «no hay servidor propio» |

## 5. Pruebas de Play y registro de cierre

La exigencia de una prueba cerrada con **al menos 12 personas inscritas de forma continua durante los 14 días anteriores a la solicitud** corresponde a cuentas de desarrollador **personales creadas después del 13 de noviembre de 2023**, conforme a la documentación consultada. No se presume que esta cuenta cumpla ese supuesto. Si corresponde, satisfacer ese requisito permite solicitar acceso a producción, no garantiza su aprobación.[3]

| Área | Estado actual | Condición para cerrar |
| --- | --- | --- |
| Tipo y fecha de la cuenta | Pendiente de comprobar | Evidencia del titular/consola; no adivinar si es personal, organización o su fecha |
| Requisito 12 personas / 14 días | Aplicabilidad no determinada; no ejecutado | Solo si aplica el supuesto anterior: prueba cerrada real, participación continua, registro de comentarios y correcciones; no inventar participantes ni días |
| Instalación derivada de AAB y firma | No ejecutada en este guion | Registrar artefacto instalado y validación por responsable de compilación/distribución |
| Informe previo al lanzamiento de Play | No solicitado ni revisado aquí | Revisarlo si se autoriza la subida a un canal; no sustituye Bluetooth, llamadas ni pruebas físicas |
| Matriz física y casos Q01–Q40 | No ejecutados | Evidencias por caso y familia; no aplica debe justificar alcance, Bloqueado requiere resolución |
| Privacidad y Seguridad de los datos | Borradores pendientes | Cerrar las decisiones y evidencias de `DATA_SAFETY.md`; contrastar política, ficha y comportamiento |
| Licencias y derechos; punto 1 | Pendiente, fuera de resolución de este guion | Revisión separada del titular; una prueba funcional o un aviso de uso responsable no acreditan autorización |
| Publicación | No realizada ni autorizada aquí | Requiere decisión posterior explícita; no es consecuencia automática de pasar las pruebas |

**Criterio de salida propuesto:** ningún cierre inesperado reproducible, pérdida/corrupción de datos, borrado no consentido, exposición de datos no descrita ni bloqueo de reproducción/controles esenciales. Un Fallido o Bloqueado en permisos, borrado, privacidad, integridad, instalación o casos esenciales de API 36 impide recomendar el envío hasta corregirlo y repetir la prueba. El resto de incidencias debe tener severidad, decisión y riesgo aceptado por el titular, nunca un aprobado ficticio. Conservar cobertura faltante de Android de forma explícita.

Registro final por completar tras ejecución: artefacto real, dispositivos, casos aprobados/fallidos/bloqueados/no aplicables, incidencias y evidencia, persona revisora y decisión del titular. **Preparar este guion no valida QA ni autoriza una publicación.**

## Fuentes

[3] https://support.google.com/googleplay/android-developer/answer/14151465?hl=es — Pruebas de cuentas personales nuevas
[5] https://developer.android.com/develop/ui/compose/notifications/notification-permission?hl=es-419 — Permiso de notificaciones
[7] https://developer.android.com/about/versions/16/behavior-changes-16?hl=es-419 — Cambios de comportamiento al orientar a Android 16
[9] https://developer.android.com/reference/android/provider/MediaStore — Referencia de MediaStore: consentimiento de borrado

# Melodia — brief corto de landing

Base: [contexto de producto](../../.agents/product-marketing.md). Revisión: 2026-11-05. Alcance: orientar la construcción actual; no abrir un plan anual ni bloquear el diseño por investigación pendiente.

## Objetivo y audiencia

Convertir tráfico orgánico de TikTok y Reels en descarga del APK de la beta pública **PREVIEW**. Prioridad: personas con Android y música guardada que quieren organizarla y escucharla a su manera. Es una hipótesis inicial, no un segmento validado con entrevistas.

**Una acción de conversión:** «Descargar para Android». Repetir el mismo botón y destino, sin registro, correo, lista de espera, planes ni insignia de Google Play.

## Hero de referencia

**Antetítulo:** Melodia · Reproductor para Android

**H1, dos líneas:**
> Tu música.
> A tu manera.

**Subtexto:** Tu biblioteca, tus listas y tu sonido. Un reproductor para Android que pone la música primero.

**CTA:** Descargar para Android

**Microcopy:** Beta pública PREVIEW · APK firmado · Android 7.0 o posterior

**Respaldo:** Sin anuncios. Sin cuenta de Melodia.

**Destino implementado:** `https://vnic-cesar.tailb6ee43.ts.net/melodia/descargas/melodia-7.33.apk`. Es el APK `release` firmado, no `perf` ni `debug`: 7.33 / 100, minSdk 24, firma validada con `apksigner`. La descarga y su hash se contrastaron en la URL real; las pruebas no demuestran instalación ni primera escucha en un teléfono.

## Dirección visual y movimiento

Editorial negro y verde **#2EE56B**; Manrope autoalojada. Tipografía protagonista, espacios amplios y una prueba visual central, no una cuadrícula genérica de tarjetas. El verde guía la acción y los estados del reproductor.

- Demo funcional con audio original. Rótulo visible: **«Demostración web · No es una captura de la app Android»**.
- Controles de reproducción reales; nada de audio automático. No insinuar que el demo reproduce todo el producto Android ni un catálogo de terceros.
- Entrada breve del hero, feedback al pulsar y movimiento de reproducción solo cuando aporta. Respetar reducción de movimiento, teclado y foco visible; no ocultar texto esencial detrás de animaciones.
- Si aparecen pantallas Android, deben ser capturas reales de la versión revisada y contenido propio o autorizado. No usar reseñas ni cifras para llenar el espacio.

## Recorrido y copy de secciones

1. **Hero + demo:** promesa emocional, categoría Android y descarga visibles; la prueba interactiva se reconoce como web.
2. **«Tu biblioteca vuelve a sonar.»** — «Explora canciones, carpetas, álbumes y artistas. Escucha tus archivos, también sin conexión.»
3. **«Decide qué sigue.»** — «Marca favoritos, prepara la cola y guarda tus listas.» La cola descrita es local, no una mezcla universal con streams.
4. **«Encuentra tu sonido.»** — «Ajusta el ecualizador de cinco bandas y el refuerzo de graves, según tu dispositivo.» Apoyo: «Letras de LRCLIB, sincronizadas cuando están disponibles.»
5. **Instalación sin sorpresas:** «Descarga el APK. Autoriza su instalación cuando Android lo solicite. Abre Melodia, permite el acceso al audio y elige una canción local.» Advertir sobre versiones de pruebas con otra firma y respaldo antes de desinstalar.
6. **FAQ:** usar las seis respuestas del contexto: instalación; cuenta/anuncios; uso sin internet y consultas externas; biblioteca/listas; letras/EQ; importación/descargas autorizadas.
7. **Cierre:** «Tu próxima canción la eliges tú.» + el mismo CTA. Nota: «Las funciones en línea dependen de servicios externos. Usa solo contenido propio o autorizado y respeta sus condiciones.» Enlace de privacidad solo a una URL real verificada, nunca un correo inventado.

No convertir YouTube o las descargas en la promesa principal. Existen, pero la importación de listas encola descargas, no sincroniza cuentas ni migra Spotify. Disponibilidad, derechos y máximo de 100 temas por colección deben explicarse donde corresponda, no como «música ilimitada».

## Entrada orgánica y medición inicial

Tres piezas demostrables: biblioteca local sin conexión; cola → lista; ajustes de sonido/letra disponible. Una tarea por video, grabación Android real o demo web claramente rotulado, audio propio/autorizado y enlace del perfil a esta landing.

Medición propuesta: origen y pieza con UTM, visitas y clics reales de descarga si se habilita instrumentación. No declarar que la analítica ya existe, que un clic es una instalación o que hay usuarios activos. Primera reproducción: comprobar mediante pruebas voluntarias hasta contar con evidencia; no inventar porcentajes de conversión.

## Límites y aceptación de copy

- Usar «biblioteca local sin conexión», no «la app nunca se conecta». Hay consultas automáticas a LRCLIB, Apple y YouTube; Android puede respaldar datos.
- No publicar métricas, testimonios, correo, «100 % privado», licencias resueltas, compatibilidad universal o disponibilidad en Play.
- Fuentes prioritarias: código/artefacto actual y `docs/store/STORE_LISTING.md`; el README y la versión de `docs/store/RELEASE.md` están desactualizados. Evidencias con rutas y líneas en el contexto.
- Listo para implementar: hero exacto, un único objetivo, demo rotulado, seis FAQ, alcance PREVIEW y límites claros. Verificar enlace real y artefacto al publicar; no modificar Android para cumplir el diseño.

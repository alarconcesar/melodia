# Melodia

Reproductor de música para Android con biblioteca local, listas, ecualizador y
funciones en línea. Construido con Kotlin, Jetpack Compose y Media3, con
inyección de dependencias manual.

**Estado: beta.** No está publicado en Google Play. La distribución es directa,
como APK firmado en [Releases](https://github.com/alarconcesar/melodia/releases)
y el sitio del proyecto es <https://alarconcesar.github.io/melodia/>.

- minSdk 24 (Android 7.0) · targetSdk 36
- Kotlin 1.9.22 · Compose BOM 2024.01.00 · Media3 1.2.1
- AGP 8.10.1 · Gradle 8.11.1 · JDK 17

## Funciones

| Área | Detalle |
| --- | --- |
| Biblioteca | Archivos de audio del dispositivo vía MediaStore; búsqueda, orden y filtros |
| Explorar | Carpetas, álbumes (nombre + artista) y artistas |
| Listas y favoritos | Crear, renombrar, ordenar y guardar la cola como lista |
| Reproductor | Play/pausa, anterior/siguiente, aleatorio, repetición, búsqueda en la pista, MediaSession |
| Cola | Reordenar, quitar y vaciar conservando el tema actual; se restaura al reabrir |
| Sonido | Ecualizador de cinco bandas y refuerzo de graves, según el dispositivo; velocidad y tono |
| Letras | Desde LRCLIB cuando existen, con sincronización si el texto trae tiempos |
| Carátulas | MediaStore o portada incrustada primero; iTunes solo como respaldo |
| En línea | Búsqueda en YouTube, reproducción y descargas gestionadas por cola |
| Sistema | Servicio en primer plano, controles en notificación y pantalla bloqueada, widget y temporizador |

## Requisitos para compilar

```bash
export ANDROID_HOME=$HOME/Android        # platforms;android-36 + build-tools;35.0.0
git clone https://github.com/alarconcesar/melodia.git
cd melodia
./gradlew assembleDebug                  # APK de depuración
./gradlew testDebugUnitTest              # pruebas JVM (Robolectric)
./gradlew assembleRelease                # requiere keystore.properties
```

`keystore.properties` no está en el repositorio y contiene la ruta y las claves
de firma. Sin ese archivo, `assembleRelease` produce un APK sin firmar; usa
`assembleDebug` o `assemblePerf` para probar.

> El tema debe heredar de `Theme.AppCompat.*` y la dependencia
> `androidx.appcompat:appcompat` debe estar presente, o la app abre en negro.

## Estructura

```text
app/src/main/java/com/melodia/app/
├── AppContainer.kt          # inyección manual
├── MainViewModel.kt         # estado de la interfaz y sincronía con ExoPlayer
├── data/                    # MediaStore, Room, descargas, letras, carátulas
├── player/                  # ExoPlayer, ecualizador, cola persistente
├── ui/                      # tema, componentes y pantallas Compose
└── widget/                  # widget y política de acciones
web/                         # sitio del proyecto (Vite, estático)
docs/                        # sistema de diseño, arquitectura y notas
```

Más detalle en [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) y
[`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md).

## Privacidad

Melodia no tiene cuentas ni servidores propios, pero **sí se conecta a servicios
externos**: YouTube, LRCLIB e iTunes, en algunos casos de forma automática. La
política completa está en
<https://alarconcesar.github.io/melodia/privacidad/> y el texto fuente en
[`docs/store/PRIVACY.md`](docs/store/PRIVACY.md).

## Uso responsable del contenido

Usa únicamente audio propio o para el que tengas permisos, licencias o
autorizaciones. Las funciones de búsqueda y descarga no conceden derechos sobre
el contenido ni autorizan a eludir restricciones de terceros. Melodia no es una
aplicación oficial de YouTube, LRCLIB ni Apple.

## Contribuir

Antes de abrir un cambio, revisa los problemas abiertos y describe el caso que
quieres resolver. Para reportar un fallo, indica versión de Android, modelo de
teléfono, versión de Melodia y los pasos para reproducirlo. No adjuntes audios,
rutas personales ni datos de tu biblioteca.

## Licencia

GNU General Public License, versión 3. Ver [`LICENSE`](LICENSE).

Melodia incluye [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor),
distribuido bajo GPLv3. Al redistribuir la app debes cumplir la licencia y ofrecer
el código fuente correspondiente.

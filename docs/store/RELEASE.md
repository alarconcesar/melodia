# Melodia — Estado de preparación de la publicación

Revisión: 5 de noviembre de 2026. Rama `publicacion-20261105`, versión **7.32 (versionCode 99)**.
Base de partida: `1f91a7e` (7.27 / 94); la preparación de publicación entró en 7.28 (95) 7.29 retira
la medición de rendimiento y toda mención a la política, y 7.30–7.31 rehacen el icono con el glifo Material centrado por masa.

**Preparado, no enviado.** Este documento registra qué está verificado y qué falta. No autoriza subir
la app a Google Play ni sustituye las pruebas en un teléfono real.

## 1. Cadena de compilación

| Elemento | Antes | Ahora |
| --- | --- | --- |
| Android Gradle Plugin | 8.2.2 | **8.10.1** |
| Gradle | 8.5 | **8.11.1** (con suma SHA-256 oficial) |
| compileSdk / targetSdk | 35 / 35 | **36 / 36** |
| minSdk | 24 | 24 (sin cambios) |
| JDK | 17 | 17 |

- SDK 36 instalado en `/home/ubuntu/Android` con `sdkmanager`, sin `apt` ni cambios del sistema.
- AGP 8.10.1 exige Gradle ≥ 8.11.1 y es la rama oficial compatible con API 36; no se usó
  `suppressUnsupportedCompileSdk`.
- Lint vuelve a ser una compuerta real (`checkReleaseBuilds` y `abortOnError` en `true`): **0 errores**,
  87 avisos y 11 sugerencias, ninguna bloqueante (el grueso son `UseKtx`, versiones de dependencia y
  `IconLauncherShape` sobre el icono adaptativo existente).

Comando de verificación completa:

```bash
ANDROID_HOME=/home/ubuntu/Android ./gradlew \
  testDebugUnitTest lintRelease assemblePerf assembleRelease bundleRelease --console=plain
```

Resultado del build final: **BUILD SUCCESSFUL**, **203 pruebas, 0 fallos, 0 errores, 0 omitidas**.

## 2. Firma y artefactos

| Artefacto | Ruta | Firma | Uso |
| --- | --- | --- | --- |
| APK de pruebas | `app/build/outputs/apk/perf/app-perf.apk` | clave de depuración (la misma de las entregas anteriores) | instalar encima de la versión que ya tiene César |
| APK de publicación | `app/build/outputs/apk/release/app-release.apk` | **clave de publicación existente** | verificación local; Play usa el AAB |
| App Bundle | `app/build/outputs/bundle/release/app-release.aab` | **clave de publicación existente** | subida a Play Console |

- Se reutilizó la llave existente; **no se generó ni reemplazó ninguna** y `applicationId` sigue siendo
  `com.melodia.app`. `keystore.properties` y `*.jks` están ignorados por git y no entran en el repo.
- `apksigner verify` pasa en perf y release; el AAB valida con `bundletool 1.18.3 validate` (JAR
  descargado del release oficial con SHA-256 comprobado) y su firma JAR verifica con `jarsigner`.
- Manifest del AAB comprobado: `com.melodia.app`, `versionCode 99`, `versionName 7.32`, `minSdk 24`,
  `targetSdk 36`, `debuggable=false`.
- **APK/AAB no contienen código nativo** (`.so` = 0), por lo que no aplica el problema de empaquetado
  de 16 KB; igual conviene probar en un entorno de 16 KB antes de dar por cerrado ese punto.
- Respaldo privado de la llave y de la configuración de nginx anterior: cifrado en reposo y subido a
  `gdrive:backups/melodia/2026-11-05-7.28/`; se descargó y descifró desde Drive para comprobar el
  contenido. No quedan copias en claro temporales.

> El APK release **no** se instala encima del perf (firma distinta): para probarlo hay que desinstalar
> antes. El perf conserva la firma de depuración a propósito, para que César actualice sin perder datos.

## 3. Privacidad y declaraciones

- `docs/store/PRIVACY.md` es la **fuente única**: de ella se genera el recurso de la app
  (`res/raw/privacy_policy.txt`, tarea `generatePrivacyPolicy`) y el sitio público
  (`tools/store_site.py`). No hay dos textos que puedan divergir.
- La política describe datos locales, consultas externas (YouTube/NewPipe, LRCLIB, iTunes), dirección
  IP, consultas automáticas, retención desconocida de terceros, borrado, derechos y respaldos de
  Android. **No promete que «ningún dato sale»** ni que borrar la app elimina copias externas.
- Dentro de la app **no hay ninguna mención a la política de privacidad**: ni enlace, ni hoja, ni
  texto embebido. Decisión del titular (5 de noviembre de 2026). El texto vive en el sitio público y
  la URL se declara únicamente en la ficha de Play Console, que es donde la tienda la exige.
- URL pública: <https://vnic-cesar.tailb6ee43.ts.net/melodia/privacidad/>
  - Verificada por HTTPS real contra las dos IP de ingreso: 200, TLS válido, contenido idéntico al
    build local (SHA-256).
  - El panel del hub sigue cerrado (401); solo se publicó la carpeta de la política.
  - Sin scripts, sin formularios, sin `mailto`, `noindex`.
- `docs/store/DATA_SAFETY.md` queda **borrador deliberadamente incompleto**: separa tratamiento local,
  terceros, retención desconocida y decisiones pendientes. No se declaró «no recopila» por no tener
  backend propio.
- `docs/store/STORE_LISTING.md` está íntegramente en español, sin prometer descarga libre de contenido
  protegido y sin inventar una edad IARC. Licencias y derechos siguen **pendientes**; YouTube se
  conserva (punto 1 fuera de este alcance).

## 4. Recursos de tienda

- `docs/store/icon-512.png` — icono existente, sin rediseño; además quedó enlazado en el manifest
  (`android:icon` y `android:roundIcon`), que antes no lo declaraba.
- `docs/store/feature-graphic.png` — 1024×500 RGB, derivado del material original archivado en
  `docs/store/sources/feature-graphic-original.png`.
- **Faltan capturas reales** de la versión candidata en un teléfono. No se fabricaron maquetas.

## 5. QA

- `docs/store/QA_DEVICE_CHECKLIST.md`: matriz API 24–36 con **40 casos**, criterios de fallo y tabla de
  cierre. Todos figuran como «No ejecutado»: es un guion, no un resultado.
- Cobertura automatizada nueva de esta pasada:
  - `DeleteConsentTest` (4): consentimiento por URI en Android 10 y por lote en 11+.
  - `PublicationPolicyTest` (9): límite de 2000 URI por solicitud y rutas del widget.
  - `PrivacyPolicyTest` se retiró junto con la hoja y el recurso: la app ya no contiene la palabra
  «privacidad» ni el texto de la política.
- `SettingsPolishTest` conserva el caso de «Acerca de», que sí informa de servicios externos y
  consultas automáticas (no es la política).
- Correcciones de comportamiento incluidas:
  - Borrado en Android 10 usa `RecoverableSecurityException`; `createDeleteRequest` solo desde API 30
    (antes se llamaba desde 29 y podía devolver un éxito falso).
  - Lotes de borrado de máximo 2000 URI y reintento de los pendientes tras cada consentimiento.
  - El widget ya no arranca el servicio de reproducción solo para un temporizador: política pura
    (`WidgetActionPolicy`) que decide entre servicio, dormir, cancelar o abrir la app.
  - Fallo de borrado ya no se presenta como éxito: aviso y se conserva la selección.
- **Sin prueba en dispositivo real**: ni instalación, ni Bluetooth, ni llamadas, ni notificaciones, ni
  rotación con fuente grande. Nada de eso se declara aprobado.

## 6. Pendientes antes de enviar a Play

1. **Contacto público**: César indicó «aún no pongas ningún correo». Sin un contacto real, la política
   y la ficha quedan incompletas; no se puso ningún marcador ficticio.
2. **Pruebas físicas** Q01–Q40 sobre el artefacto candidato, con evidencia por caso.
3. **Seguridad de los datos**: cerrar cada categoría de `DATA_SAFETY.md` contra el artefacto real.
4. **IARC y audiencia objetivo**: responder el cuestionario; no se anticipa edad.
5. **Licencias y derechos**: inventario de dependencias (incluida NewPipeExtractor) y revisión de las
   condiciones de YouTube, LRCLIB y Apple. El punto 1 sigue sin resolver.
6. **Capturas de tienda** reales y revisión de derechos de las imágenes.
7. **Cuenta de desarrollador**: comprobar tipo y fecha; el requisito de 12 personas durante 14 días
   solo aplica a cuentas personales creadas después del 13/11/2023.

Ningún pendiente de esta lista se cierra por haber compilado correctamente.

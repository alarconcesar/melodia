# Publicación de Melodia en GitHub (open source + Pages + Releases)

**Estado: ejecutado el 6-nov-2026.** Repositorio público en <https://github.com/alarconcesar/melodia>, sitio en <https://alarconcesar.github.io/melodia/> y descarga en <https://github.com/alarconcesar/melodia/releases/tag/v7.33> (marcado como prerelease). Licencia GPLv3. Verificado por HTTPS: página, política, condiciones, robots, sitemap, tarjeta social y APK con SHA-256 idéntico al firmado. QA de 101 comprobaciones sin fallos en ocho anchos, servicio y descarga incluidos.

Decisión de César (6-nov-2026): el código será abierto en `alarconcesar/melodia`, la landing vivirá en GitHub Pages y el APK se descargará desde GitHub Releases. **No se publica en Google Play.** Este documento es el plan de ejecución; nada de esto está ejecutado todavía salvo donde se indica «verificado».

## Sí se puede, con dos condiciones

GitHub Pages publica sitios estáticos desde un repositorio, y Pages está disponible en repositorios públicos con GitHub Free.[1] Un APK es un archivo adjunto de un release y el límite por archivo es 2 GiB, muy por encima de los 2.8 MB actuales.[2] La landing que ya existe es un build estático de Vite, así que encaja sin cambios de arquitectura.

Condiciones reales:

1. **Licencia.** El repo no tiene `LICENSE` y una dependencia lo obliga: NewPipeExtractor se distribuye bajo GNU GPL versión 3.[3] Publicar el APK enlazando esa biblioteca sin ofrecer el código correspondiente bajo GPL es el escenario que la propia licencia prohíbe. Hacer el repositorio público resuelve la parte de «código disponible», pero falta declarar la licencia del proyecto. Recomendación: **GPLv3 para todo el repositorio**, que es la opción simple y compatible; una licencia permisiva (MIT/Apache) sobre el conjunto chocaría con la dependencia.
2. **Alcance de lo que se distribuye.** La app permite buscar y descargar audio de YouTube. Que no esté en Play no cambia las condiciones de ese servicio, y las políticas de uso aceptable de GitHub prohíben contenido o actividad que infrinja derechos de propiedad intelectual.[5] No es un bloqueo técnico ni una decisión que yo pueda tomar: conviene que la descripción del repo y la app dejen claro el uso con contenido propio o autorizado, y que la función no se presente como catálogo ni como descarga libre. Si el proyecto crece, es el punto que más riesgo concentra.

## Estado verificado del repositorio hoy

- `alarconcesar/melodia` es **privado**, sin licencia declarada y sin releases ni Pages.[gh]
- La rama `main` remota está en `e827ff0` (versión 4.3). Hay **65 commits locales sin subir**; el trabajo actual (7.33, landing y marketing) todavía no está en GitHub.
- Historial sin credenciales: ningún `.env`, `keystore.properties`, `local.properties`, `.jks` ni `.pem` en archivos rastreados ni en todo el historial. `keystore.properties` está ignorado y la llave de firma vive fuera del repo.
- Tamaño del historial: 3.1 MB de contenido, 8.2 MB de `.git`. No hay archivos cerca del límite de GitHub.
- El APK de descarga está ignorado por git: se adjunta al release, no se versiona en el árbol.

## Cambios técnicos necesarios para Pages

1. **Rutas base.** `web/vite.config.js` ya usa `base: '/melodia/'`, que coincide con `https://alarconcesar.github.io/melodia/`. Los enlaces internos absolutos siguen funcionando.
2. **Política de privacidad dentro del build.** Hoy `/melodia/privacidad/` la sirve nginx desde `app/build/store-site`, fuera de `dist/`. En Pages no existe nginx: hay que generar ese HTML con `tools/store_site.py` y copiarlo a `web/public/privacidad/` antes de construir, con enlace en el pie y en la FAQ.
3. **Descarga.** Apuntar el botón al asset del release: `https://github.com/alarconcesar/melodia/releases/download/v7.33/melodia-7.33.apk`. No usar `/releases/latest` si la beta se marca como prerelease, porque puede resolver a otra versión. `version.json` con el hash sigue siendo válido y conviene publicarlo junto al APK.
4. **Metadatos.** Quitar `noindex` de `index.html` y de `robots.txt`, apuntar canonical, `og:url`, `og:image` y el JSON-LD al dominio de Pages, y actualizar el `sitemap.xml`. Los archivos de `condiciones/` y `404.html` ya están preparados como estáticos.
5. **Publicación.** Workflow de GitHub Actions que construye `web/` y publica `dist/` en Pages, o una rama `gh-pages` con el build. Recomiendo Actions para que el sitio salga siempre del código y no de un `dist/` commiteado a mano.
6. **Release.** Tag `v7.33` sobre el commit de la versión, con el APK release firmado, su SHA-256 y notas de cambios. Marcar como prerelease si sigue siendo beta.
7. **README público.** El actual dice «sin tracking», minSdk 26 y targetSdk 34; el APK real es 24/36 y la app sí hace consultas externas. Antes de abrir el repo hay que corregirlo, añadir requisitos de compilación, estado del proyecto, licencia y cómo reportar problemas.

## Lo que ya no hace falta

Play Console, cuenta de desarrollador, Seguridad de los datos, cuestionario IARC, capturas de tienda y la revisión de ficha de Play. Ese trabajo queda en pausa; los documentos de `docs/store/` pueden quedarse como referencia histórica, pero dejan de ser el camino de publicación.

## Sigue pendiente y no lo resuelve GitHub

- Pruebas en un teléfono real de la versión candidata.
- Contacto público del proyecto (César pidió no inventar correo; un repo público necesita un canal de soporte, aunque sea Issues).
- Licencias y condiciones de los servicios de terceros, y el aviso de uso responsable dentro de la app.
- Decisión sobre el nombre del repo, la descripción y si el proyecto se presenta como beta.

## Orden ejecutado

1. Licencia GPLv3 en `LICENSE`, detectada por GitHub como `gpl-3.0`.
2. README corregido y publicado; descripción y temas del repositorio actualizados.
3. Landing adaptada: política dentro del build, descarga a Releases y metadatos por modo (`.env` para el preview, `.env.pages` para Pages).
4. Build de Pages probado en local simulando la ruta `/melodia/`.
5. Historial completo subido: `main` en `b1050c3`, remoto verificado con `git ls-remote`.
6. Pages activado con `build_type=workflow`; el workflow construyó y desplegó sin errores.
7. Release `v7.33` publicado como prerelease con `melodia-7.33.apk` y `version.json`.
8. QA de 101 comprobaciones sobre la URL pública, sin fallos.

Pendiente después de esta entrega: pruebas en teléfono real, canal de soporte (Issues), y revisar el uso de contenido de terceros antes de difundir el proyecto.

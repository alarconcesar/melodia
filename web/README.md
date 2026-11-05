# Melodia · sitio de marketing

Fuente del sitio en `/home/ubuntu/projects/melodia/web`. Rama inicial `marketing-landing-20261105`. No modifica ni recompila la app Android.

## Dirección

Landing de una app de música para oyentes que ya tienen una biblioteca y quieren escucharla sin anuncios ni crear otra cuenta. Negro plano, verde de marca `#2EE56B`, tipografía Manrope autoalojada, composición editorial asimétrica. Diales: variación 8, movimiento 6, densidad 3. No es una plantilla SaaS, ni representa una app publicada en Play.

## Estructura y mensaje (antes del código)

1. Hero dividido: **Tu música. / A tu manera.** Subtítulo: Tu biblioteca, tus listas y tu sonido. Un reproductor para Android que pone la música primero. CTA: **Descargar para Android**.
2. Franja de hechos: sin anuncios, sin cuenta de Melodia, biblioteca local sin conexión. Son características, no cifras de adopción.
3. Manifiesto que revela palabras al entrar: **Las canciones cambian. / Tu forma de escucharlas la eliges tú.**
4. Beneficios con demostración interactiva: biblioteca organizada; letras cuando existen; ecualizador de cinco bandas compatible por equipo; control de la cola. Sin prometer catálogos ni permisos ajenos.
5. Imagen editorial de un momento de escucha y declaración de marca.
6. Instalación en tres pasos: descargar APK, permitir instalar desde ese navegador, conceder acceso al audio y reproducir.
7. Seis FAQ: plataforma, conexión, anuncios/cuenta, instalación, letras, YouTube. Respuestas apoyadas en el código y la ficha, no testimonios ficticios.
8. Descarga de versión de prueba firmada y cierre con el mismo CTA. Indicar APK externo a Play; no atribuir resultados de instalación física aún no ejecutados.
9. Pie: privacidad pública existente, condiciones de la demostración y uso responsable. Sin correo ni perfiles sociales inventados.

## Visuales y movimiento

- Arte original generado para carátula y foto editorial; no generar imágenes que finjan ser capturas de Android.
- Reproductor web realmente interactivo con un fragmento instrumental sintetizado original. Rotulado demostración web, no captura de la app. Nunca autoplay con sonido.
- Entradas de jerarquía con transform/opacity y curva `cubic-bezier(.23,1,.32,1)`; revelado de palabras mediante IntersectionObserver; transiciones de reproducción y pestañas; inclinación al puntero solo en ratón fino.
- Menú con diálogo nativo y foco accesible; FAQ con details/summary; controles de mínimo 44 px. Movimiento reducido y navegación sin JS conservan todo el contenido y la descarga.
- Sin secuestro de scroll, librería pesada de motion ni loops de página entera. Audio se pausa al ocultar la pestaña.

## SEO y publicación

Preview con `noindex, nofollow`: no indexar el hostname de infraestructura ni anunciar un lanzamiento. Preparar title, descripción, canonical, tarjetas OG y JSON-LD honestos. La publicación comercial, dominio, red social, presupuesto y correo siguen siendo decisiones de César.

## Estado de la entrega

- Vista previa pública: https://vnic-cesar.tailb6ee43.ts.net/melodia/
- Verificación real: `qa/report.json` (101 comprobaciones sin fallos sobre HTTPS en ocho anchos entre 320 y 1920 px). `qa/public-files.json` compara los 32 archivos servidos con dist.
- APK release 7.33/100 firmado, con hash y tamaño de descarga contrastados desde el navegador. No es la firma perf de las pruebas internas; la web avisa de la diferencia.
- Esta entrega no modifica Android, no publica en Google Play, no crea correos ni activa anuncios o píxeles.
- Publicar: `sudo -n /home/ubuntu/.hermes/hermes-agent/venv/bin/python scripts/deploy.py`. Valida nginx, conserva privacidad y protege el hub.
- La medición de instalaciones, las capturas Android reales, el dominio definitivo y la revisión de licencias del APK quedan separados del QA de la landing. El evento local de descarga no es analítica activa.

## Desarrollo

```sh
cd /home/ubuntu/projects/melodia/web
npm ci
npm run build
npm run qa
```

Build estático Vite con base `/melodia/`. Los binarios Android no entran al git; el empaquetado toma únicamente el APK release ya firmado y verificado. Nunca se sirven credenciales ni el árbol Android. QA y despliegue están en `scripts/`.

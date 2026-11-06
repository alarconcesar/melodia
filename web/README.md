# Melodia · sitio de marketing

Fuente: `/home/ubuntu/projects/melodia/web`. No modifica ni recompila la app Android.

- **Sitio oficial:** https://alarconcesar.github.io/melodia/
- **Código y APK:** https://github.com/alarconcesar/melodia y GitHub Releases.
- **Preview del VPS:** https://vnic-cesar.tailb6ee43.ts.net/melodia/ (no indexable).

## Dirección y mensaje

La prioridad de César es destacar **la descarga gratuita de música y la escucha sin conexión**, no solamente que el APK sea gratis. El H1 de marca aprobado se conserva: destacar un beneficio no autoriza a reemplazarlo. La descarga gratuita vive en el subtítulo y la sección dedicada. El copy vigente tiene una sola fuente: `index.html`. Usar contenido propio o cuya descarga esté autorizada; no prometer catálogos, permisos sobre música ajena, disponibilidad universal ni elusión de pagos.

Se conserva el diseño aprobado: negro, verde `#2EE56B`, Manrope autoalojada, composición editorial asimétrica y animaciones accesibles. No hay anuncios, píxeles, métricas de adopción, testimonios, correo ni perfiles sociales inventados.

El recorrido presenta el beneficio en la primera pantalla, una franja de hechos y una sección de búsqueda → descarga → escucha sin conexión; después, biblioteca, sonido, instalación, FAQ y descarga del APK. El CTA de conversión se mantiene: **Descargar para Android**.

## Visuales y movimiento

- Arte y fragmentos musicales originales; procedencia en `assets-provenance.json`.
- El reproductor web funciona y está rotulado como demostración, no como captura de Android. Nunca autoplay con sonido.
- Entradas y revelados progresivos mediante transform/opacity e IntersectionObserver; movimiento reducido conserva el contenido. No secuestrar el scroll.
- Menú con diálogo nativo, foco accesible y ajuste para pantallas de poca altura. Enlaces y controles con áreas de toque de al menos 44 × 44 px.
- El contenido y la descarga permanecen disponibles sin JavaScript. El audio se pausa al ocultar la pestaña.

## Desarrollo y publicación

```sh
cd /home/ubuntu/projects/melodia/web
npm ci
npm run dev
npm run build:pages
```

- Base estática Vite: `/melodia/`.
- `.env.pages` define el origen oficial y el APK de Releases; `.env` conserva el modo de preview local.
- `npm run build:pages` genera también la política de privacidad, robots y sitemap del origen oficial. No usar Vite directamente si se necesitan esos pasos previos.
- GitHub Actions publica `main` mediante `.github/workflows/pages.yml`.
- Los binarios Android, credenciales, `node_modules` y `dist` no entran a Git. El APK distribuido sigue siendo el `release` ya firmado; la web no cambia su firma ni su versión.
- Para sincronizar el preview, reutilizar `scripts/deploy.py` después del build y QA: copia únicamente `dist`, valida nginx y conserva política y autenticación del hub. Un build de Pages en el preview conserva canonical y descarga en GitHub.

## QA reproducible

```sh
MELODIA_QA_URL=https://alarconcesar.github.io/melodia/ npm run qa
MELODIA_QA_URL=https://alarconcesar.github.io/melodia/ npm run qa:mobile
```

- `scripts/qa.py`: recursos, reproducción real del audio original, descarga y hash del APK, teclado, diálogo, FAQ, reducción de movimiento y funcionamiento sin JavaScript.
- `scripts/qa-mobile.py`: 14 tamaños, desde 320 × 568 hasta 1920 × 1080, incluyendo móviles cortos, horizontal y tablets. Verifica geometría, dos líneas del titular, CTA inicial, texto sin recortes, áreas de toque, todas las FAQ, navegación táctil, audio y descarga con SHA-256.
- Resultados y cantidades reales: `qa/report.json` y `qa/mobile-report.json`; leer sus URL y fallos, no reutilizar totales de ejecuciones anteriores.
- `MELODIA_QA_OUTPUT` permite guardar las verificaciones de producción en `qa/runtime/` sin alterar los informes versionados. Las capturas también se guardan allí.
- Estos resultados son de Chromium en navegador, **no de un teléfono físico ni de la app Android**. Una descarga web no demuestra instalación o primera escucha en Android. El caso `root-font-20` comprueba la raíz CSS, no equivale a zoom ni aumento de texto definido en píxeles.

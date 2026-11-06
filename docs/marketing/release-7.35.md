## Melodia 7.35 · beta

Corrección del gesto de deslizar canciones y publicación del código abierto.

### Qué cambió
- **Deslizar una canción ya no parpadea.** El detector medía la distancia del dedo dentro de la fila que se movía, le restaba su propio desplazamiento y la fila retrocedía. Ahora el gesto se mide sobre un contenedor fijo.
- **Un gesto cancelado ya no encola ni marca favorito por error.** Se distingue la cancelación del sistema de un deslizamiento real.
- Si tocas mientras la fila vuelve, el gesto continúa desde donde está, sin saltos.

### Sigue igual
Deslizar a la derecha encola al final · a la izquierda marca favorito · mantener presionado entra en selección · el botón ⋮ abre la ficha del tema.

### Instalación
APK firmado para Android 7.0 o posterior, distribuido desde GitHub Releases (no desde Google Play). La firma `release` es distinta de las compilaciones internas de prueba: si vienes de una de ellas, respalda listas y favoritos antes de desinstalar.

### Verificación
- 209 pruebas automáticas, 0 fallos; lint sin errores.
- 6 pruebas nuevas del gesto, incluida la competencia con el scroll de una lista.
- Las pruebas son de navegador/JVM: no sustituyen una prueba en un teléfono físico.

Código bajo GPLv3 en https://github.com/alarconcesar/melodia

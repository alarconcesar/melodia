# Política de privacidad — Melodia

Última actualización: 5 de noviembre de 2026.

## Alcance

Melodia es un reproductor de audio para Android con biblioteca local y funciones en línea. Funciona sin crear una cuenta de Melodia y no integra anuncios. La versión revisada no incorpora un servidor propio de usuarios ni un servicio de analítica o envío automático de fallos al desarrollador. Esto **no significa que ningún dato salga del dispositivo**: sus funciones se conectan a servicios externos y Android puede realizar copias de seguridad.

## Datos y finalidades

- **Biblioteca de audio:** Melodia accede, con el permiso de Android, a los audios disponibles y sus metadatos: título, artista, álbum, duración, identificadores de la biblioteca y rutas de archivo. Los utiliza para organizar, buscar y reproducir música, mostrar carátulas y permitir las operaciones de edición o borrado que solicites. No se ha implementado una subida de los archivos de audio locales para reconocer canciones; las consultas externas de metadatos descritas abajo sí pueden revelar qué canciones hay en tu biblioteca.
- **Organización y reproducción:** las listas, favoritos, cola, posición de reproducción, historial y estadísticas de escucha se guardan en una base de datos local y preferencias de la aplicación. Sirven para reanudar la escucha, organizar la biblioteca y mostrar tu actividad dentro de Melodia.
- **Preferencias:** se conservan localmente los ajustes de sonido, apariencia, carpetas, calidad y ahorro de datos.
- **Actividad en línea:** las búsquedas, enlaces e identificadores de vídeos o listas se utilizan para consultar YouTube, resolver el audio y gestionar las descargas. La app conserva búsquedas recientes, historial de escucha en línea y un registro de descargas en sus preferencias locales.
- **Cachés y descargas:** se guardan letras, carátulas, fichas de metadatos y archivos temporales de descargas para reducir consultas y permitir la reanudación. Los audios completados se guardan en el almacenamiento multimedia compartido, normalmente en Música/Melodia.
- **Diagnóstico local:** si ocurre un fallo, la app puede guardar un informe técnico local para mostrarlo al reabrir. Puede incluir mensajes de error, rutas o información de la operación que falló. No se envía automáticamente al desarrollador. Si decides copiarlo o compartirlo mediante otra aplicación, revisa antes su contenido.

## Servicios externos y datos transmitidos

| Destinatario | Datos de las solicitudes | Finalidad |
| --- | --- | --- |
| YouTube y su infraestructura de distribución, mediante la biblioteca NewPipeExtractor | Texto de búsqueda, enlaces e identificadores de vídeos o listas y solicitudes de audio o miniaturas | Búsqueda, resultados, reproducción y descargas solicitadas; resolución anticipada de enlaces de algunos resultados |
| LRCLIB | Título, artista y, cuando están disponibles y son útiles, álbum y duración; o una consulta derivada del título y artista | Obtener letras de canciones locales o en línea y guardarlas en caché |
| iTunes Search de Apple y los servidores que entregan sus imágenes | Título y artista o una consulta derivada del título; solicitudes de carátulas | Buscar carátulas y completar o reparar metadatos |

Cada conexión permite al servicio receptor y a su infraestructura conocer la dirección IP de salida de tu conexión y los datos técnicos necesarios para atenderla, como la ruta solicitada y las cabeceras de cliente. Las consultas pueden reflejar tus intereses o el contenido de tu biblioteca aunque no tengas una cuenta de Melodia.

NewPipeExtractor es una biblioteca integrada en la aplicación, no un servidor propio de Melodia. Los destinatarios de sus solicitudes son YouTube y los servidores implicados en entregar los resultados y el audio. Las letras, imágenes y audio también pueden servirse desde redes de distribución de terceros.

Los servicios externos tienen sus propias condiciones y prácticas de privacidad. No se ha verificado aquí cuánto conservan las consultas o registros de conexión ni todos sus usos posteriores. Melodia no puede garantizar su eliminación ni que el tratamiento se limite al tiempo de respuesta; no debe interpretarse que carecen de registros por el hecho de que Melodia no tenga un servidor propio.

## Consultas automáticas

No todas las conexiones requieren que pulses un botón de búsqueda en línea:

- Al cargar o recargar la biblioteca, Melodia puede buscar en iTunes carátulas que falten, después de intentar usar el arte local. Al iniciar también puede consultar iTunes para reparar metadatos de descargas anteriores.
- Al cambiar la canción actual, incluso una canción local o restaurada de la cola, puede consultar LRCLIB para cargar la letra sin que abras la pantalla de letras.
- El buscador de Inicio también consulta YouTube tras una breve pausa al escribir una consulta de al menos dos caracteres. No es un buscador exclusivamente local.
- Tras una búsqueda en Inicio o en la sección en línea, puede resolver anticipadamente los enlaces de audio de los primeros resultados, antes de que los reproduzcas.
- Al finalizar una descarga puede solicitar letras, miniaturas y metadatos y preparar la caché del tema guardado.

La versión revisada no ofrece un interruptor general que desactive todas estas consultas automáticas. El ahorro de datos ajusta la calidad del audio; **no equivale a desactivar las conexiones externas**. Para evitar esas conexiones puedes impedir el acceso a la red con los controles disponibles en tu dispositivo o usarlo sin conexión. Las funciones dependientes de red dejarán de estar disponibles y algunas consultas podrán reintentarse cuando vuelva la conexión.

## Almacenamiento, conservación y copias de seguridad

Los datos internos se guardan en el espacio privado de la app: base de datos Room, preferencias, archivos de carátulas y cachés de letras o descargas. Las cachés tienen mecanismos de poda por cantidad y Android puede eliminarlas; no existe un plazo único de conservación para todos los datos. Las listas, favoritos, historiales y preferencias permanecen hasta su eliminación, sustitución o limpieza de los datos de la app. El informe de fallo se sustituye por el siguiente fallo y puede eliminarse tras un arranque correcto o desde la pantalla de diagnóstico.

La copia de seguridad de Android está habilitada en el manifiesto de la versión revisada (`allowBackup=true`). Según la configuración, versión de Android y fabricante, datos internos como preferencias, base de datos y archivos pueden incluirse en copias de seguridad o transferirse a otro dispositivo. La caché temporal no equivale a una copia de seguridad. Estos mecanismos los gestiona Android o el proveedor de respaldo del dispositivo, no un servidor de Melodia; sus condiciones y plazos pueden diferir de los del almacenamiento local.

Si eliges crear un respaldo desde la app, se genera un archivo JSON en Descargas/Melodia que contiene listas, referencias a canciones, rutas, metadatos y favoritos, no una copia del audio. Ese archivo no tiene cifrado propio de Melodia. Puede seguir existiendo tras borrar los datos internos o desinstalar la app, y otras herramientas de copia o sincronización que utilices podrían conservarlo.

## Permisos y seguridad

- **Lectura de audio:** `READ_MEDIA_AUDIO` en Android 13 o posterior; `READ_EXTERNAL_STORAGE` hasta Android 12L. Permite leer la biblioteca. Denegarlo limita la biblioteca local.
- **Escritura en almacenamiento:** `WRITE_EXTERNAL_STORAGE` solo hasta Android 9, para guardar archivos en el almacenamiento compartido.
- **Internet:** permite consultar servicios, cargar imágenes, reproducir audio en línea y descargarlo; no tiene un diálogo de permiso independiente en Android.
- **Notificaciones:** la app solicita `POST_NOTIFICATIONS` en Android 13 o posterior para sus avisos y controles, con las reglas y exenciones de sesiones multimedia del sistema.
- **Reproducción en segundo plano:** los permisos de servicio en primer plano multimedia y de mantenimiento de actividad permiten continuar la reproducción con la pantalla apagada; el ajuste de audio permite aplicar el ecualizador y los efectos.

Las consultas directas de letras y metadatos usan direcciones HTTPS. Los enlaces de audio e imágenes proceden también de los servicios externos. HTTPS protege el transporte, no oculta tu dirección IP ni la solicitud al destinatario y no significa cifrado de extremo a extremo frente a ese destinatario. No se promete cifrado propio de la base de datos, de los respaldos exportados ni ausencia de riesgos en todas las conexiones.

## Borrado, controles y derechos

- Puedes quitar favoritos, eliminar listas o temas de ellas y borrar estadísticas o el historial de descargas mediante las funciones disponibles. Eliminar una lista o un registro de historial no elimina el archivo de audio ni los registros de terceros.
- Para borrar los datos internos de Melodia, utiliza **Ajustes de Android → Aplicaciones → Melodia → Almacenamiento → Borrar datos** o desinstala la aplicación. Esto afecta a la base de datos, preferencias y archivos internos. Borrar solo la caché no borra las listas ni todos los historiales.
- Los audios guardados en Música/Melodia y los respaldos JSON en Descargas/Melodia deben eliminarse por separado desde la app cuando exista esa operación o desde un gestor de archivos. Android puede solicitar confirmación para modificar o borrar archivos; si la deniegas, esos archivos deben permanecer.
- Revisa por separado las copias de seguridad, las transferencias y las herramientas de sincronización de tu dispositivo. Borrar datos locales o desinstalar no demuestra que se hayan eliminado copias anteriores; una restauración puede recuperar datos.
- Puedes revisar o retirar permisos desde los ajustes de Android. Retirarlos no borra lo ya almacenado ni las solicitudes que ya recibieron otros servicios.

Según la legislación aplicable, puedes tener derechos de información, acceso, rectificación, supresión, oposición, limitación o portabilidad y de retirar el consentimiento cuando sea la base del tratamiento. Las operaciones locales anteriores permiten gestionar parte de esos datos; el respaldo exportado no representa necesariamente todos ellos. Para los datos conservados por servicios externos, consulta sus mecanismos de privacidad y ejercicio de derechos. Cuando proceda, también puedes acudir a la autoridad de protección de datos competente. No se garantiza que una acción dentro de Melodia elimine información de esos terceros.

## Contenido de terceros y menores

Melodia puede acceder a contenido y metadatos de terceros, cuya disponibilidad, exactitud y adecuación no controla. No incorpora verificación de edad ni se presenta como un servicio específicamente dirigido a menores. No se afirma que los servicios externos dejen de tratar datos cuando los utiliza un menor. El uso del contenido debe respetar las autorizaciones y condiciones aplicables.

## Cambios

La política debe revisarse cuando cambien las funciones, destinatarios o prácticas de datos de Melodia. La fecha indicada identifica la revisión de este texto.

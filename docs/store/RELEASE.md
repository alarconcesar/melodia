# Publicar Melodia en Google Play

## 1. Requisitos
- Cuenta de desarrollador de Google Play (pago único).
- Este repo + Android SDK con `platforms;android-34` (o 35) y JDK 17.

## 2. Keystore de firma (UNA sola vez — ¡respaldo sagrado!)
El keystore firma todas las versiones. **Si lo pierdes, no podrás actualizar la app.**

```bash
keytool -genkeypair -v -keystore melodia-upload.jks -alias melodia \
  -keyalg RSA -keysize 2048 -validity 10000
```

- Guarda `melodia-upload.jks` + alias + contraseñas en un lugar seguro (NO va al repo).
- En Play Console: Configuración → Firma de apps → usa **Play App Signing** (Google resguarda la firma final).

## 3. Configurar firma local
Crea `keystore.properties` en la raíz (ignorado por git):

```properties
storeFile=/ruta/a/melodia-upload.jks
storePassword=***
keyAlias=melodia
keyPassword=***
```

Y referencia desde `app/build.gradle.kts` (ver sección signingConfigs).

## 4. Compilar el bundle release

```bash
export ANDROID_HOME=$HOME/Android
./gradlew bundleRelease
# sale en: app/build/outputs/bundle/release/app-release.aab
```

## 5. Subir a Play Console
1. Crear app → nombre "Melodia", idioma español, categoría Música y audio.
2. Subir el `.aab` a un canal Cerrado (prueba interna) primero.
3. Completar: ficha de tienda (`docs/store/STORE_LISTING.md`), gráficos, clasificación IARC, audiencia objetivo, **Seguridad de los datos** (ver STORE_LISTING), política de privacidad (publica `PRIVACY.md` en una URL).
4. Declarar: sin anuncios, sin cambios de API sensibles. El permiso `FOREGROUND_SERVICE_MEDIA_PLAYBACK` se justifica como reproducción de música en segundo plano.
5. Lanzar a producción cuando el canal cerrado esté verificado.

## 6. Versionado
- `versionCode` entero que siempre sube (Play lo exige).
- `versionName` visible (3.0, 3.1…).

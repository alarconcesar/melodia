# Melodia: reglas de R8/ProGuard para la release.
# Room, Media3, Coil y Compose traen sus propias reglas; aquí solo lo específico.

# NewPipeExtractor: se instancia por ServiceList y LinkHandlers (cuidado con el shrinking).
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.schabi.newpipe.extractor.services.youtube.** { *; }

# Base de datos local: Room genera *Impl por reflexión de nombre.
-keep class com.melodia.app.data.local.** { *; }

# Servicio y widget: referenciados desde el manifiesto.
-keep class com.melodia.app.player.MelodiaPlaybackService { *; }
-keep class com.melodia.app.widget.MelodiaWidget { *; }

# Rhino (motor JS que trae NewPipeExtractor) usa java.beans/javax.script,
# inexistentes en Android: solo avisos, el shrinking los elimina igual.
-dontwarn java.beans.**
-dontwarn javax.script.**

# Quita logs en release (menos bytecode, nada de PII en logcat).
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

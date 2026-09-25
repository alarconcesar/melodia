package org.conscrypt;

import java.security.Provider;

/**
 * Stub SOLO para los tests JVM (Robolectric en el VPS aarch64).
 *
 * conscrypt-openjdk-uber no incluye nativos para linux-aarch_64, así que
 * Robolectric moría en setUpApplicationState al instalar OpenSSLProvider
 * (UnsatisfiedLinkError). Este proveedor vacío satisface esa llamada: los
 * tests no hacen TLS, solo componen UI.
 *
 * No entra al APK: vive en src/test, no en src/main.
 */
public class OpenSSLProvider extends Provider {
    public OpenSSLProvider() {
        super("AndroidOpenSSL", 1.0d, "stub de test (sin JNI)");
    }
}

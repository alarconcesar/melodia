package com.melodia.app.data.online

import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.search.SearchExtractor

/**
 * Traduce los fallos de YouTube a algo que el usuario pueda entender.
 *
 * Antes se mostraba el nombre de la clase y el mensaje crudo
 * ("NothingFoundException: These results may be inappropriate for some users."):
 * eso no dice qué hacer y parece un error de la app. Aquí cada caso conocido
 * tiene su frase y los desconocidos caen en el texto genérico.
 */
object YtErrors {

    fun message(t: Throwable, fallback: String): String = when (t) {
        is DlUrlExpiredException ->
            "El enlace de YouTube venció — reintenta la descarga."
        is SearchExtractor.NothingFoundException ->
            "YouTube no tiene resultados para esa búsqueda. Prueba con el nombre del artista o del álbum."
        is AgeRestrictedContentException ->
            "Ese tema tiene restricción de edad y YouTube no lo entrega aquí."
        is GeographicRestrictionException ->
            "Ese contenido no está disponible en tu país."
        is ContentNotAvailableException ->
            "Ese contenido ya no está disponible en YouTube."
        is ReCaptchaException, is SignInConfirmNotBotException ->
            "YouTube pidió una verificación. Espera unos minutos y reintenta."
        is java.io.IOException ->
            "Sin conexión o YouTube no responde — revisa tu internet y reintenta."
        else -> fallback
    }

    /** Reintentar solo sirve cuando el fallo puede cambiar solo (red, verificación). */
    fun retryable(t: Throwable): Boolean = when (t) {
        is SearchExtractor.NothingFoundException,
        is AgeRestrictedContentException,
        is GeographicRestrictionException,
        is ContentNotAvailableException -> false
        else -> true
    }
}

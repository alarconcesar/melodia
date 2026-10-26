package com.melodia.app

import com.melodia.app.data.online.YtErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.search.SearchExtractor
import java.io.IOException

/**
 * Los fallos de YouTube se traducen a frases útiles: nada de nombres de clase
 * ni mensajes en inglés que parezcan errores de la app.
 */
class YtErrorsTest {

    private fun nfe() = SearchExtractor.NothingFoundException(
        "These results may be inappropriate for some users."
    )

    @Test
    fun sinResultadosSeExplicaEnEspanolYNoSeReintenta() {
        val msg = YtErrors.message(nfe(), "fallback")
        assertTrue("debe hablar de resultados: $msg", msg.contains("resultados", true))
        assertFalse("no debe filtrar la clase: $msg", msg.contains("Exception"))
        assertFalse("no debe filtrar el mensaje crudo: $msg", msg.contains("inappropriate"))
        assertFalse(YtErrors.retryable(nfe()))
    }

    @Test
    fun sinInternetLoDiceYSiSePuedeReintentar() {
        val msg = YtErrors.message(IOException("failed to connect"), "fallback")
        assertTrue(msg.contains("conexión", true))
        assertFalse(msg.contains("IOException"))
        assertTrue(YtErrors.retryable(IOException("boom")))
    }

    @Test
    fun verificacionDeYoutubeAvisaEsperar() {
        val t = ReCaptchaException("rate-limited", "https://youtube.com")
        val msg = YtErrors.message(t, "fallback")
        assertTrue(msg.contains("verificación", true))
        assertTrue(YtErrors.retryable(t))
    }

    @Test
    fun restriccionDeEdadNoSeReintenta() {
        val t = AgeRestrictedContentException("age")
        assertTrue(YtErrors.message(t, "fallback").contains("edad", true))
        assertFalse(YtErrors.retryable(t))
    }

    @Test
    fun errorDesconocidoUsaElTextoGenericoSinTecnicismos() {
        val msg = YtErrors.message(RuntimeException("Null pointer at line 42"), "YouTube no respondió — reintenta")
        assertEquals("YouTube no respondió — reintenta", msg)
        assertFalse(msg.contains("42"))
    }
}

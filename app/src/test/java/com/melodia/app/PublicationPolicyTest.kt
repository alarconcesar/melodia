package com.melodia.app

import com.melodia.app.data.MediaDeleteBatch
import com.melodia.app.widget.WidgetActionPolicy
import com.melodia.app.widget.WidgetActionPolicy.Route
import org.junit.Assert.*
import org.junit.Test

/** Límites de Android 16 y rutas de widget sin servicio, puros y deterministas. */
class PublicationPolicyTest {
    @Test fun loteDeBorradoVacioNoSeConvierteEnSolicitud() {
        assertTrue(MediaDeleteBatch.next(emptyList<Long>()).isEmpty())
    }

    @Test fun loteCon2000ConservaLosIdsYElOrden() {
        val ids = (1L..2000L).toList()
        assertEquals(ids, MediaDeleteBatch.next(ids))
    }

    @Test fun loteCon2001DejaElUltimoParaElSiguienteConsentimiento() {
        val pending = (1L..2001L).toList()
        val first = MediaDeleteBatch.next(pending)
        assertEquals(2000, first.size)
        assertEquals(2000L, first.last())
        val remaining = pending.filterNot { it in first.toSet() }
        assertEquals(listOf(2001L), MediaDeleteBatch.next(remaining))
    }

    @Test fun tresLotesNoPierdenNiRepitenArchivos() {
        val pending = (1L..4001L).toList()
        val batches = generateSequence(pending) { rest -> rest.drop(MediaDeleteBatch.MAX_URIS).takeIf { it.isNotEmpty() } }
            .map { MediaDeleteBatch.next(it) }.toList()
        assertEquals(listOf(2000, 2000, 1), batches.map { it.size })
        assertEquals(pending, batches.flatten())
    }

    @Test fun temporizadorEnFrioAbreLaAppSinArrancarForeground() {
        assertEquals(Route.OPEN_APP, WidgetActionPolicy.route(true, false, false, false))
    }

    @Test fun temporizadorSoloSeIniciaConReproduccion() {
        assertEquals(Route.START_SLEEP, WidgetActionPolicy.route(true, true, true, false))
        assertEquals(Route.OPEN_APP, WidgetActionPolicy.route(true, false, true, false))
    }

    @Test fun temporizadorExistenteSePuedeCancelarEnPausaSinServicioNuevo() {
        assertEquals(Route.CANCEL_SLEEP, WidgetActionPolicy.route(true, false, false, true))
    }

    @Test fun controlesSinTemaAbrenLaAppParaRestaurarSinServicioVacio() {
        assertEquals(Route.OPEN_APP, WidgetActionPolicy.route(false, false, false, false))
    }

    @Test fun controlesConTemaActivoConservanElServicio() {
        assertEquals(Route.START_SERVICE, WidgetActionPolicy.route(false, true, true, false))
        assertEquals(Route.START_SERVICE, WidgetActionPolicy.route(false, false, true, false))
    }
}

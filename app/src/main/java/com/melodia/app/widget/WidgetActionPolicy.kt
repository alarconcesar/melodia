package com.melodia.app.widget

/** El temporizador no justifica arrancar un servicio foreground de reproducción. */
internal object WidgetActionPolicy {
    enum class Route { START_SERVICE, START_SLEEP, CANCEL_SLEEP, OPEN_APP }

    fun route(isSleep: Boolean, isPlaying: Boolean, hasActiveTrack: Boolean, hasTimer: Boolean): Route = when {
        isSleep && hasTimer -> Route.CANCEL_SLEEP
        isSleep && isPlaying -> Route.START_SLEEP
        isSleep -> Route.OPEN_APP
        !isPlaying && !hasActiveTrack -> Route.OPEN_APP
        else -> Route.START_SERVICE
    }
}

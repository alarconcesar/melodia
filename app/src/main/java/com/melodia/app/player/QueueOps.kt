package com.melodia.app.player

/**
 * Operaciones puras sobre la cola de reproducción.
 *
 * Viven fuera de [MusicPlayer] y del ViewModel para poder probarlas en la JVM:
 * un movimiento mal calculado desincroniza la lista que ve el usuario de la
 * que reproduce ExoPlayer (se salta temas o revienta al llegar al final).
 */
object QueueOps {

    /**
     * Devuelve una lista nueva con el elemento de [from] movido a [to].
     * `null` cuando el movimiento no aplica (índices fuera de rango o mismo sitio),
     * así el llamador no toca ni la UI ni el reproductor.
     */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T>? {
        if (list.isEmpty()) return null
        if (from == to) return null
        if (from !in list.indices || to !in list.indices) return null
        val out = list.toMutableList()
        val item = out.removeAt(from)
        out.add(to, item)
        return out
    }

    /** `true` si el tema de [index] puede subir un puesto. */
    fun canMoveUp(index: Int, size: Int): Boolean = index > 0 && index < size

    /** `true` si el tema de [index] puede bajar un puesto. */
    fun canMoveDown(index: Int, size: Int): Boolean = index >= 0 && index < size - 1

    /**
     * Cola resultante de "vaciar" conservando el tema actual: una lista de un
     * solo elemento, o vacía si el índice actual no existe.
     */
    fun <T> keepOnly(list: List<T>, index: Int): List<T> {
        if (index !in list.indices) return emptyList()
        return listOf(list[index])
    }
}

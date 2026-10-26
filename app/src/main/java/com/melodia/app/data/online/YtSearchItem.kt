package com.melodia.app.data.online

/**
 * Resultado de búsqueda en YouTube: un tema suelto o una colección
 * (playlist o álbum, que en YouTube llegan igual como lista).
 *
 * La búsqueda mezcla ambos tipos en el mismo orden que devuelve YouTube, así que
 * la UI necesita una lista común en vez de `List<YtTrack>`.
 */
sealed interface YtSearchItem {
    /** Clave estable para LazyColumn y para deduplicar entre páginas. */
    val key: String

    data class Track(val track: YtTrack) : YtSearchItem {
        override val key: String get() = "v:${track.id}"
    }

    data class Collection(
        val id: String,
        val title: String,
        val uploader: String,
        /** Cantidad de temas si YouTube la informa; 0 = desconocida. */
        val count: Int,
        val thumbUrl: String?,
        val url: String
    ) : YtSearchItem {
        override val key: String get() = "p:$id"
    }
}

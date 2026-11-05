package com.melodia.app.data

/** Android 16 limita createDeleteRequest a 2000 URI por consentimiento. */
internal object MediaDeleteBatch {
    const val MAX_URIS = 2000
    fun <T> next(pending: List<T>): List<T> = pending.take(MAX_URIS)
}

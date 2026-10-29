package com.melodia.app.player
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class SleepTimer(private val onExpire: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private val _minsLeft = MutableStateFlow<Int?>(null)
    val minsLeft: StateFlow<Int?> = _minsLeft
    fun start(minutes: Int) {
        cancel()
        // Sin guarda, start(0) expiraba de inmediato y pausaba la música.
        if (minutes <= 0) return
        _minsLeft.value = minutes
        val holder = arrayOfNulls<Job>(1)
        holder[0] = scope.launch {
            repeat(minutes) { delay(60_000); _minsLeft.value = (_minsLeft.value ?: 1) - 1 }
            _minsLeft.value = null
            // Solo dispara si no se canceló en el camino.
            if (job === holder[0]) { job = null; onExpire() }
        }
        job = holder[0]
    }
    /** true mientras el timer sigue corriendo (el fundido lo consulta antes de pausar). */
    fun isActive(): Boolean = try { job?.isActive } catch (_: Exception) { false } ?: false
    fun cancel() { job?.cancel(); job = null; _minsLeft.value = null }
}

package com.melodia.app.player
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class SleepTimer(private val onExpire: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private val _minsLeft = MutableStateFlow<Int?>(null)
    val minsLeft: StateFlow<Int?> = _minsLeft
    fun start(minutes: Int) { cancel(); _minsLeft.value = minutes
        job = scope.launch { repeat(minutes) { delay(60_000); _minsLeft.value = (_minsLeft.value ?: 1) - 1 }; _minsLeft.value = null; onExpire() } }
    fun cancel() { job?.cancel(); job = null; _minsLeft.value = null }
}

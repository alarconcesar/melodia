package com.melodia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melodia.app.MainViewModel
import com.melodia.app.data.online.DlJob
import com.melodia.app.data.online.DlStatus
import com.melodia.app.data.online.YtRepository
import com.melodia.app.ui.ds.DsBackground
import com.melodia.app.ui.theme.Ds
import com.melodia.app.ui.ds.EmptyState
import com.melodia.app.ui.ds.MicroLabel

/**
 * Descargas como pantalla propia: la cola en tarjetas con su progreso, acciones
 * globales (reanudar/pausar todo) e historial. Antes era un `AlertDialog`
 * flotante donde no cabía nada.
 */
@Composable
fun DownloadsScreen(vm: MainViewModel, onClose: () -> Unit, onPlayRecord: (YtRepository.DlRecord) -> Unit = {}) {
    val jobs by vm.dlJobs.collectAsState()
    val hist by vm.dlHistory.collectAsState()
    DownloadsScreenContent(
        jobs = jobs,
        history = hist,
        onClose = onClose,
        onResumeAll = { vm.resumeAllDl() },
        onPauseAll = { vm.pauseAllDl() },
        onPause = { vm.pauseDl(it) },
        onResume = { vm.resumeDl(it) },
        onCancel = { vm.cancelDl(it) },
        onClearHistory = { vm.clearDlHistory() },
        onPlayRecord = onPlayRecord
    )
}

/** Contenido puro (testeable sin ViewModel). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreenContent(
    jobs: List<DlJob>,
    history: List<YtRepository.DlRecord>,
    onClose: () -> Unit,
    onResumeAll: () -> Unit,
    onPauseAll: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlayRecord: (YtRepository.DlRecord) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    // DONE queda visible unos segundos como "Guardada": antes la tarjeta
    // desaparecía al instante y salía "Sin descargas en curso" aunque acabara
    // de terminar (hasta que refrescaba el historial).
    // DONE queda visible 8 s como "Guardada": no cuenta como en curso.
    val activos = jobs
    val enCurso = activos.filter { it.status != DlStatus.DONE }
    val reanudables = activos.filter { it.status == DlStatus.PAUSED || it.status == DlStatus.ERROR }
    val pausables = activos.filter { it.status == DlStatus.ACTIVE || it.status == DlStatus.QUEUED }

    Box(Modifier.fillMaxSize()) {
        DsBackground()
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, "Atrás", tint = cs.onBackground)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Descargas", style = Ds.Display, color = cs.onBackground, modifier = Modifier.weight(1f))
                }
            }

            if (activos.isNotEmpty()) {
                if (enCurso.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MicroLabel("En curso · ${enCurso.size}")
                            Spacer(Modifier.weight(1f))
                            if (pausables.isNotEmpty()) {
                                Text(
                                    "Pausar todo", style = Ds.Sub, color = cs.onSurfaceVariant,
                                    modifier = Modifier.clickable(onClick = onPauseAll).padding(6.dp)
                                )
                            }
                            if (reanudables.isNotEmpty()) {
                                Text(
                                    "Reanudar todo",
                                    style = Ds.Sub.copy(fontWeight = FontWeight.Medium),
                                    color = cs.primary,
                                    modifier = Modifier.clickable(onClick = onResumeAll).padding(6.dp)
                                )
                            }
                        }
                    }
                }
                items(activos, key = { it.track.id }) { j ->
                    // Las tarjetas se deslizan al entrar/salir en vez de saltar.
                    Box(Modifier.animateItemPlacement()) {
                        DlJobCard(
                            job = j,
                            onPause = { onPause(j.track.id) },
                            onResume = { onResume(j.track.id) },
                            onCancel = { onCancel(j.track.id) }
                        )
                    }
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = "Sin descargas en curso",
                        subtitle = "Lo que bajes desde Online aparece aquí con su progreso."
                    )
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Historial", style = Ds.Title, color = cs.onBackground, modifier = Modifier.weight(1f))
                        IconButton(onClick = onClearHistory, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Borrar historial", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                        }
                    }
                }
                itemsIndexed(history, key = { i, r -> "hist-$i-${r.at}-${r.title}" }) { _, r ->
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = { onPlayRecord(r) })
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            r.title, style = Ds.Body, color = cs.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${r.artist.ifBlank { "YouTube" }} · ${if (r.high) "Alta" else "Estándar"} · " +
                                fecha(r.at),
                            style = Ds.Sub, color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Tarjeta de una descarga: título, estado, progreso y acciones. */
@Composable
private fun DlJobCard(
    job: DlJob,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val st = job.status
    Surface(
        shape = Ds.CardShape, color = cs.surface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        job.track.title, style = Ds.Body, color = cs.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${job.track.artist.ifBlank { "YouTube" }} · ${if (job.high) "Alta" else "Estándar"}" +
                            estado(job) +
                            if (job.speedBps > 0 && job.status == DlStatus.ACTIVE) " · ${vel(job.speedBps)}" else "",
                        style = Ds.Sub,
                        color = if (st == DlStatus.ERROR) cs.error else cs.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                when (st) {
                    DlStatus.ACTIVE, DlStatus.QUEUED -> IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Pause, "Pausar", Modifier.size(18.dp), tint = cs.onBackground)
                    }
                    DlStatus.PAUSED, DlStatus.ERROR -> IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, "Reanudar", Modifier.size(18.dp), tint = cs.primary)
                    }
                    DlStatus.DONE -> Icon(
                        Icons.Default.Check, "Guardada",
                        Modifier.size(20.dp), tint = cs.primary
                    )
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Quitar", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                }
            }
            if (st == DlStatus.ACTIVE || st == DlStatus.QUEUED || st == DlStatus.PAUSED) {
                Spacer(Modifier.height(10.dp))
                // Progreso suavizado: los saltos de ~120 ms del medidor se ven
                // como tirones; el tween los convierte en avance continuo.
                val avance by androidx.compose.animation.core.animateFloatAsState(
                    job.progress.coerceIn(0f, 1f),
                    androidx.compose.animation.core.tween(420, easing = androidx.compose.animation.core.LinearEasing),
                    label = "dlprog"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = avance,
                        color = cs.primary,
                        trackColor = cs.onSurfaceVariant.copy(alpha = 0.20f),
                        modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    // El número es exacto; la barra va suavizada (avance).
                    Text("${(job.progress * 100).toInt()}%", style = Ds.Sub, color = cs.primary)
                }
            }
        }
    }
}

private fun estado(job: DlJob): String = when (job.status) {
    DlStatus.QUEUED -> " · En cola"
    DlStatus.PAUSED -> if (job.total > 0) " · Pausada · ${mb(job.downloaded)}/${mb(job.total)}" else " · Pausada"
    DlStatus.ERROR -> " · ${job.error ?: "Falló"}"
    DlStatus.DONE -> " · Guardada"
    else -> if (job.total > 0) " · ${mb(job.downloaded)}/${mb(job.total)}" else ""
}

private fun mb(b: Long): String = "%.1f MB".format(b / 1048576f)

/** Velocidad legible: KB/s o MB/s. */
private fun vel(bps: Long): String =
    if (bps >= 1_048_576L) "%.1f MB/s".format(bps / 1048576f)
    else "${(bps / 1024L).coerceAtLeast(1L)} KB/s"

private fun fecha(at: Long): String = try {
    java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(at))
} catch (_: Exception) { "" }

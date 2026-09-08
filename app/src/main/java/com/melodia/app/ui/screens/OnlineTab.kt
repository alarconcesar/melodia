package com.melodia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.melodia.app.MainViewModel
import com.melodia.app.data.online.YtTrack
import com.melodia.app.ui.theme.Ds

private fun fmtDur(sec: Long): String {
    if (sec < 0) return "--:--"
    val m = sec / 60
    val s = sec % 60
    return if (m >= 60) "${m / 60}:${(m % 60).toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnlineTab(vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    val query by vm.ytQuery.collectAsState()
    val results by vm.ytResults.collectAsState()
    val loading by vm.ytLoading.collectAsState()
    val error by vm.ytError.collectAsState()
    val dl by vm.dlProgress.collectAsState()
    val busy by vm.busyYt.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Online", style = Ds.Display, color = cs.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Busca en YouTube · ▶ para escuchar · ⬇ para guardar en tu música",
            style = Ds.Body, color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { vm.setYtQuery(it) },
                placeholder = { Text("Canción, artista…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { kb?.hide(); vm.searchYt() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = cs.primary, modifier = Modifier.size(52.dp)) {
                Box(Modifier.clickable { kb?.hide(); vm.searchYt() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Search, null, tint = cs.surface)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary)
            }
            error != null && results.isEmpty() -> Text(
                error ?: "", style = Ds.Body, color = cs.error,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            results.isEmpty() -> Text(
                "Escribe algo y pulsa buscar 🔎",
                style = Ds.Body, color = cs.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            else -> {
                error?.let {
                    Text(it, style = Ds.Sub, color = cs.error, modifier = Modifier.padding(bottom = 6.dp))
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 200.dp)
                ) {
                    items(results, key = { it.id }) { track ->
                        YtRow(
                            track = track,
                            progress = dl[track.id],
                            busy = busy == track.id,
                            onPlay = { vm.playYt(track) },
                            onDownload = { vm.downloadYt(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YtRow(
    track: YtTrack,
    progress: Float?,
    busy: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = cs.surface, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.thumbUrl != null) {
                    AsyncImage(
                        model = track.thumbUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                            .background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = cs.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title, style = Ds.Body, color = cs.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${track.artist.ifBlank { "YouTube" }} · ${fmtDur(track.durationSec)}",
                        style = Ds.Sub, color = cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (busy) {
                    CircularProgressIndicator(
                        color = cs.primary, strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, "Escuchar", tint = cs.primary, modifier = Modifier.size(28.dp))
                    }
                }
                if (progress == null) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, "Descargar", tint = cs.onSurfaceVariant)
                    }
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    color = cs.primary,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

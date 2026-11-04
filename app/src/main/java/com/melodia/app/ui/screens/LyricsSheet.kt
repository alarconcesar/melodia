package com.melodia.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodia.app.MainViewModel
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.EmptyState
import com.melodia.app.ui.ds.MelodiaSpinner
import com.melodia.app.ui.ds.PageHeader
import com.melodia.app.ui.theme.Ds

@Composable
fun LyricsSheet(s: Song, vm: MainViewModel, onClose: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val state by vm.playerState.collectAsState()
    LyricsContent(s, lyrics, state.positionMs, state.durationMs, onClose,
        onRetry = vm::retryLyrics,
        onSeek = { vm.player.seekTo(it) },
        onNotice = { vm.avisar("Aún sin duración: espera a que cargue el tema") })
}

/** Los versos conservan tamaño y ritmo al activarse: el seguimiento no hace saltar el layout. */
@Composable
fun LyricsContent(
    song: Song, lyrics: MainViewModel.LyricsUi, positionMs: Long, durationMs: Long,
    onClose: () -> Unit, onRetry: () -> Unit, onSeek: (Float) -> Unit, onNotice: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.padding(horizontal = 12.dp)) {
            PageHeader("Letra", song.title, onClose) {
                IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, "Reintentar", tint = cs.onSurfaceVariant) }
            }
        }
        Text(song.artist.ifBlank { "Desconocido" }, style = Ds.Sub, color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        when (lyrics) {
            is MainViewModel.LyricsUi.Idle, is MainViewModel.LyricsUi.Loading -> {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MelodiaSpinner()
                    Text("Buscando la letra…", style = Ds.Sub, color = cs.onSurfaceVariant)
                }
            }
            // Los streams comparten id: título/artista distinguen también el tema online.
            is MainViewModel.LyricsUi.Synced -> key(song.id, song.title, song.artist) {
                val currentIndex = lyrics.lines.indexOfLast { it.ms <= positionMs + 300 }
                val listState = rememberLazyListState()
                var following by rememberSaveable { mutableStateOf(true) }
                val manualScroll = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            // En Compose 1.6 el arrastre del dedo llega como Drag; el
                            // animateScrollToItem del seguimiento no emite nested scroll.
                            // isScrollInProgress no sirve: también está activo al animar.
                            if (source == NestedScrollSource.Drag && available.y != 0f) following = false
                            return Offset.Zero // Solo observa: el gesto sigue siendo de la lista.
                        }
                    }
                }
                LaunchedEffect(currentIndex, following) {
                    if (following && currentIndex >= 0) {
                        listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
                    }
                }
                // El slot siempre ocupa lo mismo: mostrar la acción no desplaza los versos.
                Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp)
                    .testTag("lyrics-follow-slot"), contentAlignment = Alignment.Center) {
                    TextButton(
                        onClick = { following = true }, enabled = !following,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .alpha(if (following) 0f else 1f)
                            .then(if (following) Modifier.clearAndSetSemantics {} else Modifier)
                    ) {
                        Text("Volver al verso actual", style = Ds.Sub, color = cs.primary)
                    }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("lyrics-synced-list").nestedScroll(manualScroll),
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp)) {
                    itemsIndexed(lyrics.lines, key = { i, line -> "$i-${line.ms}" }) { index, line ->
                        val active = index == currentIndex
                        val tint by animateColorAsState(if (active) cs.primary else cs.onSurfaceVariant,
                            tween(160), label = "verso")
                        Text(line.text, style = Ds.Body.copy(fontSize = 19.sp, lineHeight = 29.sp,
                            fontWeight = FontWeight.Medium), color = tint,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = active }
                                .clickable(role = Role.Button, onClickLabel = "Ir a este verso") {
                                    if (durationMs > 0) onSeek((line.ms.toFloat() / durationMs).coerceIn(0f, 1f)) else onNotice()
                                }.padding(vertical = 12.dp))
                    }
                }
            }
            is MainViewModel.LyricsUi.Plain -> key(song.id) {
                val lines = remember(lyrics.text) { lyrics.text.lines() }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 100.dp)) {
                    item { Text("Letra sin sincronizar", style = Ds.Micro, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)) }
                    itemsIndexed(lines) { _, line ->
                        Text(line.ifBlank { " " }, style = Ds.Body.copy(fontSize = 18.sp, lineHeight = 28.sp),
                            color = cs.onSurface, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
            }
            is MainViewModel.LyricsUi.None -> {
                EmptyState(Icons.Default.Lyrics, "Sin letra para este tema",
                    "No está en la base pública de letras. Puedes reintentar más tarde.",
                    action = "Reintentar", onAction = onRetry)
            }
        }
    }
}

package com.melodia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import com.melodia.app.MainViewModel
import com.melodia.app.data.model.Song
import com.melodia.app.ui.ds.MelodiaSpinner
import com.melodia.app.ui.ds.MicroLabel
import com.melodia.app.ui.theme.Ds

/** Hoja de letra en tiempo real: línea actual resaltada + auto-scroll + tap para saltar. */
@Composable
fun LyricsSheet(s: Song, vm: MainViewModel, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val lyr by vm.lyrics.collectAsState()
    val state by vm.playerState.collectAsState()

    Column(
        Modifier.fillMaxSize().background(cs.background)
            .statusBarsPadding().navigationBarsPadding().padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(28.dp), tint = cs.onBackground)
            }
            Spacer(Modifier.weight(1f))
            MicroLabel("Letra")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.retryLyrics() }) {
                Icon(Icons.Default.Refresh, "Reintentar", tint = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            s.title, style = Ds.Body, color = cs.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            s.artist.ifBlank { "Desconocido" }, style = Ds.Sub, color = cs.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        when (val l = lyr) {
            is MainViewModel.LyricsUi.Idle, is MainViewModel.LyricsUi.Loading -> {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    MelodiaSpinner()
                }
            }
            is MainViewModel.LyricsUi.Synced -> {
                val lines = l.lines
                val pos = state.positionMs
                val idx = lines.indexOfLast { it.ms <= pos + 300 }.coerceAtLeast(0)
                val listState = rememberLazyListState()
                androidx.compose.runtime.LaunchedEffect(s.id) {
                    try { listState.scrollToItem(0) } catch (_: Exception) {}
                }
                LaunchedEffect(idx) {
                    // Si el usuario está leyendo otras estrofas (scrolleando),
                    // no se le arranca la lista de las manos cada 3 segundos.
                    if (!listState.isScrollInProgress) {
                        try { listState.animateScrollToItem((idx - 2).coerceAtLeast(0)) } catch (_: Exception) {}
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 60.dp)
                ) {
                    itemsIndexed(lines, key = { i, e -> "$i-${e.ms}" }) { i, e ->
                        val cur = i == idx
                        val lineColor by androidx.compose.animation.animateColorAsState(
                            if (cur) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.75f),
                            label = "lyr"
                        )
                        Text(
                            e.text,
                            style = if (cur) Ds.Body else Ds.Sub,
                            color = lineColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    val d = state.durationMs
                                    if (d > 0) vm.player.seekTo((e.ms.toFloat() / d).coerceIn(0f, 1f))
                                    else vm.avisar("Aún sin duración: espera a que cargue el tema")
                                }
                                .padding(vertical = if (cur) 10.dp else 7.dp)
                        )
                    }
                }
            }
            is MainViewModel.LyricsUi.Plain -> {
                // Clave por tema: si no, hereda el scroll de la letra anterior.
                androidx.compose.runtime.key(s.id) {
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(l.text, style = Ds.Body, color = cs.onBackground, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(60.dp))
                }
                }
            }
            is MainViewModel.LyricsUi.None -> {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sin letra para este tema", style = Ds.Title, color = cs.onBackground)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No está en la base pública de letras. Puedes reintentar más tarde.",
                        style = Ds.Sub, color = cs.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = cs.primary,
                        modifier = Modifier.clickable { vm.retryLyrics() }
                    ) {
                        Text(
                            "Reintentar", style = Ds.Body, color = cs.onPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

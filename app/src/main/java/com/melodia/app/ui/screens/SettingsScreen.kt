package com.melodia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melodia.app.MainViewModel
import com.melodia.app.ui.ds.DsBackground
import com.melodia.app.ui.ds.MicroLabel
import com.melodia.app.ui.theme.Accents
import com.melodia.app.ui.theme.Ds
import com.melodia.app.ui.theme.accentName
import com.melodia.app.ui.theme.isDarkMode
import com.melodia.app.ui.theme.saveUiPrefs

/**
 * Ajustes como pantalla propia: agrupa apariencia, reproducción, biblioteca,
 * datos y información. Antes todo esto vivía en un menú de 8 opciones.
 */
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onClose: () -> Unit,
    onOpenEq: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenCrossfade: () -> Unit,
    onOpenStats: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val state by vm.playerState.collectAsState()
    val folder by vm.scanFolder.collectAsState()
    val minSecs by vm.minSecs.collectAsState()
    val saver by vm.dataSaver.collectAsState()
    val highQ by vm.ytHighQuality.collectAsState()
    val sleepLeft by vm.sleep.minsLeft.collectAsState()
    var showSleep by rememberSaveable { mutableStateOf(false) }
    var safeMode by remember { mutableStateOf(com.melodia.app.SafeMode.estaActivo(ctx)) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showExcluded by rememberSaveable { mutableStateOf(false) }
    val excluded by vm.excludedFolders.collectAsState()
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "—"
        } catch (_: Exception) { "—" }
    }
    val dark = isDarkMode ?: isSystemInDarkTheme()

    Box(Modifier.fillMaxSize()) {
        DsBackground()
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, "Atrás", tint = cs.onBackground)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Ajustes", style = Ds.Display, color = cs.onBackground)
                }
            }

            // ── Apariencia ──
            item { SectionLabel("Apariencia") }
            item {
                SettingsCard {
                    Text("Tema", style = Ds.Body, color = cs.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip(isDarkMode == false, "Claro", Icons.Default.LightMode) {
                            isDarkMode = false; saveUiPrefs(ctx)
                        }
                        ToggleChip(isDarkMode == true, "Oscuro", Icons.Default.DarkMode) {
                            isDarkMode = true; saveUiPrefs(ctx)
                        }
                        ToggleChip(isDarkMode == null, "Sistema", Icons.Default.BrightnessAuto) {
                            isDarkMode = null; saveUiPrefs(ctx)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Color de acento", style = Ds.Body, color = cs.onBackground)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Accents.forEach { a ->
                            val sel = accentName == a.name
                            val col = if (dark) a.dark else a.light
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(col)
                                    .border(
                                        if (sel) 3.dp else 1.dp,
                                        if (sel) cs.onBackground else cs.outline.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .clickable { accentName = a.name; saveUiPrefs(ctx) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) Icon(
                                    Icons.Default.Check, a.name, Modifier.size(20.dp),
                                    tint = if (dark) Color(0xFF06110A) else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── Reproducción ──
            item { SectionLabel("Reproducción") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Fundido entre canciones",
                        value = if (state.crossfade) "Activado · ${state.crossfadeSecs} s" else "Desactivado",
                        onClick = onOpenCrossfade
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Equalizer,
                        title = "Ecualizador",
                        value = "5 bandas, bajos y presets",
                        onClick = onOpenEq
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "Temporizador",
                        value = sleepLeft?.let { "Apaga en $it min" } ?: "Apagado",
                        onClick = { showSleep = true }
                    )
                }
            }

            // ── Biblioteca ──
            item { SectionLabel("Biblioteca") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Folder,
                        title = "Carpetas y escaneo",
                        value = (folder?.substringAfterLast("/") ?: "Toda la música") + " · mínimo ${minSecs}s",
                        onClick = onOpenLibrary
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.FolderOff,
                        title = "Carpetas excluidas",
                        value = if (excluded.isEmpty()) "Ninguna" else "${excluded.size} fuera de la biblioteca",
                        onClick = { showExcluded = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Assessment,
                        title = "Mis estadísticas",
                        value = "Lo que más escuchas",
                        onClick = onOpenStats
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Save,
                        title = "Respaldo y restauración",
                        value = "Exporta listas, favoritos y datos",
                        onClick = onOpenBackup
                    )
                }
            }

            // ── Datos y descargas ──
            item { SectionLabel("Datos y descargas") }
            item {
                SettingsCard {
                    SettingsSwitch(
                        icon = Icons.Default.Download,
                        title = "Descargas de alta calidad",
                        subtitle = "Mejor sonido, archivo más grande",
                        checked = highQ,
                        onChecked = { vm.setYtQuality(it) }
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        icon = Icons.Default.DataUsage,
                        title = "Ahorro de datos",
                        subtitle = "Audio más liviano al escuchar online",
                        checked = saver,
                        onChecked = { vm.setDataSaver(it) }
                    )
                }
            }

            // ── Rendimiento: medido en vivo, sin adb ──
            item { SectionLabel("Rendimiento") }
            item {
                SettingsCard {
                    var tick by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            tick++
                        }
                    }
                    val overall = remember(tick) { com.melodia.app.perf.FrameMonitor.overall() }
                    val perScreen = remember(tick) { com.melodia.app.perf.FrameMonitor.byScreen() }
                    SettingsRow(
                        icon = Icons.Default.Speed,
                        title = if (overall.frames < 30) "Midiendo…" else "${overall.fps} fps · ${overall.slowPct}% lentos",
                        value = if (overall.frames < 30) "Navega un momento y vuelve"
                        else "Peor frame: ${overall.worstMs} ms"
                    )
                    perScreen.take(4).forEach { st ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(st.screen, style = Ds.Sub, color = cs.onBackground, modifier = Modifier.weight(1f))
                            Text(
                                "${st.fps} fps · ${st.slowPct}% · ${st.worstMs} ms",
                                style = Ds.Sub,
                                color = if (st.slowPct >= 5) cs.error else cs.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reinicia la medición, usa una pantalla 10 s y vuelve a mirar aquí.",
                        style = Ds.Sub, color = cs.onSurfaceVariant
                    )
                    Text(
                        "Reiniciar medición",
                        style = Ds.Sub.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = cs.primary,
                        modifier = Modifier.clickable {
                            com.melodia.app.perf.FrameMonitor.reset()
                            tick++
                        }.padding(vertical = 6.dp)
                    )
                }
            }

            // ── Info ──
            item { SectionLabel("Información") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "Melodia $version",
                        value = "Reproductor local · sin anuncios",
                        onClick = { showAbout = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacidad",
                        value = "No recopilamos nada",
                        onClick = { showAbout = true }
                    )
                    SettingsDivider()
                    SettingsSwitch(
                        icon = Icons.Default.HealthAndSafety,
                        title = "Modo seguro",
                        subtitle = "Sin efectos de vidrio, esqueleto ni gestos",
                        checked = safeMode,
                        onChecked = { on ->
                            safeMode = on
                            if (on) com.melodia.app.SafeMode.activarTodo(ctx)
                            else com.melodia.app.SafeMode.apagarTodo(ctx)
                            try {
                                android.widget.Toast.makeText(
                                    ctx, "Se aplica al reiniciar la app",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text("Apagar en…", color = cs.onBackground) },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 60).forEach { m ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { vm.sleep.start(m); showSleep = false }
                                .padding(vertical = 10.dp)
                        ) { Text("$m minutos", color = cs.onBackground) }
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { vm.sleep.cancel(); showSleep = false }
                            .padding(vertical = 10.dp)
                    ) { Text("Cancelar temporizador", color = cs.onBackground) }
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("Cerrar") } }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Melodia", color = cs.onBackground) },
            text = {
                Column {
                    Text(
                        "Reproductor de la música que ya está en tu teléfono. Sin cuentas, sin anuncios y sin recopilar datos: nada sale del dispositivo salvo las búsquedas online que tú pides.",
                        style = Ds.Body, color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Fuentes usadas solo cuando eliges online:", style = Ds.Sub, color = cs.onBackground)
                    Text("· YouTube (audio y búsqueda)", style = Ds.Sub, color = cs.onSurfaceVariant)
                    Text("· LRCLIB (letras)", style = Ds.Sub, color = cs.onSurfaceVariant)
                    Text("· Tus archivos (carátulas y metadatos)", style = Ds.Sub, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Versión $version", style = Ds.Sub, color = cs.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Cerrar") } }
        )
    }

    if (showExcluded) {
        ExcludedFoldersDialog(vm, excluded, onClose = { showExcluded = false })
    }
}

// ── Piezas de la pantalla ────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Box(Modifier.padding(top = 16.dp, bottom = 8.dp)) { MicroLabel(text) }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = Ds.CardShape, color = cs.surface,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .then(
                if (onClick != null) Modifier.then(
                    com.melodia.app.ui.ds.pressClick(onClick, min = 0.985f)
                ) else Modifier
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = cs.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ds.Body, color = cs.onBackground)
            if (value != null) Text(
                value, style = Ds.Sub, color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = cs.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ds.Body, color = cs.onBackground)
            Text(subtitle, style = Ds.Sub, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)

    }
}

/**
 * Carpetas que no deben entrar a la biblioteca (audios de WhatsApp, grabaciones
 * de voz, etc.). Se listan también las excluidas para poder reactivarlas.
 */
@Composable
private fun ExcludedFoldersDialog(
    vm: MainViewModel,
    excluded: Set<String>,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val folders by vm.folders.collectAsState()
    val paths = remember(folders, excluded) {
        (folders.map { it.path } + excluded).distinct().sortedBy { it.lowercase() }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Carpetas excluidas", color = cs.onBackground) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Lo que esté dentro de una carpeta excluida no aparece en tu biblioteca.",
                    style = Ds.Sub, color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (paths.isEmpty()) {
                    Text("Todavía no hay carpetas para mostrar.", style = Ds.Body, color = cs.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(paths, key = { "ex-$it" }) { p ->
                            val activa = excluded.contains(p)
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { vm.toggleExcludedFolder(p) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        try { java.io.File(p).name.ifBlank { p } } catch (_: Exception) { p },
                                        style = Ds.Body, color = cs.onBackground,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        p, style = Ds.Sub, color = cs.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(checked = activa, onCheckedChange = { vm.toggleExcludedFolder(p) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Listo") } }
    )
}

package com.melodia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodia.app.MainViewModel
import com.melodia.app.ui.ds.DsBackground
import com.melodia.app.ui.ds.PageHeader
import com.melodia.app.ui.theme.Accents
import com.melodia.app.ui.theme.Ds
import com.melodia.app.ui.theme.accentName
import com.melodia.app.ui.theme.isDarkMode
import com.melodia.app.ui.theme.saveUiPrefs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    // Ajustes solo necesita el fundido, no el reloj ni el progreso de reproducción.
    val initialPlaybackSettings = remember(vm) {
        vm.playerState.value.let { it.crossfade to it.crossfadeSecs }
    }
    val playbackSettings by remember(vm) {
        vm.playerState.map { it.crossfade to it.crossfadeSecs }.distinctUntilChanged()
    }.collectAsState(initial = initialPlaybackSettings)
    val (crossfadeEnabled, crossfadeSeconds) = playbackSettings
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
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // El regreso permanece a mano incluso al desplazarse hasta el final.
            Box(Modifier.padding(horizontal = 20.dp)) {
                PageHeader(title = "Ajustes", onBack = onClose)
            }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "apariencia-titulo") { SectionLabel("Apariencia") }
                item(key = "apariencia") {
                    SettingsCard {
                        SettingsAppearance(
                            themeMode = isDarkMode,
                            selectedAccent = accentName,
                            dark = dark,
                            onThemeSelected = { isDarkMode = it; saveUiPrefs(ctx) },
                            onAccentSelected = { accentName = it; saveUiPrefs(ctx) }
                        )
                    }
                }

                // ── Reproducción ──
                item(key = "reproduccion-titulo") { SectionLabel("Reproducción") }
                item(key = "reproduccion") {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Fundido entre canciones",
                            value = if (crossfadeEnabled) "Activado · $crossfadeSeconds s" else "Desactivado",
                            onClick = onOpenCrossfade
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Equalizer,
                            title = "Ecualizador",
                            value = "5 bandas, bajos y preajustes",
                            onClick = onOpenEq
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Timer,
                            title = "Temporizador",
                            value = sleepLeft?.let { "Pausa la música en $it min" } ?: "Desactivado",
                            onClick = { showSleep = true }
                        )
                    }
                }

                // ── Biblioteca ──
                item(key = "biblioteca-titulo") { SectionLabel("Biblioteca") }
                item(key = "biblioteca") {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Default.Folder,
                            title = "Carpetas y escaneo",
                            value = (folder?.trimEnd('/')?.substringAfterLast("/")?.ifBlank { "Carpeta seleccionada" } ?: "Toda la música") + " · mínimo $minSecs s",
                            onClick = onOpenLibrary
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.FolderOff,
                            title = "Carpetas excluidas",
                            value = when (excluded.size) {
                                0 -> "Ninguna carpeta excluida"
                                1 -> "1 carpeta fuera de la biblioteca"
                                else -> "${excluded.size} carpetas fuera de la biblioteca"
                            },
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
                item(key = "datos-titulo") { SectionLabel("Datos y descargas") }
                item(key = "datos") {
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
                            subtitle = "Audio más liviano al escuchar en línea",
                            checked = saver,
                            onChecked = { vm.setDataSaver(it) }
                        )
                    }
                }

                // ── Info ──
                item(key = "informacion-titulo") { SectionLabel("Información") }
                item(key = "informacion") {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "Melodia $version",
                            value = "Reproductor local · sin anuncios",
                            onClick = { showAbout = true }
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            icon = Icons.Default.HealthAndSafety,
                            title = "Modo seguro",
                            subtitle = "Reduce efectos visuales y desactiva extras y gestos. Se aplica al reiniciar la app.",
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
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            containerColor = cs.surface,
            tonalElevation = 0.dp,
            title = { Text("Temporizador", style = Ds.Title, color = cs.onSurface) },
            text = {
                SleepTimerOptions(
                    remainingMinutes = sleepLeft,
                    onStart = { vm.sleep.start(it); showSleep = false },
                    onCancel = { vm.sleep.cancel(); showSleep = false }
                )
            },
            confirmButton = {
                TextButton(onClick = { showSleep = false }) { Text("Cerrar", color = cs.primary) }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = cs.surface,
            tonalElevation = 0.dp,
            title = { Text("Acerca de Melodia", style = Ds.Title, color = cs.onSurface) },
            text = { SettingsAboutContent(version) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Cerrar", color = cs.primary) }
            }
        )
    }

    if (showExcluded) {
        ExcludedFoldersDialog(vm, excluded, onClose = { showExcluded = false })
    }
}

// ── Piezas puras: se prueban sin ViewModel, Room ni servicio de audio ──

private val SettingsTitleStyle = Ds.Body.copy(fontWeight = FontWeight.Medium, lineHeight = 22.sp)
private val SettingsDetailStyle = Ds.Sub.copy(fontSize = 14.sp, lineHeight = 20.sp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsAppearance(
    themeMode: Boolean?,
    selectedAccent: String,
    dark: Boolean,
    onThemeSelected: (Boolean?) -> Unit,
    onAccentSelected: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Tema", style = SettingsTitleStyle, color = cs.onSurface)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsChoice("Claro", themeMode == false, Icons.Default.LightMode) { onThemeSelected(false) }
            SettingsChoice("Oscuro", themeMode == true, Icons.Default.DarkMode) { onThemeSelected(true) }
            SettingsChoice("Sistema", themeMode == null, Icons.Default.BrightnessAuto) { onThemeSelected(null) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Color de acento", style = SettingsTitleStyle, color = cs.onSurface)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Accents.forEach { accent ->
                SettingsChoice(
                    text = accent.name,
                    selected = selectedAccent == accent.name,
                    swatch = if (dark) accent.dark else accent.light,
                    onClick = { onAccentSelected(accent.name) }
                )
            }
        }
    }
}

@Composable
private fun SettingsChoice(
    text: String,
    selected: Boolean,
    icon: ImageVector? = null,
    swatch: Color? = null,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val foreground = if (selected) cs.onSecondaryContainer else cs.onSurface
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) cs.secondaryContainer else cs.surfaceVariant)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) "Seleccionado" else "No seleccionado" }
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (swatch != null) Box(Modifier.size(20.dp).clip(CircleShape).background(swatch))
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = foreground)
        Text(text, style = SettingsDetailStyle, color = foreground, modifier = Modifier.weight(1f, fill = false))
        if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = foreground)
        else Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = SettingsTitleStyle, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = Ds.CardShape, color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp, shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .then(
                if (onClick != null) com.melodia.app.ui.ds.pressClick(
                    onClick, min = 0.985f, role = Role.Button, label = "Abrir $title"
                ) else Modifier.semantics(mergeDescendants = true) {}
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = cs.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = SettingsTitleStyle, color = cs.onSurface)
            if (value != null) Text(value, style = SettingsDetailStyle, color = cs.onSurfaceVariant)
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val largeText = LocalDensity.current.fontScale >= 1.3f
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChecked)
            .semantics { stateDescription = if (checked) "Activado" else "Desactivado" }
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // En 320 dp o con letra grande, el control baja sin comprimir el texto.
        val stacked = maxWidth < 280.dp || largeText
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = cs.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = SettingsTitleStyle, color = cs.onSurface)
                Text(subtitle, style = SettingsDetailStyle, color = cs.onSurfaceVariant)
                if (stacked) {
                    Switch(checked = checked, onCheckedChange = null, modifier = Modifier.align(Alignment.End))
                }
            }
            if (!stacked) {
                Spacer(Modifier.width(12.dp))
                // Una sola acción y un solo foco: la fila contiene la semántica.
                Switch(checked = checked, onCheckedChange = null)
            }
        }
    }
}

/** Opciones desplazables: siguen alcanzables con fuente grande o poco alto. */
@Composable
internal fun SleepTimerOptions(
    remainingMinutes: Int?,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            remainingMinutes?.let { "La música se pausará en $it min." }
                ?: "Elige cuándo pausar la música.",
            style = SettingsDetailStyle, color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        listOf(5, 10, 15, 30, 60).forEach { minutes ->
            Text(
                "Pausar en $minutes minutos", style = SettingsTitleStyle, color = cs.onSurface,
                modifier = Modifier.fillMaxWidth().clip(Ds.RowShape)
                    .clickable(role = Role.Button, onClickLabel = "Pausar la música en $minutes minutos") {
                        onStart(minutes)
                    }
                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
        TextButton(onClick = onCancel, enabled = remainingMinutes != null) {
            Text(
                if (remainingMinutes != null) "Cancelar temporizador" else "Sin temporizador activo",
                color = if (remainingMinutes != null) cs.primary else cs.onSurfaceVariant,
                style = SettingsDetailStyle
            )
        }
    }
}

@Composable
internal fun SettingsAboutContent(version: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Melodia funciona sin cuenta y sin anuncios. Tus listas y favoritos se guardan en este dispositivo.",
            style = SettingsDetailStyle, color = cs.onSurfaceVariant
        )
        Text(
            "Las funciones en línea envían búsquedas, títulos o artistas a servicios externos. La app también puede buscar carátulas y metadatos de la biblioteca automáticamente.",
            style = SettingsDetailStyle, color = cs.onSurfaceVariant
        )
        Text("Servicios y fuentes", style = SettingsTitleStyle, color = cs.onSurface)
        Text("YouTube: búsqueda y audio", style = SettingsDetailStyle, color = cs.onSurfaceVariant)
        Text("LRCLIB: letras", style = SettingsDetailStyle, color = cs.onSurfaceVariant)
        Text("iTunes (Apple): carátulas y metadatos", style = SettingsDetailStyle, color = cs.onSurfaceVariant)
        Text("Archivos del dispositivo: audio y etiquetas", style = SettingsDetailStyle, color = cs.onSurfaceVariant)
        Text("Versión $version", style = SettingsDetailStyle, color = cs.onSurfaceVariant)
    }
}

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
        containerColor = cs.surface,
        tonalElevation = 0.dp,
        title = { Text("Carpetas excluidas", style = Ds.Title, color = cs.onSurface) },
        text = { ExcludedFoldersContent(paths, excluded, onToggle = vm::toggleExcludedFolder) },
        confirmButton = { TextButton(onClick = onClose) { Text("Listo", color = cs.primary) } }
    )
}

@Composable
internal fun ExcludedFoldersContent(
    paths: List<String>,
    excluded: Set<String>,
    onToggle: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "explicacion") {
            Text(
                "Marca las carpetas que no quieres en tu biblioteca. Desmárcalas para volver a incluirlas.",
                style = SettingsDetailStyle, color = cs.onSurfaceVariant
            )
        }
        if (paths.isEmpty()) {
            item(key = "vacio") {
                Text("Todavía no hay carpetas para mostrar.", style = SettingsDetailStyle, color = cs.onSurfaceVariant)
            }
        } else {
            items(paths, key = { "ex-$it" }) { path ->
                val isExcluded = path in excluded
                Row(
                    Modifier.fillMaxWidth().clip(Ds.RowShape)
                        .toggleable(value = isExcluded, role = Role.Checkbox, onValueChange = { onToggle(path) })
                        .semantics { stateDescription = if (isExcluded) "Excluida" else "Incluida" }
                        .heightIn(min = 64.dp).padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            java.io.File(path).name.ifBlank { path },
                            style = SettingsTitleStyle, color = cs.onSurface
                        )
                        Text(path, style = SettingsDetailStyle, color = cs.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Checkbox(checked = isExcluded, onCheckedChange = null)
                }
            }
        }
    }
}

package com.melodia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melodia.app.ui.ds.EmptyState

/** Diferencia falta de permiso de biblioteca realmente vacía. */
@Composable
fun LibraryEmptyContent(
    hasPermission: Boolean, onGrant: () -> Unit, onSettings: () -> Unit,
    onReload: () -> Unit, onOnline: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(Icons.Default.LibraryMusic,
            if (hasPermission) "Tu biblioteca está vacía" else "Tu música, en un solo lugar",
            if (hasPermission) "No encontramos archivos de música. Revisa tus carpetas o descubre un tema en Online."
            else "Permite el acceso al audio para mostrar las canciones que ya están en tu teléfono.")
        Button(onClick = if (hasPermission) onReload else onGrant,
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (hasPermission) "Actualizar biblioteca" else "Conceder permiso de audio")
        }
        TextButton(onClick = if (hasPermission) onOnline else onSettings,
            modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (hasPermission) "Buscar en Online" else "Abrir ajustes de la app")
        }
    }
}

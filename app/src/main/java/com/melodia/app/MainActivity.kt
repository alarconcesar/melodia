package com.melodia.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.melodia.app.ui.screens.MelodiaScreen
import com.melodia.app.ui.theme.MelodiaTheme
import com.melodia.app.ui.theme.loadUiPrefs

class MainActivity : ComponentActivity() {
    private lateinit var vm: MainViewModel
    private val audioPerm =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    // Un solo diálogo con todo lo pendiente (antes eran 3 launchers en el
    // mismo frame = cascada de diálogos).
    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        try { if (res[audioPerm] == true && ::vm.isInitialized) vm.reload() } catch (_: Exception) {}
    }
    override fun onDestroy() {
        try { com.melodia.app.perf.FrameMonitor.stop() } catch (_: Exception) {}
        super.onDestroy()
    }


    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.melodia.app.perf.FrameMonitor.start()
        // Pantalla completa real: el fondo y el blur llegan hasta los bordes
        try { enableEdgeToEdge() } catch (_: Exception) {}
        val app = application as MelodiaApp
        // ── Red de seguridad: si el arranque anterior murió, el detalle va primero.
        //    Es Views puro, así que se ve aunque el fallo esté dentro de Compose.
        CrashScreen.leer(this)?.let { trace ->
            setContentView(CrashScreen.build(this, trace))
            return
        }
        app.containerError?.let { err ->
            setContentView(CrashScreen.build(this, err))
            return
        }
        loadUiPrefs(this)
        val c = app.appContainer
        vm = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return c.makeMainViewModel() as T
                }
            }
        )[MainViewModel::class.java]
        val faltantes = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, audioPerm) != PackageManager.PERMISSION_GRANTED) add(audioPerm)
            // Android 13+: permiso para mostrar la notificación de reproducción
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
            // Android 8 y anteriores: permiso de escritura para guardar descargas
            if (Build.VERSION.SDK_INT < 29 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (faltantes.isNotEmpty()) perms.launch(faltantes.toTypedArray())
        setContent { MelodiaTheme { MelodiaScreen(vm) } }
        // Si la app ya arrancó bien 8 s, el crash previo quedó atrás: limpiar el rastro.
        // Con el ciclo de vida (no Handler): si se destruye antes, no la retiene.
        lifecycleScope.launch {
            try {
                delay(8_000L)
                try { CrashScreen.borrar(this@MainActivity) } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        // Si se concedió el permiso desde Ajustes, recargar biblioteca.
        // Con guard: en la ruta CrashScreen vm no existe y esto crasheaba.
        try {
            if (!::vm.isInitialized) return
            if (ContextCompat.checkSelfPermission(this, audioPerm) == PackageManager.PERMISSION_GRANTED
                && vm.songs.value.isEmpty()
            ) {
                vm.reload()
            }
        } catch (_: Exception) {}
    }
}

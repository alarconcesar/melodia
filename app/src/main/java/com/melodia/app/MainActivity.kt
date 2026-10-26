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
import com.melodia.app.ui.screens.MelodiaScreen
import com.melodia.app.ui.theme.MelodiaTheme
import com.melodia.app.ui.theme.loadUiPrefs

class MainActivity : ComponentActivity() {
    private lateinit var vm: MainViewModel
    private val audioPerm =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private val perm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.reload()
    }
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val writePerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

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
                    return MainViewModel(c.musicPlayer, c.musicRepository, c.playlistRepo, c.statsRepo, c.sleepTimer, c.ytRepo, c.dlManager, c.lyricsRepo, c.artRepo, c.queueStore) as T
                }
            }
        )[MainViewModel::class.java]
        if (ContextCompat.checkSelfPermission(this, audioPerm) != PackageManager.PERMISSION_GRANTED) {
            perm.launch(audioPerm)
        }
        // Android 13+: permiso para mostrar la notificación de reproducción
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Android 8 y anteriores: permiso de escritura para guardar descargas
        if (Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            writePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        setContent { MelodiaTheme { MelodiaScreen(vm) } }
        // Si la app ya arrancó bien 8 s, el crash previo quedó atrás: limpiar el rastro
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { try { CrashScreen.borrar(this) } catch (_: Exception) {} }, 8_000L
        )
    }

    override fun onResume() {
        super.onResume()
        // Si se concedió el permiso desde Ajustes, recargar biblioteca
        if (ContextCompat.checkSelfPermission(this, audioPerm) == PackageManager.PERMISSION_GRANTED
            && vm.songs.value.isEmpty()
        ) {
            vm.reload()
        }
    }
}

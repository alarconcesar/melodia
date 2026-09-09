package com.melodia.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        loadUiPrefs(this)
        val app = application as MelodiaApp
        val c = app.appContainer
        vm = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(c.musicPlayer, c.musicRepository, c.playlistRepo, c.statsRepo, c.sleepTimer, c.ytRepo, c.dlManager, c.lyricsRepo, c.artRepo) as T
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

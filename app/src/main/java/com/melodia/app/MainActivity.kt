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

class MainActivity : ComponentActivity() {
    private lateinit var vm: MainViewModel
    private val audioPerm =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private val perm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.reload()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MelodiaApp
        val c = app.appContainer
        vm = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(c.musicPlayer, c.musicRepository, c.playlistRepo, c.sleepTimer) as T
                }
            }
        )[MainViewModel::class.java]
        if (ContextCompat.checkSelfPermission(this, audioPerm) != PackageManager.PERMISSION_GRANTED) {
            perm.launch(audioPerm)
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

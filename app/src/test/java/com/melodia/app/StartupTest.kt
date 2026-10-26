package com.melodia.app

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.ui.screens.MelodiaScreen
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Compone la pantalla completa en la JVM para cazar errores de arranque sin dispositivo. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class StartupTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun laPantallaAbre() {
        val app = ApplicationProvider.getApplicationContext<Context>() as MelodiaApp
        val vm = app.appContainer.makeMainViewModel()
        rule.setContent { MelodiaTheme { MelodiaScreen(vm) } }
        rule.waitForIdle()
    }
}

package com.melodia.app

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melodia.app.ui.screens.PrivacyPolicyContent
import com.melodia.app.ui.theme.MelodiaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w320dp-h640dp-mdpi", application = Application::class)
class PrivacyPolicyTest {
    @get:Rule val rule = createComposeRule()
    private fun actualPolicy(): String = ApplicationProvider.getApplicationContext<Context>()
        .resources.openRawResource(R.raw.privacy_policy).bufferedReader().use { it.readText() }

    @Test fun elArtefactoIncluyeElTextoCompletoSinCorreoInventado() {
        val policy = actualPolicy()
        listOf("YouTube", "LRCLIB", "iTunes", "Consultas automáticas", "Copias", "Borrado").forEach {
            assertTrue("Falta $it en la política empaquetada", policy.contains(it, ignoreCase = true))
        }
        assertFalse(policy.contains("[tu correo", ignoreCase = true))
        assertFalse(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").containsMatchIn(policy))
    }

    @Test fun politicaCompletaSePuedeRecorrerYSusAccionesFuncionanConLetraGrande() {
        var closed = false
        var web = false
        val policy = actualPolicy()
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, 1.5f)) {
                MelodiaTheme {
                    Box(Modifier.fillMaxSize()) {
                        PrivacyPolicyContent(policy, { closed = true }, { web = true })
                    }
                }
            }
        }
        rule.onNodeWithText("Privacidad").assertIsDisplayed()
        listOf("Consultas automáticas", "Borrado, controles y derechos", "Cambios").forEach {
            rule.onNodeWithTag("privacy-list").performScrollToNode(hasText(it))
            rule.onNodeWithText(it).assertIsDisplayed()
        }
        rule.onNodeWithTag("privacy-list").performScrollToNode(hasText("Abrir versión web"))
        rule.onNodeWithText("Abrir versión web").performClick()
        rule.onNodeWithContentDescription("Volver").performClick()
        assertTrue(web)
        assertTrue(closed)
    }

    @Test fun destinatariosDeLaTablaSeRenderizanComoDatosNoComoPipes() {
        val policy = "# Política\n\n## Servicios\n\n| Destino | Datos | Finalidad |\n| --- | --- | --- |\n| Servicio de prueba | Consulta de prueba | Función de prueba |"
        rule.setContent { MelodiaTheme { PrivacyPolicyContent(policy, {}, {}) } }
        rule.onNodeWithText("Servicio de prueba").assertExists()
        rule.onNodeWithText("Consulta de prueba").assertExists()
        rule.onNodeWithText("Función de prueba").assertExists()
        rule.onNodeWithText("| --- | --- | --- |").assertDoesNotExist()
    }
}

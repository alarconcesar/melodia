package com.melodia.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.melodia.app.R
import com.melodia.app.ui.ds.MelodiaSpinner
import com.melodia.app.ui.ds.PageHeader
import com.melodia.app.ui.theme.Ds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MELODIA_PRIVACY_URL = "https://vnic-cesar.tailb6ee43.ts.net/melodia/privacidad/"

@Composable
fun PrivacyPolicySheet(onClose: () -> Unit) {
    val context = LocalContext.current
    val policy by produceState<String?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            try { context.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use { it.readText() } }
            catch (_: Exception) { "## No se pudo cargar el documento\nPuedes abrir la versión web para consultar la política." }
        }
    }
    Dialog(onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PrivacyPolicyContent(policy, onClose) {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MELODIA_PRIVACY_URL))) }
                catch (_: Exception) {
                    android.widget.Toast.makeText(context, "No se pudo abrir el navegador", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

private data class PolicyBlock(val type: String, val text: String, val details: List<Pair<String, String>> = emptyList())
private fun plainPolicy(value: String) = value.replace("**", "").replace("`", "")

/** Markdown limitado al documento propio: encabezados, párrafos, listas y tabla de destinatarios. */
private fun policyBlocks(source: String): List<PolicyBlock> {
    val result = mutableListOf<PolicyBlock>()
    val paragraph = mutableListOf<String>()
    var columns = emptyList<String>()
    fun flush() {
        if (paragraph.isNotEmpty()) {
            result += PolicyBlock("paragraph", plainPolicy(paragraph.joinToString(" ")))
            paragraph.clear()
        }
    }
    for (raw in source.lines()) {
        val line = raw.trim()
        when {
            line.startsWith("# ") -> { flush() } // El título ya vive en PageHeader.
            line.startsWith("## ") -> { flush(); columns = emptyList(); result += PolicyBlock("heading", line.removePrefix("## ")) }
            line.startsWith("|") -> {
                flush()
                val cells = line.trim('|').split('|').map { plainPolicy(it.trim()) }
                if (cells.all { it.isNotEmpty() && it.all { c -> c == '-' || c == ':' || c == ' ' } }) continue
                if (columns.isEmpty()) columns = cells
                else result += PolicyBlock("recipient", cells.first(), columns.zip(cells).drop(1))
            }
            line.startsWith("- ") -> { flush(); result += PolicyBlock("bullet", plainPolicy(line.removePrefix("- "))) }
            line.isBlank() -> flush()
            else -> paragraph += line
        }
    }
    flush()
    return result
}

/** Contenido puro y completo, también disponible sin conexión ni acceso a servicios externos. */
@Composable
fun PrivacyPolicyContent(policy: String?, onClose: () -> Unit, onOpenWeb: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val blocks = remember(policy) { policy?.let(::policyBlocks).orEmpty() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.padding(horizontal = 12.dp)) {
            PageHeader("Privacidad", "Texto completo", onClose)
        }
        if (policy == null) {
            Box(Modifier.padding(32.dp)) { MelodiaSpinner() }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("privacy-list"),
                contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(blocks) { _, block ->
                    when (block.type) {
                        "heading" -> Text(block.text, style = Ds.Title, color = cs.onSurface,
                            modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                        "bullet" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", style = Ds.Body, color = cs.onSurfaceVariant)
                            Text(block.text, style = Ds.Body, color = cs.onSurface, modifier = Modifier.weight(1f))
                        }
                        "recipient" -> Surface(shape = Ds.CardShape, color = cs.surface) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(block.text, style = Ds.Title, color = cs.onSurface)
                                block.details.forEach { (label, value) ->
                                    Text(label, style = Ds.Sub, color = cs.onSurfaceVariant)
                                    Text(value, style = Ds.Body, color = cs.onSurface)
                                }
                            }
                        }
                        else -> Text(block.text, style = Ds.Body, color = cs.onSurface)
                    }
                }
                item {
                    TextButton(onClick = onOpenWeb, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Abrir versión web", color = cs.primary)
                    }
                }
            }
        }
    }
}

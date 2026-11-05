package com.melodia.app

import android.app.Application
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.app.RemoteAction
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.melodia.app.data.MusicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadows.ShadowContentResolver

/** Provider controlado: prueba el flujo real del repositorio sin Room ni archivos del usuario. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, shadows = [DeleteConsentTest.ShadowRecoverableConsent::class])
class DeleteConsentTest {
    // Robolectric 4.11 no implementa el getter de esta excepción en API 29.
    // El shadow modela solo la acción autorizable; no simula un borrado exitoso.
    @Implements(className = "android.app.RecoverableSecurityException", minSdk = 29)
    class ShadowRecoverableConsent {
        private lateinit var action: RemoteAction
        @Implementation
        fun __constructor__(cause: Throwable, message: CharSequence, userAction: RemoteAction) {
            action = userAction
        }
        @Implementation
        fun getUserAction(): RemoteAction = action
    }

    private class AudioProvider : ContentProvider() {
        var allowed = false
        val deleted = mutableListOf<Long>()
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
        override fun getType(uri: Uri): String = "audio/mpeg"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            val id = uri.lastPathSegment!!.toLong()
            if (id in deleted) return 0
            if (!allowed) {
                if (Build.VERSION.SDK_INT >= 29) {
                    val ctx = requireNotNull(context)
                    val pending = PendingIntent.getBroadcast(ctx, 1,
                        Intent("com.melodia.app.TEST_DELETE_CONSENT").setPackage(ctx.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val action = RemoteAction(Icon.createWithResource(ctx, android.R.drawable.ic_menu_delete),
                        "Autorizar", "Autorizar archivo", pending)
                    throw RecoverableSecurityException(SecurityException("Sin permiso"), "Autoriza el archivo", action)
                }
                throw SecurityException("Sin permiso de escritura")
            }
            deleted += id
            return 1
        }
    }

    private fun setup(): Pair<MusicRepository, AudioProvider> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AudioProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = "media" })
        ShadowContentResolver.registerProviderInternal("media", provider)
        return MusicRepository(context) to provider
    }

    @Test fun android10DevuelveConsentimientoSinInvocarApi30() = runBlocking {
        val (repo, provider) = setup()
        val sender = repo.deleteSongs(listOf(10L))
        assertNotNull(sender)
        assertTrue(provider.deleted.isEmpty())
    }

    @Test fun android10ReintentaElBorradoDespuesDeAutorizar() = runBlocking {
        val (repo, provider) = setup()
        assertNotNull(repo.deleteSongs(listOf(10L, 11L)))
        provider.allowed = true // El resultado OK concede permiso, no realiza delete.
        assertNull(repo.deleteSongs(listOf(10L, 11L)))
        assertEquals(listOf(10L, 11L), provider.deleted)
    }

    @Test
    @Config(sdk = [24])
    fun android7NoCargaClaseApi29NiConfundePermisoDenegadoConExito() = runBlocking {
        val (repo, provider) = setup()
        var failure: Throwable? = null
        try { repo.deleteSongs(listOf(10L)) } catch (error: Throwable) { failure = error }
        assertTrue(failure is SecurityException)
        assertTrue(provider.deleted.isEmpty())
    }

    @Test fun listaVaciaNoPideConsentimiento() = runBlocking {
        val (repo, _) = setup()
        assertNull(repo.deleteSongs(emptyList()))
    }
}

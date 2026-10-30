package com.example.notally.ui.settings

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Web client ID del proyecto `notally` (tipo 3 en los OAuth clients).
 *
 * No es un secreto: es el `serverClientId` que Google usa para emitir el ID
 * token, y debe coincidir con el cliente web habilitado en Firebase Auth.
 */
internal const val CLIENTE_WEB_GOOGLE =
    "1068320990561-vuao4q2to2tnatua9q8bounj5gh0jac9.apps.googleusercontent.com"

/**
 * Abre el selector de cuentas de Google y devuelve el ID token.
 *
 * Devuelve `null` solo si el usuario cancela. Cualquier otro fallo (sin Play
 * Services, SHA no registrado, configuración incompleta) se propaga para que
 * la UI muestre el mensaje genérico. El token no se registra en logs.
 */
internal suspend fun obtenerIdTokenGoogle(contexto: Context): String? {
    val gestor = CredentialManager.create(contexto)
    val solicitud = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setServerClientId(CLIENTE_WEB_GOOGLE)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build(),
        )
        .build()

    val respuesta = try {
        gestor.getCredential(context = contexto, request = solicitud)
    } catch (_: GetCredentialCancellationException) {
        return null
    }

    val credencial = respuesta.credential
    if (
        credencial is CustomCredential &&
        credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return GoogleIdTokenCredential.createFrom(credencial.data).idToken
    }
    error("Credencial de Google inesperada")
}

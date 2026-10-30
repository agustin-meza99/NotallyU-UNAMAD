package com.example.notally.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Usuario autenticado, reducido a lo que la UI necesita. */
data class UsuarioFirebase(
    val uid: String,
    val email: String?,
    val displayName: String? = null,
    val photoUrl: String? = null,
)

/** Errores de autenticación ya traducidos a casos de dominio. */
enum class ErrorAuth {
    CREDENCIALES,
    EMAIL_INVALIDO,
    RED,
    DEMASIADOS_INTENTOS,
    EMAIL_EN_USO,
    PASSWORD_DEBIL,
    CUENTA_DESHABILITADA,
    PROVEEDOR_DESHABILITADO,
    GOOGLE,
    DESCONOCIDO,
}

/** Resultado explícito de iniciar sesión o crear una cuenta. */
sealed interface ResultadoAuth {
    data object Exito : ResultadoAuth
    data class Fallo(val error: ErrorAuth) : ResultadoAuth
}

/**
 * Sesión de Firebase Authentication con email y contraseña.
 *
 * No guarda contraseñas ni escribe credenciales en logs. El estado de sesión lo
 * mantiene Firebase Auth y se refleja en [usuario]; cerrar sesión no toca Room
 * ni Firestore.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    private val _usuario = MutableStateFlow(auth.currentUser?.aUsuario())
    val usuario: StateFlow<UsuarioFirebase?> = _usuario.asStateFlow()

    init {
        // El listener vive lo que vive el proceso: AuthRepository es un
        // singleton del AppContainer.
        auth.addAuthStateListener { instancia ->
            _usuario.value = instancia.currentUser?.aUsuario()
        }
    }

    suspend fun iniciarSesion(email: String, password: String): ResultadoAuth =
        ejecutarAuth {
            auth.signInWithEmailAndPassword(email.trim(), password).esperar()
        }

    suspend fun crearCuenta(email: String, password: String): ResultadoAuth =
        ejecutarAuth {
            // El nombre visible es opcional; no se pide en el formulario.
            auth.createUserWithEmailAndPassword(email.trim(), password).esperar()
        }

    /**
     * Canjea el ID token de Google por una sesión de Firebase.
     *
     * El token lo obtiene la UI con Credential Manager; aquí solo se usa para
     * construir la credencial y nunca se guarda ni se escribe en logs.
     */
    suspend fun iniciarConGoogle(idToken: String): ResultadoAuth =
        ejecutarAuth {
            val credencial = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credencial).esperar()
        }

    fun cerrarSesion() {
        auth.signOut()
    }

    private suspend fun ejecutarAuth(bloque: suspend () -> Unit): ResultadoAuth = try {
        bloque()
        ResultadoAuth.Exito
    } catch (error: FirebaseAuthException) {
        ResultadoAuth.Fallo(error.errorCode.aErrorAuth())
    } catch (_: FirebaseNetworkException) {
        ResultadoAuth.Fallo(ErrorAuth.RED)
    } catch (error: Exception) {
        ResultadoAuth.Fallo(
            if (error is java.io.IOException) ErrorAuth.RED else ErrorAuth.DESCONOCIDO,
        )
    }
}

private fun FirebaseUser.aUsuario(): UsuarioFirebase = UsuarioFirebase(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
)

private fun String.aErrorAuth(): ErrorAuth = when (this) {
    "ERROR_INVALID_EMAIL" -> ErrorAuth.EMAIL_INVALIDO
    "ERROR_WRONG_PASSWORD",
    "ERROR_INVALID_CREDENTIALS",
    "ERROR_USER_NOT_FOUND",
    -> ErrorAuth.CREDENCIALES

    "ERROR_NETWORK_REQUEST_FAILED" -> ErrorAuth.RED
    "ERROR_TOO_MANY_REQUESTS" -> ErrorAuth.DEMASIADOS_INTENTOS
    "ERROR_EMAIL_ALREADY_IN_USE" -> ErrorAuth.EMAIL_EN_USO
    "ERROR_WEAK_PASSWORD" -> ErrorAuth.PASSWORD_DEBIL
    "ERROR_USER_DISABLED" -> ErrorAuth.CUENTA_DESHABILITADA
    "ERROR_OPERATION_NOT_ALLOWED" -> ErrorAuth.PROVEEDOR_DESHABILITADO
    else -> ErrorAuth.DESCONOCIDO
}

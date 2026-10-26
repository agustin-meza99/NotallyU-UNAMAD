package com.example.notally.data.aula

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Credenciales y metadatos de la última importación del Aula Virtual. */
data class AulaCredenciales(
    val usuario: String,
    val clave: String,
    val conectado: Boolean,
    val ultimaImportacion: Long?,
)

/**
 * Guarda el usuario y la contraseña del Aula Virtual cifrados en el
 * dispositivo.
 *
 * Usa [EncryptedSharedPreferences] con una [MasterKey] AES-256-GCM respaldada
 * por Android Keystore. El archivo de preferencias queda fuera de las reglas
 * de backup y de transferencia entre dispositivos: la clave no sale del
 * dispositivo, así que restaurar el archivo en otro equipo sería inútil.
 *
 * Ningún valor de esta clase se escribe en logs ni se envía a Firestore.
 */
class AulaCredencialesStore(context: Context) {

    private val contexto = context.applicationContext

    private val preferencias: SharedPreferences by lazy { abrirPreferencias() }

    private fun abrirPreferencias(): SharedPreferences {
        val claveMaestra = MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            contexto,
            NOMBRE_ARCHIVO,
            claveMaestra,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    suspend fun guardar(usuario: String, clave: String) = withContext(Dispatchers.IO) {
        preferencias.edit()
            .putString(CLAVE_USUARIO, usuario)
            .putString(CLAVE_CONTRASENA, clave)
            .putBoolean(CLAVE_CONECTADO, true)
            .apply()
    }

    /** Marca la conexión como vigente y registra el instante de la importación. */
    suspend fun marcarImportacion(ahora: Long) = withContext(Dispatchers.IO) {
        preferencias.edit()
            .putBoolean(CLAVE_CONECTADO, true)
            .putLong(CLAVE_ULTIMA_IMPORTACION, ahora)
            .apply()
    }

    suspend fun leer(): AulaCredenciales? = withContext(Dispatchers.IO) {
        val usuario = preferencias.getString(CLAVE_USUARIO, null)
        val clave = preferencias.getString(CLAVE_CONTRASENA, null)
        if (usuario.isNullOrBlank() || clave.isNullOrBlank()) {
            null
        } else {
            AulaCredenciales(
                usuario = usuario,
                clave = clave,
                conectado = preferencias.getBoolean(CLAVE_CONECTADO, false),
                ultimaImportacion = preferencias
                    .takeIf { it.contains(CLAVE_ULTIMA_IMPORTACION) }
                    ?.getLong(CLAVE_ULTIMA_IMPORTACION, 0L),
            )
        }
    }

    /** Borra credenciales y metadatos. No lanza si el archivo no existe. */
    suspend fun limpiar() = withContext(Dispatchers.IO) {
        preferencias.edit().clear().apply()
    }

    private companion object {
        const val NOMBRE_ARCHIVO = "notally_aula_credenciales"
        const val CLAVE_USUARIO = "usuario"
        const val CLAVE_CONTRASENA = "clave"
        const val CLAVE_CONECTADO = "conectado"
        const val CLAVE_ULTIMA_IMPORTACION = "ultima_importacion"
    }
}

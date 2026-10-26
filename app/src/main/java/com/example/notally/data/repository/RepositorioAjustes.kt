package com.example.notally.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.notally.data.aTexto
import com.example.notally.data.model.Ajustes
import com.example.notally.data.model.ModoTema
import com.example.notally.data.modoTemaDesdeTexto
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val NOMBRE_ARCHIVO_AJUSTES = "notally_settings"

/** DataStore de preferencias de Notally, ligado al contexto de la aplicación. */
val Context.dataStoreAjustesNotally: DataStore<Preferences> by preferencesDataStore(
    name = NOMBRE_ARCHIVO_AJUSTES,
)

class RepositorioAjustes(
    private val dataStore: DataStore<Preferences>,
) {

    val ajustes: Flow<Ajustes> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias ->
            Ajustes(
                modoTema = preferencias[Claves.MODO_TEMA]
                    ?.let { guardado -> runCatching { modoTemaDesdeTexto(guardado) }.getOrNull() }
                    ?: ModoTema.SISTEMA,
                usarColorDinamico = preferencias[Claves.USAR_COLOR_DINAMICO] ?: false,
                pestanaPorDefecto = preferencias[Claves.PESTANA_POR_DEFECTO]
                    ?.takeIf { it in Ajustes.PESTANAS_VALIDAS }
                    ?: Ajustes.PESTANA_HOY,
                semanaEmpiezaLunes = preferencias[Claves.SEMANA_EMPIEZA_LUNES] ?: true,
                notificacionesActivas = preferencias[Claves.NOTIFICACIONES_ACTIVAS] ?: true,
            )
        }

    suspend fun actual(): Ajustes = ajustes.first()

    /** `true` cuando el seed de demo ya se ejecutó (o se decidió no ejecutar). */
    suspend fun semillaCompletada(): Boolean = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[Claves.SEMILLA_COMPLETADA] ?: false }
        .first()

    /**
     * Marca el seed como hecho. Vive en DataStore (no en Room) para no requerir
     * una migración de esquema.
     */
    suspend fun marcarSemillaCompletada() {
        dataStore.edit { it[Claves.SEMILLA_COMPLETADA] = true }
    }

    suspend fun cambiarModoTema(modo: ModoTema) {
        dataStore.edit { it[Claves.MODO_TEMA] = modo.aTexto() }
    }

    suspend fun cambiarColorDinamico(activo: Boolean) {
        dataStore.edit { it[Claves.USAR_COLOR_DINAMICO] = activo }
    }

    suspend fun cambiarPestanaPorDefecto(pestana: String) {
        require(pestana in Ajustes.PESTANAS_VALIDAS) { "Pestaña desconocida: $pestana" }
        dataStore.edit { it[Claves.PESTANA_POR_DEFECTO] = pestana }
    }

    suspend fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        dataStore.edit { it[Claves.SEMANA_EMPIEZA_LUNES] = activo }
    }

    suspend fun cambiarNotificacionesActivas(activo: Boolean) {
        dataStore.edit { it[Claves.NOTIFICACIONES_ACTIVAS] = activo }
    }

    suspend fun actualizar(ajustes: Ajustes) {
        require(ajustes.pestanaPorDefecto in Ajustes.PESTANAS_VALIDAS) {
            "Pestaña desconocida: ${ajustes.pestanaPorDefecto}"
        }
        dataStore.edit { preferencias ->
            preferencias[Claves.MODO_TEMA] = ajustes.modoTema.aTexto()
            preferencias[Claves.USAR_COLOR_DINAMICO] = ajustes.usarColorDinamico
            preferencias[Claves.PESTANA_POR_DEFECTO] = ajustes.pestanaPorDefecto
            preferencias[Claves.SEMANA_EMPIEZA_LUNES] = ajustes.semanaEmpiezaLunes
            preferencias[Claves.NOTIFICACIONES_ACTIVAS] = ajustes.notificacionesActivas
        }
    }

    private object Claves {
        val SEMILLA_COMPLETADA = booleanPreferencesKey("seed_done")
        val MODO_TEMA = stringPreferencesKey("theme_mode")
        val USAR_COLOR_DINAMICO = booleanPreferencesKey("use_dynamic_color")
        val PESTANA_POR_DEFECTO = stringPreferencesKey("default_tab")
        val SEMANA_EMPIEZA_LUNES = booleanPreferencesKey("week_starts_monday")
        val NOTIFICACIONES_ACTIVAS = booleanPreferencesKey("notifications_enabled")
    }
}

package com.example.notally.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.notally.data.model.Settings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val NOMBRE_ARCHIVO_SETTINGS = "notally_settings"

/** DataStore de preferencias de Notally, ligado al contexto de la aplicación. */
val Context.dataStoreSettingsNotally: DataStore<Preferences> by preferencesDataStore(
    name = NOMBRE_ARCHIVO_SETTINGS,
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<Settings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias ->
            Settings(
                pestanaPorDefecto = preferencias[Claves.PESTANA_POR_DEFECTO]
                    ?.takeIf { it in Settings.PESTANAS_VALIDAS }
                    ?: Settings.PESTANA_HOY,
                semanaEmpiezaLunes = preferencias[Claves.SEMANA_EMPIEZA_LUNES] ?: true,
                notificacionesActivas = preferencias[Claves.NOTIFICACIONES_ACTIVAS] ?: true,
            )
        }

    suspend fun actual(): Settings = settings.first()

    suspend fun cambiarPestanaPorDefecto(pestana: String) {
        require(pestana in Settings.PESTANAS_VALIDAS) { "Pestaña desconocida: $pestana" }
        dataStore.edit { it[Claves.PESTANA_POR_DEFECTO] = pestana }
    }

    suspend fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        dataStore.edit { it[Claves.SEMANA_EMPIEZA_LUNES] = activo }
    }

    suspend fun cambiarNotificacionesActivas(activo: Boolean) {
        dataStore.edit { it[Claves.NOTIFICACIONES_ACTIVAS] = activo }
    }

    suspend fun actualizar(settings: Settings) {
        require(settings.pestanaPorDefecto in Settings.PESTANAS_VALIDAS) {
            "Pestaña desconocida: ${settings.pestanaPorDefecto}"
        }
        dataStore.edit { preferencias ->
            preferencias[Claves.PESTANA_POR_DEFECTO] = settings.pestanaPorDefecto
            preferencias[Claves.SEMANA_EMPIEZA_LUNES] = settings.semanaEmpiezaLunes
            preferencias[Claves.NOTIFICACIONES_ACTIVAS] = settings.notificacionesActivas
        }
    }

    private object Claves {
        val PESTANA_POR_DEFECTO = stringPreferencesKey("default_tab")
        val SEMANA_EMPIEZA_LUNES = booleanPreferencesKey("week_starts_monday")
        val NOTIFICACIONES_ACTIVAS = booleanPreferencesKey("notifications_enabled")
    }
}

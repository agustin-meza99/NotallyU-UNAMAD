package com.example.notally.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.notally.data.model.Settings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
                semanaEmpiezaLunes = preferencias[Claves.SEMANA_EMPIEZA_LUNES] ?: true,
            )
        }

    suspend fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        dataStore.edit { it[Claves.SEMANA_EMPIEZA_LUNES] = activo }
    }

    private object Claves {
        val SEMANA_EMPIEZA_LUNES = booleanPreferencesKey("week_starts_monday")
    }
}

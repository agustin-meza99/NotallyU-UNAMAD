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
import com.example.notally.data.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val SETTINGS_FILE_NAME = "notally_settings"

/** DataStore de preferencias de Notally, ligado al contexto de la aplicación. */
val Context.notallySettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_FILE_NAME,
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<Settings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            Settings(
                themeMode = preferences[Keys.THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                useDynamicColor = preferences[Keys.USE_DYNAMIC_COLOR] ?: false,
                defaultTab = preferences[Keys.DEFAULT_TAB]
                    ?.takeIf { it in Settings.VALID_TABS }
                    ?: Settings.TAB_TODAY,
                weekStartsMonday = preferences[Keys.WEEK_STARTS_MONDAY] ?: true,
                notificationsEnabled = preferences[Keys.NOTIFICATIONS_ENABLED] ?: true,
            )
        }

    suspend fun current(): Settings = settings.first()

    /** `true` cuando el seed de demo ya se ejecutó (o se decidió no ejecutar). */
    suspend fun isSeedDone(): Boolean = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { it[Keys.SEED_DONE] ?: false }
        .first()

    /**
     * Marca el seed como hecho. Vive en DataStore (no en Room) para no requerir
     * una migración de esquema.
     */
    suspend fun markSeedDone() {
        dataStore.edit { it[Keys.SEED_DONE] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultTab(tab: String) {
        require(tab in Settings.VALID_TABS) { "Pestaña desconocida: $tab" }
        dataStore.edit { it[Keys.DEFAULT_TAB] = tab }
    }

    suspend fun setWeekStartsMonday(enabled: Boolean) {
        dataStore.edit { it[Keys.WEEK_STARTS_MONDAY] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun update(settings: Settings) {
        require(settings.defaultTab in Settings.VALID_TABS) {
            "Pestaña desconocida: ${settings.defaultTab}"
        }
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = settings.themeMode.name
            preferences[Keys.USE_DYNAMIC_COLOR] = settings.useDynamicColor
            preferences[Keys.DEFAULT_TAB] = settings.defaultTab
            preferences[Keys.WEEK_STARTS_MONDAY] = settings.weekStartsMonday
            preferences[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
        }
    }

    private object Keys {
        val SEED_DONE = booleanPreferencesKey("seed_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val DEFAULT_TAB = stringPreferencesKey("default_tab")
        val WEEK_STARTS_MONDAY = booleanPreferencesKey("week_starts_monday")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}

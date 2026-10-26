package com.example.notally.data.model

/** Modo de tema elegido por el usuario. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Preferencias de la aplicación, persistidas en DataStore.
 *
 * [defaultTab] usa los valores de las constantes [TAB_TODAY], [TAB_UPCOMING],
 * [TAB_ALL] y [TAB_SUBJECTS].
 */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val defaultTab: String = TAB_TODAY,
    val weekStartsMonday: Boolean = true,
    val notificationsEnabled: Boolean = true,
) {
    companion object {
        const val TAB_TODAY = "today"
        const val TAB_UPCOMING = "upcoming"
        const val TAB_ALL = "all"
        const val TAB_SUBJECTS = "subjects"

        val VALID_TABS = setOf(TAB_TODAY, TAB_UPCOMING, TAB_ALL, TAB_SUBJECTS)
    }
}

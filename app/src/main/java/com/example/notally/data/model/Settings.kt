package com.example.notally.data.model

/**
 * Preferencias de la aplicación, persistidas en DataStore.
 *
 * Los valores persistidos de [pestanaPorDefecto] usan las constantes
 * [PESTANA_HOY], [PESTANA_PROXIMAS], [PESTANA_TODAS] y [PESTANA_CURSOS].
 */
data class Settings(
    val pestanaPorDefecto: String = PESTANA_HOY,
    val semanaEmpiezaLunes: Boolean = true,
    val notificacionesActivas: Boolean = true,
) {
    companion object {
        const val PESTANA_HOY = "today"
        const val PESTANA_PROXIMAS = "upcoming"
        const val PESTANA_TODAS = "all"
        const val PESTANA_CURSOS = "subjects"

        val PESTANAS_VALIDAS =
            setOf(PESTANA_HOY, PESTANA_PROXIMAS, PESTANA_TODAS, PESTANA_CURSOS)
    }
}

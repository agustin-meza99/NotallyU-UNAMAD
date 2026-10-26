package com.example.notally.data.model

/** Modo de tema elegido por el usuario. */
enum class ModoTema {
    SISTEMA,
    CLARO,
    OSCURO,
}

/**
 * Preferencias de la aplicación, persistidas en DataStore.
 *
 * Los valores persistidos de [pestanaPorDefecto] usan las constantes
 * [PESTANA_HOY], [PESTANA_PROXIMAS], [PESTANA_TODAS] y [PESTANA_MATERIAS].
 */
data class Ajustes(
    val modoTema: ModoTema = ModoTema.SISTEMA,
    val pestanaPorDefecto: String = PESTANA_HOY,
    val semanaEmpiezaLunes: Boolean = true,
    val notificacionesActivas: Boolean = true,
) {
    companion object {
        const val PESTANA_HOY = "today"
        const val PESTANA_PROXIMAS = "upcoming"
        const val PESTANA_TODAS = "all"
        const val PESTANA_MATERIAS = "subjects"

        val PESTANAS_VALIDAS =
            setOf(PESTANA_HOY, PESTANA_PROXIMAS, PESTANA_TODAS, PESTANA_MATERIAS)
    }
}

package com.example.notally.data.model

/** Modo visual de la aplicación (ISO/IEC 25010 — Usabilidad y Portabilidad). */
enum class TemaApp {
    SISTEMA,
    CLARO,
    OSCURO,
}

/**
 * Preferencias de la aplicación, persistidas en DataStore.
 */
data class Settings(
    val semanaEmpiezaLunes: Boolean = true,
    /** Última subida completada a Firestore, en epoch ms. */
    val ultimaSincronizacionFirestore: Long? = null,
    /** Tema institucional UNAMAD: sigue al sistema, claro u oscuro. */
    val tema: TemaApp = TemaApp.SISTEMA,
)

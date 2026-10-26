package com.example.notally.data.model

/**
 * Preferencias de la aplicación, persistidas en DataStore.
 */
data class Settings(
    val semanaEmpiezaLunes: Boolean = true,
    /** Última subida completada a Firestore, en epoch ms. */
    val ultimaSincronizacionFirestore: Long? = null,
)

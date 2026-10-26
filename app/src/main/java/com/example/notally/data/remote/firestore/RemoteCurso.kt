package com.example.notally.data.remote.firestore

/**
 * Representacion plana de `users/{uid}/courses/{courseId}`.
 *
 * Contrato (ver `firestore.rules`, funcion `validCurso`):
 * - `name`: string, 1..120 caracteres.
 * - `colorArgb`: number (ARGB opaco).
 * - `professor`: string de hasta 120 caracteres o null.
 * - `credits`: number >= 0 o null.
 * - `createdAt` / `updatedAt`: Timestamp del servidor.
 * - `deletedAt`: Timestamp o null (tombstone de borrado suave).
 *
 * Los timestamps se modelan como `Long?` en epoch ms para mantener la capa
 * pura y testeable en JVM. `null` significa "no hay valor local; el borde
 * Firestore debe usar `FieldValue.serverTimestamp()`" (solo valido en create;
 * en update se conserva el Timestamp remoto conocido, porque las reglas exigen
 * `createdAt == resource.data.createdAt`).
 *
 * El `docId` no forma parte del documento: es el id del path. Por eso esta
 * clase no incluye `id`.
 */
data class RemoteCurso(
    val name: String,
    val colorArgb: Long,
    val professor: String? = null,
    val credits: Int? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

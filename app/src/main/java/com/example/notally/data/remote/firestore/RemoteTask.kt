package com.example.notally.data.remote.firestore

/**
 * Representacion plana de `users/{uid}/tasks/{taskId}`.
 *
 * Contrato (ver `firestore.rules`, funcion `validTask`):
 * - `title`: string, 1..300 caracteres.
 * - `notes`: string, 0..20000 caracteres.
 * - `subjectId`: string de hasta 128 caracteres o null.
 * - `dueAt`: number (epoch ms) o null.
 * - `priority`: string, uno de `LOW`, `MEDIUM`, `HIGH`.
 * - `status`: string, uno de `PENDING`, `IN_PROGRESS`, `DONE`.
 * - `subtasks`: lista embebida de hasta 100 [RemoteSubtask].
 * - `createdAt` / `updatedAt`: Timestamp del servidor.
 * - `completedAt`: number (epoch ms) o null.
 * - `deletedAt`: Timestamp o null (tombstone de borrado suave).
 *
 * Los timestamps se modelan como `Long?` en epoch ms para mantener la capa
 * pura y testeable en JVM (sin `com.google.firebase.Timestamp`). `null` en
 * `createdAt`/`updatedAt` significa "usar `FieldValue.serverTimestamp()` en el
 * borde"; en update el borde debe conservar el Timestamp remoto conocido porque
 * las reglas exigen `createdAt == resource.data.createdAt`.
 *
 * No hay campo `isArchived`: las reglas no lo permiten (ver
 * `docs/ARCHITECTURE.md` seccion 5.3). El mapper falla de forma explicita si
 * se intenta subir una tarea archivada.
 *
 * El `docId` no forma parte del documento: es el id del path. Por eso esta
 * clase no incluye `id`.
 */
data class RemoteTask(
    val title: String,
    val priority: String,
    val status: String,
    val notes: String = "",
    val subjectId: String? = null,
    val dueAt: Long? = null,
    val subtasks: List<RemoteSubtask> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
)

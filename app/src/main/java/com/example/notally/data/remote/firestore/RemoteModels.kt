package com.example.notally.data.remote.firestore


/**
 * Representacion plana de `users/{uid}/tasks/{taskId}`.
 *
 * Contrato (ver `firestore.rules`, funcion `validTask`):
 * - `title`: string, 1..300 caracteres.
 * - `notes`: string, 0..20000 caracteres.
 * - `courseId`: string de hasta 128 caracteres o null.
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
    val courseId: String? = null,
    val dueAt: Long? = null,
    val subtasks: List<RemoteSubtask> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
)

/**
 * Representacion plana de `users/{uid}/courses/{courseId}`.
 *
 * Contrato (ver `firestore.rules`, funcion `validCurso`):
 * - `name`: string, 1..120 caracteres.
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
    val professor: String? = null,
    val credits: Int? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
)

/**
 * Subtarea embebida en `users/{uid}/tasks/{taskId}`.
 *
 * Contrato (ver `firestore.rules`, funcion `validSubtask`):
 * - `id`: string, 1..128 caracteres.
 * - `title`: string, 0..300 caracteres.
 * - `isDone`: bool.
 * - `position`: number (orden dentro de la tarea).
 *
 * Firestore no permite subcolecciones dentro de la lista; las subtareas viajan
 * embebidas en el documento de la tarea (maximo 100 por tarea).
 *
 * Es un tipo Kotlin puro a proposito: no importa `com.google.firebase.*` para
 * poder probarse en JVM sin Android ni red.
 */
data class RemoteSubtask(
    val id: String,
    val title: String,
    val isDone: Boolean,
    val position: Int,
)

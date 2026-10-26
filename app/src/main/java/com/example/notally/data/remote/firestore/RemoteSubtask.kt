package com.example.notally.data.remote.firestore

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

package com.example.notally.data.firebase

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Espera un [Task] de Google Play Services sin depender de
 * `kotlinx-coroutines-play-services`.
 *
 * Si la corrutina se cancela, el `Task` subyacente sigue su curso, pero ya no
 * se entrega su resultado.
 */
internal suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
    addOnCompleteListener { tarea ->
        if (!continuacion.isActive) return@addOnCompleteListener
        val error = tarea.exception
        if (tarea.isSuccessful && error == null) {
            continuacion.resume(tarea.result)
        } else {
            continuacion.resumeWithException(
                error ?: IllegalStateException("La tarea de Firebase falló sin excepción."),
            )
        }
    }
}

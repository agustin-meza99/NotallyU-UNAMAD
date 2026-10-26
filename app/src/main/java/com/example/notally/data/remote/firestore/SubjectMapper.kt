package com.example.notally.data.remote.firestore

import com.example.notally.data.model.Materia

/** Limites replicados de `firestore.rules` para materias. */
object SubjectLimits {
    const val MAX_NAME = 120
    const val MAX_PROFESSOR = 120
}

/**
 * Convierte una materia local en su forma remota.
 *
 * El docId remoto es `id.toString()` hoy (ver `docs/ARCHITECTURE.md` 5.4).
 * `Materia` no guarda `updatedAt` en Room, asi que por defecto se reutiliza
 * `createdAt`; el SyncEngine debe pasar el `updatedAt` real de su metadata de
 * last-write-wins cuando lo tenga.
 *
 * @throws IllegalArgumentException si rompe los limites de las reglas.
 */
fun Materia.toRemote(updatedAtMs: Long = creadoEn): RemoteSubject {
    val remote = RemoteSubject(
        name = nombre,
        colorArgb = color,
        professor = profesor,
        credits = creditos,
        createdAt = creadoEn,
        updatedAt = updatedAtMs,
        // Room hace borrado duro; una Materia local nunca es un tombstone.
        deletedAt = null,
    )
    validateRemoteSubject(remote)
    return remote
}

/**
 * Convierte una materia remota en local.
 *
 * Perdida documentada: `updatedAt` y `deletedAt` no existen en `Materia` de
 * Room. `deletedAt` debe filtrarlo el SyncEngine (aqui se rechaza el tombstone)
 * y `updatedAt` solo interesa para last-write-wins, que vive en la capa de sync.
 *
 * @param localId id autogenerado de Room. `0` deja que Room lo asigne.
 * @param now reloj de respaldo si el documento no trae `createdAt`.
 * @throws IllegalArgumentException si el documento es un tombstone.
 */
fun RemoteSubject.toLocal(
    localId: Long = 0L,
    now: Long = System.currentTimeMillis(),
): Materia {
    require(deletedAt == null) {
        "RemoteSubject es un tombstone (deletedAt=$deletedAt): el SyncEngine debe " +
            "aplicar el borrado local y no mapearlo como materia viva."
    }
    validateRemoteSubject(this)
    return Materia(
        id = localId,
        nombre = name,
        color = colorArgb,
        profesor = professor,
        creditos = credits,
        creadoEn = createdAt ?: now,
    )
}

internal fun validateRemoteSubject(subject: RemoteSubject) {
    require(subject.name.length in 1..SubjectLimits.MAX_NAME) {
        "name debe tener 1..${SubjectLimits.MAX_NAME} caracteres (tiene ${subject.name.length})."
    }
    require(subject.professor == null || subject.professor.length <= SubjectLimits.MAX_PROFESSOR) {
        "professor supera ${SubjectLimits.MAX_PROFESSOR} caracteres."
    }
    require(subject.credits == null || subject.credits >= 0) {
        "credits no puede ser negativo (${subject.credits})."
    }
}

package com.example.notally.data.remote.firestore

import com.example.notally.data.model.Curso

/** Limites replicados de `firestore.rules` para cursos. */
object CursoLimits {
    const val MAX_NAME = 120
    const val MAX_PROFESSOR = 120
}

/**
 * Convierte un curso local en su forma remota.
 *
 * El docId remoto es `id.toString()` hoy (ver `docs/ARCHITECTURE.md` 5.4).
 * `Curso` no guarda `updatedAt` en Room, asi que por defecto se reutiliza
 * `createdAt`; el SyncEngine debe pasar el `updatedAt` real de su metadata de
 * last-write-wins cuando lo tenga.
 *
 * @throws IllegalArgumentException si rompe los limites de las reglas.
 */
fun Curso.toRemote(updatedAtMs: Long = creadoEn): RemoteCurso {
    val remote = RemoteCurso(
        name = nombre,
        colorArgb = color,
        professor = profesor,
        credits = creditos,
        createdAt = creadoEn,
        updatedAt = updatedAtMs,
        // Room hace borrado duro; un curso local nunca es un tombstone.
        deletedAt = null,
    )
    validateRemoteCurso(remote)
    return remote
}

/**
 * Convierte un curso remoto en local.
 *
 * Perdida documentada: `updatedAt` y `deletedAt` no existen en `Curso` de
 * Room. `deletedAt` debe filtrarlo el SyncEngine (aqui se rechaza el tombstone)
 * y `updatedAt` solo interesa para last-write-wins, que vive en la capa de sync.
 *
 * @param localId id autogenerado de Room. `0` deja que Room lo asigne.
 * @param now reloj de respaldo si el documento no trae `createdAt`.
 * @throws IllegalArgumentException si el documento es un tombstone.
 */
fun RemoteCurso.toLocal(
    localId: Long = 0L,
    now: Long = System.currentTimeMillis(),
): Curso {
    require(deletedAt == null) {
        "RemoteCurso es un tombstone (deletedAt=$deletedAt): el SyncEngine debe " +
            "aplicar el borrado local y no mapearlo como curso vivo."
    }
    validateRemoteCurso(this)
    return Curso(
        id = localId,
        nombre = name,
        color = colorArgb,
        profesor = professor,
        creditos = credits,
        creadoEn = createdAt ?: now,
    )
}

internal fun validateRemoteCurso(curso: RemoteCurso) {
    require(curso.name.length in 1..CursoLimits.MAX_NAME) {
        "name debe tener 1..${CursoLimits.MAX_NAME} caracteres (tiene ${curso.name.length})."
    }
    require(curso.professor == null || curso.professor.length <= CursoLimits.MAX_PROFESSOR) {
        "professor supera ${CursoLimits.MAX_PROFESSOR} caracteres."
    }
    require(curso.credits == null || curso.credits >= 0) {
        "credits no puede ser negativo (${curso.credits})."
    }
}

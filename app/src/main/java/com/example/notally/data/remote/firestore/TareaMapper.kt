package com.example.notally.data.remote.firestore

import com.example.notally.data.aTexto
import com.example.notally.data.estadoDesdeTexto
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.prioridadDesdeTexto

/**
 * Limites replicados de `firestore.rules`. Si cambian las reglas, cambian
 * tambien aqui y en sus tests: el mapper es la primera barrera, las reglas la
 * ultima.
 */
object TaskLimits {
    const val MAX_TITLE = 300
    const val MAX_NOTES = 20_000
    const val MAX_SUBTASKS = 100
    const val MAX_SUBTASK_TITLE = 300
    const val MAX_ID = 128
}

// Contrato remoto: los valores persistidos siguen en inglés.
private val VALID_PRIORITIES = setOf("LOW", "MEDIUM", "HIGH")
private val VALID_STATUSES = setOf("PENDING", "IN_PROGRESS", "DONE")

/**
 * Convierte una tarea local en su forma remota.
 *
 * Los nombres de campo del documento remoto no cambian: `title`, `priority`,
 * `status`, `dueAt`, etc. El mapper traduce los enums locales a su texto
 * original en inglés.
 *
 * @param subtasks subtareas de la tarea. Room las guarda en otra tabla, por eso
 *   no vienen dentro de [Tarea].
 * @param courseId id remoto del curso. Por defecto usa `id.toString()`
 *   hasta que exista la migracion a UUID descrita en `docs/ARCHITECTURE.md`.
 * @throws IllegalArgumentException si la tarea esta archivada (no hay campo
 *   remoto para `isArchived`) o si algun campo rompe los limites de las reglas.
 */
fun Tarea.toRemote(
    subtasks: List<Subtarea> = emptyList(),
    courseId: String? = this.cursoId?.toString(),
): RemoteTask {
    require(!archivada) {
        "Tarea.archivada no tiene campo en firestore.rules; V1 no sincroniza " +
            "archivadas. Desarchiva la tarea o usa deletedAt (ver ARCHITECTURE.md 5.3)."
    }
    val remote = RemoteTask(
        title = titulo,
        priority = prioridad.aTexto(),
        status = estado.aTexto(),
        notes = notas,
        courseId = courseId,
        dueAt = entregaEn,
        subtasks = subtasks
            .sortedBy { it.posicion }
            .map { subtarea ->
                RemoteSubtask(
                    id = subtarea.id.toString(),
                    title = subtarea.titulo,
                    isDone = subtarea.hecha,
                    position = subtarea.posicion,
                )
            },
        createdAt = creadoEn,
        updatedAt = actualizadoEn,
        completedAt = completadoEn,
        // Room hace borrado duro; una Tarea local nunca es un tombstone.
        deletedAt = null,
    )
    validateRemoteTask(remote)
    return remote
}

/**
 * Convierte una tarea remota en local.
 *
 * @param localId id autogenerado de Room. `0` deja que Room lo asigne.
 * @param isArchived valor local explicito: el remoto no transporta archivadas.
 * @param localCursoId id numerico de curso. Si el `courseId` remoto no es
 *   numerico (p. ej. un UUID de la migracion futura) el mapper falla en vez de
 *   perder la relacion en silencio.
 * @param now reloj de respaldo cuando el documento no trae timestamps.
 * @throws IllegalArgumentException si el documento es un tombstone
 *   (`deletedAt != null`): el SyncEngine debe filtrarlo antes de mapear.
 */
fun RemoteTask.toLocal(
    localId: Long = 0L,
    isArchived: Boolean = false,
    localCursoId: Long? = courseId?.toLongOrNull(),
    now: Long = System.currentTimeMillis(),
): Tarea {
    require(deletedAt == null) {
        "RemoteTask es un tombstone (deletedAt=$deletedAt): el SyncEngine debe " +
            "aplicar el borrado local y no mapearlo como tarea viva."
    }
    validateRemoteTask(this)
    require(courseId == null || localCursoId != null) {
        "courseId remoto '$courseId' no es un id local numerico; falta la " +
            "migracion a remoteId (UUID) en Tarea/Curso."
    }
    return Tarea(
        id = localId,
        titulo = title,
        notas = notes,
        cursoId = localCursoId,
        entregaEn = dueAt,
        prioridad = prioridadDesdeTexto(priority),
        estado = estadoDesdeTexto(status),
        creadoEn = createdAt ?: now,
        actualizadoEn = updatedAt ?: createdAt ?: now,
        completadoEn = completedAt,
        archivada = isArchived,
    )
}

/**
 * Subtareas locales para [localTaskId], en orden de `position`.
 *
 * Nota de identidad: Room autogenera `Subtarea.id` (Long) y el remoto usa un
 * string; esa correspondencia se resuelve en el SyncEngine. Aqui el id local se
 * deja en `0` y solo se conservan titulo, estado y posicion.
 */
fun RemoteTask.toLocalSubtasks(localTaskId: Long): List<Subtarea> {
    require(deletedAt == null) {
        "RemoteTask es un tombstone (deletedAt=$deletedAt): no genera subtareas."
    }
    validateRemoteTask(this)
    return subtasks
        .sortedBy { it.position }
        .map { subtask ->
            Subtarea(
                id = 0L,
                tareaId = localTaskId,
                titulo = subtask.title,
                hecha = subtask.isDone,
                posicion = subtask.position,
            )
        }
}

internal fun validateRemoteTask(task: RemoteTask) {
    require(task.title.length in 1..TaskLimits.MAX_TITLE) {
        "title debe tener 1..${TaskLimits.MAX_TITLE} caracteres (tiene ${task.title.length})."
    }
    require(task.notes.length <= TaskLimits.MAX_NOTES) {
        "notes supera ${TaskLimits.MAX_NOTES} caracteres (tiene ${task.notes.length})."
    }
    require(task.courseId == null || task.courseId.length <= TaskLimits.MAX_ID) {
        "courseId supera ${TaskLimits.MAX_ID} caracteres."
    }
    require(task.priority in VALID_PRIORITIES) {
        "priority '${task.priority}' no es valida; usa $VALID_PRIORITIES."
    }
    require(task.status in VALID_STATUSES) {
        "status '${task.status}' no es valido; usa $VALID_STATUSES."
    }
    require(task.subtasks.size <= TaskLimits.MAX_SUBTASKS) {
        "subtasks supera ${TaskLimits.MAX_SUBTASKS} elementos (tiene ${task.subtasks.size})."
    }
    require(task.subtasks.map { it.id }.distinct().size == task.subtasks.size) {
        "subtasks contiene ids duplicados."
    }
    task.subtasks.forEach { subtask ->
        require(subtask.id.isNotBlank() && subtask.id.length <= TaskLimits.MAX_ID) {
            "subtask.id debe tener 1..${TaskLimits.MAX_ID} caracteres."
        }
        require(subtask.title.length <= TaskLimits.MAX_SUBTASK_TITLE) {
            "subtask.title supera ${TaskLimits.MAX_SUBTASK_TITLE} caracteres."
        }
        require(subtask.position >= 0) {
            "subtask.position no puede ser negativo (${subtask.position})."
        }
    }
}

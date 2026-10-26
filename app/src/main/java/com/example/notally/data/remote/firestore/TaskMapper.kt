package com.example.notally.data.remote.firestore

import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subtask
import com.example.notally.data.model.Task

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

private val VALID_PRIORITIES = Priority.entries.map { it.name }.toSet()
private val VALID_STATUSES = Status.entries.map { it.name }.toSet()

/**
 * Convierte una tarea local en su forma remota.
 *
 * @param subtasks subtareas de la tarea. Room las guarda en otra tabla, por eso
 *   no vienen dentro de [Task].
 * @param subjectId id remoto de la materia. Por defecto usa `id.toString()`
 *   hasta que exista la migracion a UUID descrita en `docs/ARCHITECTURE.md`.
 * @throws IllegalArgumentException si la tarea esta archivada (no hay campo
 *   remoto para `isArchived`) o si algun campo rompe los limites de las reglas.
 */
fun Task.toRemote(
    subtasks: List<Subtask> = emptyList(),
    subjectId: String? = this.subjectId?.toString(),
): RemoteTask {
    require(!isArchived) {
        "Task.isArchived no tiene campo en firestore.rules; V1 no sincroniza " +
            "archivadas. Desarchiva la tarea o usa deletedAt (ver ARCHITECTURE.md 5.3)."
    }
    val remote = RemoteTask(
        title = title,
        priority = priority.name,
        status = status.name,
        notes = notes,
        subjectId = subjectId,
        dueAt = dueAt,
        subtasks = subtasks
            .sortedBy { it.position }
            .map { subtask ->
                RemoteSubtask(
                    id = subtask.id.toString(),
                    title = subtask.title,
                    isDone = subtask.isDone,
                    position = subtask.position,
                )
            },
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        // Room hace borrado duro; un Task local nunca es un tombstone.
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
 * @param localSubjectId id numerico de materia. Si el `subjectId` remoto no es
 *   numerico (p. ej. un UUID de la migracion futura) el mapper falla en vez de
 *   perder la relacion en silencio.
 * @param now reloj de respaldo cuando el documento no trae timestamps.
 * @throws IllegalArgumentException si el documento es un tombstone
 *   (`deletedAt != null`): el SyncEngine debe filtrarlo antes de mapear.
 */
fun RemoteTask.toLocal(
    localId: Long = 0L,
    isArchived: Boolean = false,
    localSubjectId: Long? = subjectId?.toLongOrNull(),
    now: Long = System.currentTimeMillis(),
): Task {
    require(deletedAt == null) {
        "RemoteTask es un tombstone (deletedAt=$deletedAt): el SyncEngine debe " +
            "aplicar el borrado local y no mapearlo como tarea viva."
    }
    validateRemoteTask(this)
    require(subjectId == null || localSubjectId != null) {
        "subjectId remoto '$subjectId' no es un id local numerico; falta la " +
            "migracion a remoteId (UUID) en Task/Subject."
    }
    return Task(
        id = localId,
        title = title,
        notes = notes,
        subjectId = localSubjectId,
        dueAt = dueAt,
        priority = Priority.valueOf(priority),
        status = Status.valueOf(status),
        createdAt = createdAt ?: now,
        updatedAt = updatedAt ?: createdAt ?: now,
        completedAt = completedAt,
        isArchived = isArchived,
    )
}

/**
 * Subtareas locales para [localTaskId], en orden de `position`.
 *
 * Nota de identidad: Room autogenera `Subtask.id` (Long) y el remoto usa un
 * string; esa correspondencia se resuelve en el SyncEngine. Aqui el id local se
 * deja en `0` y solo se conservan titulo, estado y posicion.
 */
fun RemoteTask.toLocalSubtasks(localTaskId: Long): List<Subtask> {
    require(deletedAt == null) {
        "RemoteTask es un tombstone (deletedAt=$deletedAt): no genera subtareas."
    }
    validateRemoteTask(this)
    return subtasks
        .sortedBy { it.position }
        .map { subtask ->
            Subtask(
                id = 0L,
                taskId = localTaskId,
                title = subtask.title,
                isDone = subtask.isDone,
                position = subtask.position,
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
    require(task.subjectId == null || task.subjectId.length <= TaskLimits.MAX_ID) {
        "subjectId supera ${TaskLimits.MAX_ID} caracteres."
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

package com.example.notally.data.repository

import com.example.notally.data.local.SubtaskDao
import com.example.notally.data.local.TaskDao
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subtask
import com.example.notally.data.model.Task
import com.example.notally.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

/**
 * Acceso de lectura/escritura a tareas y subtareas.
 *
 * Las consultas por día reciben el rango ya calculado (ver
 * [com.example.notally.data.DateRanges]) para no acoplar el repositorio a la
 * zona horaria ni al reloj del sistema.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
) {

    fun observeActiveTasks(): Flow<List<Task>> = taskDao.observeActiveTasks()

    fun observeTasksForDay(startMillis: Long, endMillis: Long): Flow<List<Task>> =
        taskDao.observeTasksForDay(startMillis, endMillis)

    /**
     * Tareas no archivadas con `dueAt` posterior a [afterMillis], sin tope
     * superior. "Próximas" la usa para no perder tareas lejanas.
     */
    fun observeFutureTasks(afterMillis: Long): Flow<List<Task>> =
        taskDao.observeFutureTasks(afterMillis)

    fun observeTasksBySubject(subjectId: Long): Flow<List<Task>> =
        taskDao.observeTasksBySubject(subjectId)

    fun observeTaskWithSubtasks(taskId: Long): Flow<TaskWithSubtasks?> =
        taskDao.observeTaskWithSubtasks(taskId)

    fun observeSubtasks(taskId: Long): Flow<List<Subtask>> =
        subtaskDao.observeForTask(taskId)

    suspend fun getTaskWithSubtasks(taskId: Long): TaskWithSubtasks? =
        taskDao.getTaskWithSubtasks(taskId)

    suspend fun getTask(taskId: Long): Task? = taskDao.getTask(taskId)

    /**
     * Inserta [task] (ignorando su id) y, opcionalmente, sus subtareas en el
     * orden recibido. Devuelve el id de la tarea creada.
     */
    suspend fun createTask(
        task: Task,
        subtaskTitles: List<String> = emptyList(),
    ): Long {
        val now = System.currentTimeMillis()
        val taskId = taskDao.insert(
            task.copy(
                id = 0L,
                createdAt = now,
                updatedAt = now,
                completedAt = if (task.status == Status.DONE) now else null,
            ),
        )
        if (subtaskTitles.isNotEmpty()) {
            subtaskDao.insertAll(
                subtaskTitles.mapIndexed { index, title ->
                    Subtask(taskId = taskId, title = title, position = index)
                },
            )
        }
        return taskId
    }

    suspend fun createTask(
        title: String,
        notes: String = "",
        subjectId: Long? = null,
        dueAt: Long? = null,
        priority: Priority = Priority.MEDIUM,
        subtaskTitles: List<String> = emptyList(),
    ): Long = createTask(
        task = Task(
            title = title,
            notes = notes,
            subjectId = subjectId,
            dueAt = dueAt,
            priority = priority,
        ),
        subtaskTitles = subtaskTitles,
    )

    suspend fun updateTask(task: Task) {
        val now = System.currentTimeMillis()
        val completedAt = when (task.status) {
            Status.DONE -> task.completedAt ?: now
            else -> null
        }
        taskDao.update(task.copy(updatedAt = now, completedAt = completedAt))
    }

    suspend fun deleteTask(taskId: Long) = taskDao.deleteById(taskId)

    suspend fun toggleDone(taskId: Long, now: Long = System.currentTimeMillis()) =
        taskDao.toggleDone(taskId, now)

    suspend fun setStatus(
        taskId: Long,
        status: Status,
        now: Long = System.currentTimeMillis(),
    ) {
        val completedAt = if (status == Status.DONE) now else null
        taskDao.setStatus(taskId, status, completedAt, now)
    }

    suspend fun setArchived(
        taskId: Long,
        archived: Boolean,
        now: Long = System.currentTimeMillis(),
    ) = taskDao.setArchived(taskId, archived, now)

    /** Borra definitivamente todas las tareas con estado DONE. */
    suspend fun clearCompleted(): Int = taskDao.deleteCompleted()

    suspend fun addSubtask(taskId: Long, title: String): Long {
        val position = subtaskDao.nextPosition(taskId)
        val id = subtaskDao.insert(
            Subtask(taskId = taskId, title = title, position = position),
        )
        taskDao.touch(taskId, System.currentTimeMillis())
        return id
    }

    suspend fun setSubtaskDone(subtaskId: Long, isDone: Boolean) =
        subtaskDao.setDone(subtaskId, isDone)

    suspend fun toggleSubtask(subtaskId: Long) = subtaskDao.toggleDone(subtaskId)

    suspend fun deleteSubtask(subtaskId: Long) = subtaskDao.deleteById(subtaskId)

    suspend fun reorderSubtasks(taskId: Long, orderedSubtaskIds: List<Long>) =
        subtaskDao.reorder(taskId, orderedSubtaskIds)
}

package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.notally.data.model.Status
import com.example.notally.data.model.Task
import com.example.notally.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /**
     * Tareas no archivadas (incluye las completadas), ordenadas por fecha
     * límite ascendente y con las tareas sin fecha al final. A igual fecha,
     * primero las de mayor prioridad.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0
        ORDER BY dueAt IS NULL ASC,
                 dueAt ASC,
                 CASE priority
                     WHEN 'HIGH' THEN 0
                     WHEN 'MEDIUM' THEN 1
                     ELSE 2
                 END ASC,
                 createdAt DESC
        """
    )
    fun observeActiveTasks(): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0 AND dueAt BETWEEN :startMillis AND :endMillis
        ORDER BY dueAt ASC,
                 CASE priority
                     WHEN 'HIGH' THEN 0
                     WHEN 'MEDIUM' THEN 1
                     ELSE 2
                 END ASC
        """
    )
    fun observeTasksForDay(startMillis: Long, endMillis: Long): Flow<List<Task>>

    /**
     * Todo el futuro no archivado a partir de [afterMillis] (sin tope
     * superior). Alimenta "Próximas" para que ninguna tarea quede inaccesible.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0 AND dueAt IS NOT NULL AND dueAt > :afterMillis
        ORDER BY dueAt ASC,
                 CASE priority
                     WHEN 'HIGH' THEN 0
                     WHEN 'MEDIUM' THEN 1
                     ELSE 2
                 END ASC,
                 createdAt DESC
        """
    )
    fun observeFutureTasks(afterMillis: Long): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0 AND subjectId = :subjectId
        ORDER BY status = 'DONE' ASC,
                 dueAt IS NULL ASC,
                 dueAt ASC,
                 createdAt DESC
        """
    )
    fun observeTasksBySubject(subjectId: Long): Flow<List<Task>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskWithSubtasks(taskId: Long): Flow<TaskWithSubtasks?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithSubtasks(taskId: Long): TaskWithSubtasks?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Upsert
    suspend fun upsert(task: Task): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)

    @Delete
    suspend fun delete(task: Task)

    /**
     * Alterna entre DONE y PENDING. Al completar fija [completedAt] = [now];
     * al reabrir lo limpia.
     */
    @Query(
        """
        UPDATE tasks
        SET status = CASE WHEN status = 'DONE' THEN 'PENDING' ELSE 'DONE' END,
            completedAt = CASE WHEN status = 'DONE' THEN NULL ELSE :now END,
            updatedAt = :now
        WHERE id = :taskId
        """
    )
    suspend fun toggleDone(taskId: Long, now: Long)

    @Query(
        """
        UPDATE tasks
        SET status = :status,
            completedAt = :completedAt,
            updatedAt = :now
        WHERE id = :taskId
        """
    )
    suspend fun setStatus(taskId: Long, status: Status, completedAt: Long?, now: Long)

    @Query("UPDATE tasks SET isArchived = :archived, updatedAt = :now WHERE id = :taskId")
    suspend fun setArchived(taskId: Long, archived: Boolean, now: Long)

    @Query("UPDATE tasks SET updatedAt = :now WHERE id = :taskId")
    suspend fun touch(taskId: Long, now: Long)

    /** Borrado definitivo de todas las tareas completadas (y sus subtareas). */
    @Query("DELETE FROM tasks WHERE status = 'DONE'")
    suspend fun deleteCompleted(): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int
}

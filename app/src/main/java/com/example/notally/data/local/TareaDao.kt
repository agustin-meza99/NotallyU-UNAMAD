package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Tarea
import com.example.notally.data.model.TareaConSubtareas
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a tareas. Las consultas SQL conservan sus nombres físicos en inglés;
 * los nombres de los parámetros ligados al SQL se mantienen para no tocar la
 * consulta.
 */
@Dao
interface TareaDao {

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
    fun observarTareasActivas(): Flow<List<Tarea>>

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
    fun observarTareasDelDia(startMillis: Long, endMillis: Long): Flow<List<Tarea>>

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
    fun observarTareasFuturas(afterMillis: Long): Flow<List<Tarea>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0 AND subjectId = :courseId
        ORDER BY status = 'DONE' ASC,
                 dueAt IS NULL ASC,
                 dueAt ASC,
                 createdAt DESC
        """
    )
    fun observarTareasPorCurso(courseId: Long): Flow<List<Tarea>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observarTareaConSubtareas(taskId: Long): Flow<TareaConSubtareas?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun obtenerTareaConSubtareas(taskId: Long): TareaConSubtareas?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun obtenerTarea(taskId: Long): Tarea?

    @Insert
    suspend fun insertar(tarea: Tarea): Long

    @Update
    suspend fun actualizar(tarea: Tarea)

    @Upsert
    suspend fun insertarOActualizar(tarea: Tarea): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun eliminarPorId(taskId: Long)

    @Delete
    suspend fun eliminar(tarea: Tarea)

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
    suspend fun alternarCompletada(taskId: Long, now: Long)

    @Query(
        """
        UPDATE tasks
        SET status = :status,
            completedAt = :completedAt,
            updatedAt = :now
        WHERE id = :taskId
        """
    )
    suspend fun cambiarEstado(taskId: Long, status: Estado, completedAt: Long?, now: Long)

    @Query("UPDATE tasks SET isArchived = :archived, updatedAt = :now WHERE id = :taskId")
    suspend fun cambiarArchivada(taskId: Long, archived: Boolean, now: Long)

    @Query("UPDATE tasks SET updatedAt = :now WHERE id = :taskId")
    suspend fun tocar(taskId: Long, now: Long)

    /** Borrado definitivo de todas las tareas completadas (y sus subtareas). */
    @Query("DELETE FROM tasks WHERE status = 'DONE'")
    suspend fun eliminarCompletadas(): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun contar(): Int
}

package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Subtarea
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

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun obtenerTareaConSubtareas(taskId: Long): TareaConSubtareas?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun obtenerTarea(taskId: Long): Tarea?

    @Query("SELECT * FROM tasks WHERE externalId = :externalId LIMIT 1")
    suspend fun obtenerTareaPorExternalId(externalId: String): Tarea?

    @Insert
    suspend fun insertar(tarea: Tarea): Long

    @Update
    suspend fun actualizar(tarea: Tarea)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun eliminarPorId(taskId: Long)

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

    @Query("UPDATE tasks SET updatedAt = :now WHERE id = :taskId")
    suspend fun tocar(taskId: Long, now: Long)
}

/**
 * Acceso a cursos. Las consultas SQL conservan sus nombres físicos.
 */
@Dao
interface CursoDao {

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observarCursos(): Flow<List<Curso>>

    /**
     * Cursos con el conteo de tareas pendientes (ni completadas ni
     * archivadas). Los cursos sin tareas aparecen con `pendingTaskCount = 0`.
     */
    @Query(
        """
        SELECT subjects.*, COUNT(tasks.id) AS pendingTaskCount
        FROM subjects
        LEFT JOIN tasks ON tasks.subjectId = subjects.id
            AND tasks.status != 'DONE'
            AND tasks.isArchived = 0
        GROUP BY subjects.id
        ORDER BY subjects.name COLLATE NOCASE ASC
        """
    )
    fun observarCursosConConteo(): Flow<List<CursoConConteo>>

    @Query("SELECT * FROM subjects WHERE externalId = :externalId LIMIT 1")
    suspend fun obtenerCursoPorExternalId(externalId: String): Curso?

    @Insert
    suspend fun insertar(curso: Curso): Long

    @Update
    suspend fun actualizar(curso: Curso)

    @Query("DELETE FROM subjects WHERE id = :courseId")
    suspend fun eliminarPorId(courseId: Long)
}

/**
 * Acceso a subtareas. Los parámetros ligados al SQL conservan su nombre para no
 * modificar las consultas.
 */
@Dao
abstract class SubtareaDao {

    @Insert
    abstract suspend fun insertar(subtarea: Subtarea): Long

    @Insert
    abstract suspend fun insertarTodas(subtareas: List<Subtarea>): List<Long>

    @Update
    abstract suspend fun actualizar(subtarea: Subtarea)

    @Query("UPDATE subtasks SET isDone = :isDone WHERE id = :subtaskId")
    abstract suspend fun cambiarHecha(subtaskId: Long, isDone: Boolean)

    @Query("UPDATE subtasks SET isDone = CASE WHEN isDone = 1 THEN 0 ELSE 1 END WHERE id = :subtaskId")
    abstract suspend fun alternarHecha(subtaskId: Long)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    abstract suspend fun eliminarPorId(subtaskId: Long)

    @Query("UPDATE subtasks SET position = :position WHERE id = :subtaskId AND taskId = :taskId")
    abstract suspend fun actualizarPosicion(subtaskId: Long, taskId: Long, position: Int)

    /** Reasigna posiciones 0..n-1 según el orden recibido, atómicamente. */
    @Transaction
    open suspend fun reordenar(taskId: Long, idsOrdenados: List<Long>) {
        idsOrdenados.forEachIndexed { indice, subtaskId ->
            actualizarPosicion(subtaskId, taskId, indice)
        }
    }

    /** Posición libre al final de la lista de una tarea. */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM subtasks WHERE taskId = :taskId")
    abstract suspend fun siguientePosicion(taskId: Long): Int
}

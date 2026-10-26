package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notally.data.model.Subtarea
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a subtareas. Los parámetros ligados al SQL conservan su nombre para no
 * modificar las consultas.
 */
@Dao
abstract class SubtareaDao {

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    abstract fun observarDeTarea(taskId: Long): Flow<List<Subtarea>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    abstract suspend fun obtenerDeTarea(taskId: Long): List<Subtarea>

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

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    abstract suspend fun eliminarDeTarea(taskId: Long)

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

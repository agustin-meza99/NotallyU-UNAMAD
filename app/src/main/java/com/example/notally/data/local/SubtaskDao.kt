package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notally.data.model.Subtask
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubtaskDao {

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    abstract fun observeForTask(taskId: Long): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    abstract suspend fun getForTask(taskId: Long): List<Subtask>

    @Insert
    abstract suspend fun insert(subtask: Subtask): Long

    @Insert
    abstract suspend fun insertAll(subtasks: List<Subtask>): List<Long>

    @Update
    abstract suspend fun update(subtask: Subtask)

    @Query("UPDATE subtasks SET isDone = :isDone WHERE id = :subtaskId")
    abstract suspend fun setDone(subtaskId: Long, isDone: Boolean)

    @Query("UPDATE subtasks SET isDone = CASE WHEN isDone = 1 THEN 0 ELSE 1 END WHERE id = :subtaskId")
    abstract suspend fun toggleDone(subtaskId: Long)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    abstract suspend fun deleteById(subtaskId: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    abstract suspend fun deleteForTask(taskId: Long)

    @Query("UPDATE subtasks SET position = :position WHERE id = :subtaskId AND taskId = :taskId")
    abstract suspend fun updatePosition(subtaskId: Long, taskId: Long, position: Int)

    /** Reasigna posiciones 0..n-1 según el orden recibido, atómicamente. */
    @Transaction
    open suspend fun reorder(taskId: Long, orderedSubtaskIds: List<Long>) {
        orderedSubtaskIds.forEachIndexed { index, subtaskId ->
            updatePosition(subtaskId, taskId, index)
        }
    }

    /** Posición libre al final de la lista de una tarea. */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM subtasks WHERE taskId = :taskId")
    abstract suspend fun nextPosition(taskId: Long): Int
}

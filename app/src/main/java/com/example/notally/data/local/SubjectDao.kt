package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notally.data.model.Subject
import com.example.notally.data.model.SubjectWithTaskCount
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observeSubjects(): Flow<List<Subject>>

    /**
     * Materias con el conteo de tareas pendientes (ni completadas ni
     * archivadas). Las materias sin tareas aparecen con `pendingTaskCount = 0`.
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
    fun observeSubjectsWithTaskCount(): Flow<List<SubjectWithTaskCount>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getById(subjectId: Long): Subject?

    @Insert
    suspend fun insert(subject: Subject): Long

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteById(subjectId: Long)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
}

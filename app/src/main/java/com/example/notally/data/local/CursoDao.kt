package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import kotlinx.coroutines.flow.Flow

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

    @Insert
    suspend fun insertar(curso: Curso): Long

    @Update
    suspend fun actualizar(curso: Curso)

    @Query("DELETE FROM subjects WHERE id = :courseId")
    suspend fun eliminarPorId(courseId: Long)
}

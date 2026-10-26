package com.example.notally.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notally.data.model.Materia
import com.example.notally.data.model.MateriaConConteo
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a materias. Las consultas SQL conservan sus nombres físicos.
 */
@Dao
interface MateriaDao {

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observarMaterias(): Flow<List<Materia>>

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
    fun observarMateriasConConteo(): Flow<List<MateriaConConteo>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun obtenerPorId(subjectId: Long): Materia?

    @Insert
    suspend fun insertar(materia: Materia): Long

    @Update
    suspend fun actualizar(materia: Materia)

    @Delete
    suspend fun eliminar(materia: Materia)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun eliminarPorId(subjectId: Long)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun contar(): Int
}

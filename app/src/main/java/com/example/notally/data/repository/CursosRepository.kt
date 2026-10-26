package com.example.notally.data.repository

import com.example.notally.data.local.CursoDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import kotlinx.coroutines.flow.Flow

class CursosRepository(
    private val cursoDao: CursoDao,
) {

    fun observarCursos(): Flow<List<Curso>> = cursoDao.observarCursos()

    fun observarCursosConConteo(): Flow<List<CursoConConteo>> =
        cursoDao.observarCursosConConteo()

    suspend fun obtenerCurso(cursoId: Long): Curso? = cursoDao.obtenerPorId(cursoId)

    suspend fun crearCurso(
        nombre: String,
        color: Long,
        profesor: String? = null,
        creditos: Int? = null,
    ): Long = cursoDao.insertar(
        Curso(
            nombre = nombre,
            color = color,
            profesor = profesor,
            creditos = creditos,
        ),
    )

    suspend fun actualizarCurso(curso: Curso) = cursoDao.actualizar(curso)

    suspend fun eliminarCurso(cursoId: Long) = cursoDao.eliminarPorId(cursoId)
}

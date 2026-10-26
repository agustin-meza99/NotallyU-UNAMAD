package com.example.notally.data.repository

import com.example.notally.data.local.MateriaDao
import com.example.notally.data.model.Materia
import com.example.notally.data.model.MateriaConConteo
import kotlinx.coroutines.flow.Flow

class RepositorioMaterias(
    private val materiaDao: MateriaDao,
) {

    fun observarMaterias(): Flow<List<Materia>> = materiaDao.observarMaterias()

    fun observarMateriasConConteo(): Flow<List<MateriaConConteo>> =
        materiaDao.observarMateriasConConteo()

    suspend fun obtenerMateria(materiaId: Long): Materia? = materiaDao.obtenerPorId(materiaId)

    suspend fun crearMateria(
        nombre: String,
        color: Long,
        profesor: String? = null,
        creditos: Int? = null,
    ): Long = materiaDao.insertar(
        Materia(
            nombre = nombre,
            color = color,
            profesor = profesor,
            creditos = creditos,
        ),
    )

    suspend fun actualizarMateria(materia: Materia) = materiaDao.actualizar(materia)

    suspend fun eliminarMateria(materiaId: Long) = materiaDao.eliminarPorId(materiaId)
}

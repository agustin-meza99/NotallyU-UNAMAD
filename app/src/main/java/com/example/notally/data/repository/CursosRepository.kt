package com.example.notally.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.notally.data.local.CursoDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Resultado explícito de escribir un curso, sin excepciones hacia la UI. */
sealed interface ResultadoCurso {
    /** El curso se insertó con este id. */
    data class Creado(val id: Long) : ResultadoCurso

    /** El curso existente se actualizó. */
    data class Actualizado(val id: Long) : ResultadoCurso

    /** Nombre repetido: la restricción única de la base lo rechazó. */
    data object Duplicado : ResultadoCurso

    /** Cualquier otro fallo de escritura. */
    data object Error : ResultadoCurso

    val esExito: Boolean
        get() = this is Creado || this is Actualizado
}

class CursosRepository(
    private val cursoDao: CursoDao,
) {

    fun observarCursos(): Flow<List<Curso>> = cursoDao.observarCursos()

    fun observarCursosConConteo(): Flow<List<CursoConConteo>> =
        cursoDao.observarCursosConConteo()

    suspend fun crearCurso(
        nombre: String,
        color: Long,
        profesor: String? = null,
        creditos: Int? = null,
    ): ResultadoCurso = escribir {
        ResultadoCurso.Creado(
            cursoDao.insertar(
                Curso(
                    nombre = nombre,
                    color = color,
                    profesor = profesor,
                    creditos = creditos,
                ),
            ),
        )
    }

    suspend fun actualizarCurso(curso: Curso): ResultadoCurso = escribir {
        cursoDao.actualizar(curso)
        ResultadoCurso.Actualizado(curso.id)
    }

    private suspend fun escribir(bloque: suspend () -> ResultadoCurso): ResultadoCurso =
        try {
            bloque()
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: SQLiteConstraintException) {
            ResultadoCurso.Duplicado
        } catch (_: Exception) {
            ResultadoCurso.Error
        }

    suspend fun eliminarCurso(cursoId: Long) = cursoDao.eliminarPorId(cursoId)
}

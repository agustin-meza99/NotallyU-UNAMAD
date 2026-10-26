package com.example.notally.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.notally.data.local.CursoDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CursosRepositoryTest {

    private class CursoDaoFalso(
        private val alInsertar: suspend () -> Long,
    ) : CursoDao {
        override fun observarCursos(): Flow<List<Curso>> = emptyFlow()

        override suspend fun obtenerCursos(): List<Curso> = emptyList()

        override fun observarCursosConConteo(): Flow<List<CursoConConteo>> =
            emptyFlow()

        override suspend fun obtenerCursoPorExternalId(externalId: String): Curso? = null

        override suspend fun insertar(curso: Curso): Long = alInsertar()

        override suspend fun actualizar(curso: Curso) = Unit

        override suspend fun eliminarPorId(courseId: Long) = Unit
    }

    @Test
    fun `crear curso devuelve Creado con el id insertado`() = runBlocking {
        val repositorio = CursosRepository(CursoDaoFalso(alInsertar = { 42L }))

        val resultado = repositorio.crearCurso(nombre = "Física")

        assertEquals(ResultadoCurso.Creado(42L), resultado)
        assertTrue(resultado.esExito)
    }

    @Test
    fun `nombre duplicado se convierte en Duplicado`() = runBlocking {
        val repositorio = CursosRepository(
            CursoDaoFalso(alInsertar = { throw SQLiteConstraintException("duplicado") }),
        )

        val resultado = repositorio.crearCurso(nombre = "Física")

        assertEquals(ResultadoCurso.Duplicado, resultado)
        assertTrue(!resultado.esExito)
    }

    @Test
    fun `un fallo de escritura se convierte en Error sin lanzar`() = runBlocking {
        val repositorio = CursosRepository(
            CursoDaoFalso(alInsertar = { error("disco lleno") }),
        )

        val resultado = repositorio.crearCurso(nombre = "Física")

        assertEquals(ResultadoCurso.Error, resultado)
        assertTrue(!resultado.esExito)
    }

    @Test
    fun `actualizar curso devuelve Actualizado con el id`() = runBlocking {
        val repositorio = CursosRepository(CursoDaoFalso(alInsertar = { 0L }))

        val resultado = repositorio.actualizarCurso(
            Curso(id = 7L, nombre = "Física"),
        )

        assertEquals(ResultadoCurso.Actualizado(7L), resultado)
        assertTrue(resultado.esExito)
    }
}

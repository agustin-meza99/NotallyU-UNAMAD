package com.example.notally.data.aula

import com.example.notally.data.local.CursoDao
import com.example.notally.data.local.TareaDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.model.TareaConSubtareas
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.TareasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AulaImporterTest {

    private class CursoDaoMemoria : CursoDao {
        val cursos = mutableListOf<Curso>()
        private var siguienteId = 1L

        override fun observarCursos(): Flow<List<Curso>> = emptyFlow()

        override fun observarCursosConConteo(): Flow<List<CursoConConteo>> =
            emptyFlow()

        override suspend fun obtenerCursoPorExternalId(externalId: String): Curso? =
            cursos.firstOrNull { it.externalId == externalId }

        override suspend fun insertar(curso: Curso): Long {
            val id = siguienteId++
            cursos += curso.copy(id = id)
            return id
        }

        override suspend fun actualizar(curso: Curso) {
            val indice = cursos.indexOfFirst { it.id == curso.id }
            if (indice >= 0) cursos[indice] = curso
        }

        override suspend fun eliminarPorId(courseId: Long) {
            cursos.removeAll { it.id == courseId }
        }
    }

    private class TareaDaoMemoria : TareaDao {
        val tareas = mutableListOf<Tarea>()
        private var siguienteId = 1L

        override fun observarTareasActivas(): Flow<List<Tarea>> = emptyFlow()

        override fun observarTareasDelDia(
            startMillis: Long,
            endMillis: Long,
        ): Flow<List<Tarea>> = emptyFlow()

        override fun observarTareasFuturas(afterMillis: Long): Flow<List<Tarea>> =
            emptyFlow()

        override suspend fun obtenerTareaConSubtareas(taskId: Long): TareaConSubtareas? {
            val tarea = tareas.firstOrNull { it.id == taskId } ?: return null
            return TareaConSubtareas(tarea, emptyList<Subtarea>())
        }

        override suspend fun obtenerTarea(taskId: Long): Tarea? =
            tareas.firstOrNull { it.id == taskId }

        override suspend fun obtenerTareaPorExternalId(externalId: String): Tarea? =
            tareas.firstOrNull { it.externalId == externalId }

        override suspend fun insertar(tarea: Tarea): Long {
            val id = siguienteId++
            tareas += tarea.copy(id = id)
            return id
        }

        override suspend fun actualizar(tarea: Tarea) {
            val indice = tareas.indexOfFirst { it.id == tarea.id }
            if (indice >= 0) tareas[indice] = tarea
        }

        override suspend fun eliminarPorId(taskId: Long) {
            tareas.removeAll { it.id == taskId }
        }

        override suspend fun alternarCompletada(taskId: Long, now: Long) = Unit

        override suspend fun cambiarEstado(
            taskId: Long,
            status: Estado,
            completedAt: Long?,
            now: Long,
        ) = Unit

        override suspend fun tocar(taskId: Long, now: Long) = Unit
    }

    private class AulaFalsa(
        var tareasPorSeccion: Map<String, List<AulaTarea>>,
        var resultadoLogin: ResultadoLogin = ResultadoLogin.Exito,
    ) : AulaApi {
        var desconectado = false

        override suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin =
            resultadoLogin

        override suspend fun cursos(): List<AulaCurso> = listOf(
            AulaCurso(
                id = "curso-1",
                codigoCurso = "SI311",
                sectionId = "seccion-1",
                codigoSeccion = "A",
                nombre = "BIG DATA",
            ),
        )

        override suspend fun tareas(sectionId: String): List<AulaTarea> =
            tareasPorSeccion[sectionId].orEmpty()

        override suspend fun periodoAcademico(): String? = null

        override fun disconnect() {
            desconectado = true
        }
    }

    private val entregaOriginal =
        java.time.Instant.parse("2026-09-18T18:00:00Z").toEpochMilli()
    private val entregaNueva =
        java.time.Instant.parse("2026-09-25T18:00:00Z").toEpochMilli()

    private fun crearImportador(
        cursoDao: CursoDaoMemoria,
        tareaDao: TareaDaoMemoria,
    ) = AulaImporter(
        repositorioCursos = CursosRepository(cursoDao),
        repositorioTareas = TareasRepository(tareaDao, SubtareaDaoVacia),
        reloj = { 1_000L },
        espera = { },
    )

    @Test
    fun `reimportar no duplica y actualiza titulo y fecha`() = runBlocking {
        val cursoDao = CursoDaoMemoria()
        val tareaDao = TareaDaoMemoria()
        val api = AulaFalsa(
            tareasPorSeccion = mapOf(
                "seccion-1" to listOf(
                    AulaTarea(
                        id = "tarea-1",
                        titulo = "La ética del uso de los datos",
                        descripcion = "Primera versión",
                        unidad = "UNIDAD 1",
                        entregaEn = entregaOriginal,
                    ),
                ),
            ),
        )
        val importador = crearImportador(cursoDao, tareaDao)

        val primera = importador.importar(api, "usuario", "clave")
        assertTrue(primera is ResultadoImportacion.Exito)
        primera as ResultadoImportacion.Exito
        assertEquals(1, primera.cursos)
        assertEquals(1, primera.tareasNuevas)
        assertEquals(0, primera.tareasActualizadas)
        assertEquals(0, primera.errores)
        assertEquals(1, cursoDao.cursos.size)
        assertEquals(1, tareaDao.tareas.size)
        assertEquals("seccion-1", cursoDao.cursos.single().externalId)
        assertEquals("tarea-1", tareaDao.tareas.single().externalId)

        // El usuario la completa: la reimportación no debe reabrirla.
        tareaDao.tareas[0] = tareaDao.tareas[0].copy(
            estado = Estado.COMPLETADA,
            completadoEn = 500L,
        )
        api.tareasPorSeccion = mapOf(
            "seccion-1" to listOf(
                AulaTarea(
                    id = "tarea-1",
                    titulo = "Título actualizado",
                    descripcion = "Segunda versión",
                    unidad = "UNIDAD 2",
                    entregaEn = entregaNueva,
                ),
            ),
        )

        val segunda = importador.importar(api, "usuario", "clave")
        assertTrue(segunda is ResultadoImportacion.Exito)
        segunda as ResultadoImportacion.Exito
        assertEquals(0, segunda.tareasNuevas)
        assertEquals(1, segunda.tareasActualizadas)
        assertEquals(0, segunda.errores)

        assertEquals(1, cursoDao.cursos.size)
        val tarea = tareaDao.tareas.single()
        assertEquals(1, tareaDao.tareas.size)
        assertEquals("Título actualizado", tarea.titulo)
        assertEquals("Segunda versión", tarea.notas)
        assertEquals(entregaNueva, tarea.entregaEn)
        assertEquals(Estado.COMPLETADA, tarea.estado)
        assertEquals(500L, tarea.completadoEn)
    }

    @Test
    fun `campos vacios usan la unidad como nota`() = runBlocking {
        val cursoDao = CursoDaoMemoria()
        val tareaDao = TareaDaoMemoria()
        val api = AulaFalsa(
            tareasPorSeccion = mapOf(
                "seccion-1" to listOf(
                    AulaTarea(
                        id = "tarea-2",
                        titulo = "Sin descripción",
                        descripcion = "",
                        unidad = "UNIDAD 3",
                    ),
                ),
            ),
        )

        ejecutar(crearImportador(cursoDao, tareaDao), api)

        assertEquals("UNIDAD 3", tareaDao.tareas.single().notas)
    }

    @Test
    fun `credenciales incorrectas no tocan la base ni reintentan`() = runBlocking {
        val cursoDao = CursoDaoMemoria()
        val tareaDao = TareaDaoMemoria()
        val api = AulaFalsa(
            tareasPorSeccion = emptyMap(),
            resultadoLogin = ResultadoLogin.Fallo(ErrorAula.CREDENCIALES, "Datos inválidos"),
        )

        val resultado = crearImportador(cursoDao, tareaDao)
            .importar(api, "usuario", "mala")

        assertTrue(resultado is ResultadoImportacion.Fallo)
        assertEquals(
            ErrorAula.CREDENCIALES,
            (resultado as ResultadoImportacion.Fallo).tipo,
        )
        assertTrue(cursoDao.cursos.isEmpty())
        assertTrue(tareaDao.tareas.isEmpty())
        assertTrue(api.desconectado)
    }

    private suspend fun ejecutar(
        importador: AulaImporter,
        api: AulaApi,
    ): ResultadoImportacion = importador.importar(api, "usuario", "clave")

    private object SubtareaDaoVacia : com.example.notally.data.local.SubtareaDao() {
        override suspend fun insertar(subtarea: Subtarea): Long = 0L

        override suspend fun insertarTodas(subtareas: List<Subtarea>): List<Long> =
            emptyList()

        override suspend fun actualizar(subtarea: Subtarea) = Unit

        override suspend fun cambiarHecha(subtaskId: Long, isDone: Boolean) = Unit

        override suspend fun alternarHecha(subtaskId: Long) = Unit

        override suspend fun eliminarPorId(subtaskId: Long) = Unit

        override suspend fun actualizarPosicion(
            subtaskId: Long,
            taskId: Long,
            position: Int,
        ) = Unit

        override suspend fun siguientePosicion(taskId: Long): Int = 0
    }
}

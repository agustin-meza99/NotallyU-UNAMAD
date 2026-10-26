package com.example.notally.data.remote

import com.example.notally.data.aTexto
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.remote.firestore.RemoteSubtask
import com.example.notally.data.remote.firestore.RemoteTask
import com.example.notally.data.remote.firestore.TaskLimits
import com.example.notally.data.remote.firestore.toLocal
import com.example.notally.data.remote.firestore.toLocalSubtasks
import com.example.notally.data.remote.firestore.toRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TareaMapperTest {

    private fun tarea(
        id: Long = 7L,
        titulo: String = "Ensayo de historia",
        notas: String = "Fuentes primarias",
        cursoId: Long? = 3L,
        entregaEn: Long? = 1_700_000_000_000L,
        prioridad: Prioridad = Prioridad.ALTA,
        estado: Estado = Estado.EN_CURSO,
        creadoEn: Long = 1_600_000_000_000L,
        actualizadoEn: Long = 1_610_000_000_000L,
        completadoEn: Long? = null,
        archivada: Boolean = false,
    ) = Tarea(
        id = id,
        titulo = titulo,
        notas = notas,
        cursoId = cursoId,
        entregaEn = entregaEn,
        prioridad = prioridad,
        estado = estado,
        creadoEn = creadoEn,
        actualizadoEn = actualizadoEn,
        completadoEn = completadoEn,
        archivada = archivada,
    )

    @Test
    fun `la tarea va y vuelve por el remoto conservando los campos escalares`() {
        val local = tarea()

        val remoto = local.toRemote()
        val vuelta = remoto.toLocal(localId = local.id)

        assertEquals(local, vuelta)
        assertEquals("3", remoto.courseId)
        assertEquals("HIGH", remoto.priority)
        assertEquals("IN_PROGRESS", remoto.status)
        assertNull(remoto.deletedAt)
    }

    @Test
    fun `una tarea completada conserva completedAt`() {
        val completadoEn = 1_620_000_000_000L
        val local = tarea(estado = Estado.COMPLETADA, completadoEn = completadoEn)

        val vuelta = local.toRemote().toLocal(localId = local.id)

        assertEquals(Estado.COMPLETADA, vuelta.estado)
        assertEquals(completadoEn, vuelta.completadoEn)
    }

    @Test
    fun `los campos nulos sobreviven como nulos`() {
        val local = tarea(
            cursoId = null,
            entregaEn = null,
            completadoEn = null,
        )

        val remoto = local.toRemote()
        val vuelta = remoto.toLocal(localId = local.id)

        assertNull(remoto.courseId)
        assertNull(remoto.dueAt)
        assertNull(remoto.completedAt)
        assertNull(remoto.deletedAt)
        assertNull(vuelta.cursoId)
        assertNull(vuelta.entregaEn)
        assertNull(vuelta.completadoEn)
    }

    @Test
    fun `cada prioridad y estado va y vuelve con su texto remoto en ingles`() {
        Estado.entries.forEach { estado ->
            Prioridad.entries.forEach { prioridad ->
                val local = tarea(estado = estado, prioridad = prioridad)
                val remoto = local.toRemote()
                val vuelta = remoto.toLocal(localId = local.id)

                assertEquals(prioridad.aTexto(), remoto.priority)
                assertEquals(estado.aTexto(), remoto.status)
                assertEquals(prioridad, vuelta.prioridad)
                assertEquals(estado, vuelta.estado)
            }
        }
    }

    @Test
    fun `las subtareas se ordenan por posicion y conservan su contenido`() {
        val subtareas = listOf(
            Subtarea(id = 11L, tareaId = 7L, titulo = "Buscar fuentes", hecha = true, posicion = 1),
            Subtarea(id = 12L, tareaId = 7L, titulo = "Leer capitulo", hecha = false, posicion = 0),
        )

        val remoto = tarea().toRemote(subtasks = subtareas)
        val vuelta = remoto.toLocalSubtasks(localTaskId = 7L)

        assertEquals(listOf("12", "11"), remoto.subtasks.map { it.id })
        assertEquals(listOf("Leer capitulo", "Buscar fuentes"), vuelta.map { it.titulo })
        assertEquals(listOf(0, 1), vuelta.map { it.posicion })
        assertEquals(listOf(false, true), vuelta.map { it.hecha })
    }

    @Test
    fun `las listas de subtareas se limitan a cien`() {
        val exactamenteCien = (0 until TaskLimits.MAX_SUBTASKS).map { indice ->
            Subtarea(id = indice + 1L, tareaId = 7L, titulo = "Subtarea $indice", posicion = indice)
        }
        assertEquals(100, tarea().toRemote(subtasks = exactamenteCien).subtasks.size)

        val demasiadas = exactamenteCien + Subtarea(
            id = 101L,
            tareaId = 7L,
            titulo = "Una mas",
            posicion = 100,
        )
        assertThrows(IllegalArgumentException::class.java) {
            tarea().toRemote(subtasks = demasiadas)
        }
    }

    @Test
    fun `el titulo acepta 300 caracteres y rechaza 301`() {
        assertEquals(300, tarea(titulo = "a".repeat(300)).toRemote().title.length)
        assertThrows(IllegalArgumentException::class.java) {
            tarea(titulo = "a".repeat(301)).toRemote()
        }
    }

    @Test
    fun `un titulo vacio se rechaza porque las reglas exigen al menos un caracter`() {
        assertThrows(IllegalArgumentException::class.java) {
            tarea(titulo = "").toRemote()
        }
    }

    @Test
    fun `las notas aceptan 20000 caracteres y rechazan 20001`() {
        assertEquals(20_000, tarea(notas = "n".repeat(20_000)).toRemote().notes.length)
        assertThrows(IllegalArgumentException::class.java) {
            tarea(notas = "n".repeat(20_001)).toRemote()
        }
    }

    @Test
    fun `se rechaza un titulo de subtarea de mas de 300 caracteres`() {
        val subtarea = Subtarea(id = 1L, tareaId = 7L, titulo = "s".repeat(301), posicion = 0)

        assertThrows(IllegalArgumentException::class.java) {
            tarea().toRemote(subtasks = listOf(subtarea))
        }
    }

    @Test
    fun `se rechazan ids de subtarea duplicados`() {
        val subtareas = listOf(
            Subtarea(id = 1L, tareaId = 7L, titulo = "A", posicion = 0),
            Subtarea(id = 1L, tareaId = 7L, titulo = "B", posicion = 1),
        )

        assertThrows(IllegalArgumentException::class.java) {
            tarea().toRemote(subtasks = subtareas)
        }
    }

    @Test
    fun `se rechaza una posicion negativa de subtarea`() {
        val subtarea = Subtarea(id = 1L, tareaId = 7L, titulo = "A", posicion = -1)

        assertThrows(IllegalArgumentException::class.java) {
            tarea().toRemote(subtasks = listOf(subtarea))
        }
    }

    @Test
    fun `una tarea archivada no se puede mapear porque las reglas no tienen campo`() {
        val archivada = tarea(archivada = true)

        val error = assertThrows(IllegalArgumentException::class.java) {
            archivada.toRemote()
        }
        assertEquals(true, error.message!!.contains("archivada"))
    }

    @Test
    fun `un tombstone no se puede mapear como tarea local viva`() {
        val tombstone = RemoteTask(
            title = "Borrada",
            priority = "LOW",
            status = "PENDING",
            deletedAt = 1_630_000_000_000L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            tombstone.toLocal()
        }
        assertThrows(IllegalArgumentException::class.java) {
            tombstone.toLocalSubtasks(localTaskId = 7L)
        }
    }

    @Test
    fun `un remoto sin timestamps usa el reloj recibido`() {
        val remoto = RemoteTask(
            title = "Sin fechas",
            priority = "MEDIUM",
            status = "PENDING",
            createdAt = null,
            updatedAt = null,
        )

        val local = remoto.toLocal(now = 42_000L)

        assertEquals(42_000L, local.creadoEn)
        assertEquals(42_000L, local.actualizadoEn)
    }

    @Test
    fun `se conserva updatedAt remoto aunque falte createdAt`() {
        val remoto = RemoteTask(
            title = "Parcial",
            priority = "LOW",
            status = "PENDING",
            createdAt = null,
            updatedAt = 9_000L,
        )

        val local = remoto.toLocal(now = 42_000L)

        assertEquals(42_000L, local.creadoEn)
        assertEquals(9_000L, local.actualizadoEn)
    }

    @Test
    fun `se rechazan textos remotos invalidos de prioridad y estado`() {
        val malaPrioridad = RemoteTask(title = "X", priority = "URGENT", status = "PENDING")
        val malEstado = RemoteTask(title = "X", priority = "LOW", status = "BLOCKED")

        assertThrows(IllegalArgumentException::class.java) { malaPrioridad.toLocal() }
        assertThrows(IllegalArgumentException::class.java) { malEstado.toLocal() }
    }

    @Test
    fun `un courseId remoto no numerico falla en vez de perder la relacion`() {
        val remoto = RemoteTask(
            title = "Curso futura",
            priority = "LOW",
            status = "PENDING",
            courseId = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        )

        assertThrows(IllegalArgumentException::class.java) {
            remoto.toLocal()
        }
    }

    @Test
    fun `un id local explicito reemplaza al remoto parseado`() {
        val remoto = RemoteTask(
            title = "Con curso",
            priority = "LOW",
            status = "PENDING",
            courseId = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        )

        val local = remoto.toLocal(localCursoId = 5L)

        assertEquals(5L, local.cursoId)
    }

    @Test
    fun `se rechaza una subtarea remota con titulo demasiado largo`() {
        val remoto = RemoteTask(
            title = "Tarea",
            priority = "LOW",
            status = "PENDING",
            subtasks = listOf(
                RemoteSubtask(id = "s1", title = "s".repeat(301), isDone = false, position = 0),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            remoto.toLocal()
        }
    }

    @Test
    fun `se rechaza un titulo remoto de mas de 300 caracteres`() {
        val remoto = RemoteTask(
            title = "t".repeat(301),
            priority = "LOW",
            status = "PENDING",
        )

        assertThrows(IllegalArgumentException::class.java) {
            remoto.toLocal()
        }
    }
}

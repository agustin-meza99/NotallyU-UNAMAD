package com.example.notally.data.firebase

import com.example.notally.data.model.Curso
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MapeoFirestoreTest {

    private fun curso(
        id: Long = 3L,
        nombre: String = "Historia del Perú",
        profesor: String? = "Dra. Rojas",
        creditos: Int? = 4,
        creadoEn: Long = 1_600_000_000_000L,
        externalId: String? = "aula-123",
    ) = Curso(
        id = id,
        nombre = nombre,
        profesor = profesor,
        creditos = creditos,
        creadoEn = creadoEn,
        externalId = externalId,
    )

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
        externalId: String? = "aula-task-uuid",
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
        externalId = externalId,
    )

    @Test
    fun `el docId del curso es su externalId del Aula`() {
        val documento = construirDocumentoCurso(curso(externalId = "section-42"), null)

        assertEquals("section-42", documento!!.id)
    }

    @Test
    fun `un curso manual sin externalId no se sube`() {
        assertNull(construirDocumentoCurso(curso(externalId = null), null))
        assertNull(construirDocumentoCurso(curso(externalId = " "), null))
    }

    @Test
    fun `el curso nuevo usa serverTimestamp en createdAt y updatedAt`() {
        val campos = construirDocumentoCurso(curso(), null)!!.campos

        assertEquals("Historia del Perú", campos["name"])
        assertEquals("Dra. Rojas", campos["professor"])
        assertEquals(4, campos["credits"])
        assertEquals(MarcaTiempo.Servidor, campos["createdAt"])
        assertEquals(MarcaTiempo.Servidor, campos["updatedAt"])
        assertNull(campos["deletedAt"])
    }

    @Test
    fun `el update de curso conserva el createdAt existente`() {
        val existente = 1_500_000_000_000L

        val campos = construirDocumentoCurso(curso(), creadoEnRemoto = existente)!!.campos

        assertEquals(MarcaTiempo.Existente(existente), campos["createdAt"])
        assertEquals(MarcaTiempo.Servidor, campos["updatedAt"])
    }

    @Test
    fun `profesor y creditos nulos se envían como nulos`() {
        val campos = construirDocumentoCurso(
            curso(profesor = null, creditos = null),
            null,
        )!!.campos

        assertNull(campos["professor"])
        assertNull(campos["credits"])
    }

    @Test
    fun `el docId de la tarea es su externalId`() {
        val documento = construirDocumentoTarea(tarea(externalId = "uuid-1"))

        assertEquals("uuid-1", documento!!.id)
    }

    @Test
    fun `una tarea manual sin externalId no se sube`() {
        assertNull(construirDocumentoTarea(tarea(externalId = null)))
        assertNull(construirDocumentoTarea(tarea(externalId = "")))
    }

    @Test
    fun `una tarea archivada no se sube porque las reglas no tienen el campo`() {
        assertNull(construirDocumentoTarea(tarea(archivada = true)))
    }

    @Test
    fun `la tarea mapea prioridad estado curso y campos nulos`() {
        val campos = construirDocumentoTarea(
            tarea = tarea(
                cursoId = null,
                entregaEn = null,
                completadoEn = null,
            ),
            cursoExternalId = null,
        )!!.campos

        assertEquals("Ensayo de historia", campos["title"])
        assertEquals("Fuentes primarias", campos["notes"])
        assertNull(campos["courseId"])
        assertNull(campos["dueAt"])
        assertEquals("HIGH", campos["priority"])
        assertEquals("IN_PROGRESS", campos["status"])
        assertNull(campos["completedAt"])
        assertNull(campos["deletedAt"])
    }

    @Test
    fun `cada prioridad y estado local se traduce al texto remoto`() {
        val prioridades = mapOf(
            Prioridad.BAJA to "LOW",
            Prioridad.MEDIA to "MEDIUM",
            Prioridad.ALTA to "HIGH",
        )
        val estados = mapOf(
            Estado.PENDIENTE to "PENDING",
            Estado.EN_CURSO to "IN_PROGRESS",
            Estado.COMPLETADA to "DONE",
        )

        prioridades.forEach { (prioridad, texto) ->
            val campos = construirDocumentoTarea(tarea(prioridad = prioridad))!!.campos
            assertEquals(texto, campos["priority"])
        }
        estados.forEach { (estado, texto) ->
            val campos = construirDocumentoTarea(tarea(estado = estado))!!.campos
            assertEquals(texto, campos["status"])
        }
    }

    @Test
    fun `courseId remoto es el externalId del curso local`() {
        val campos = construirDocumentoTarea(
            tarea = tarea(cursoId = 3L),
            cursoExternalId = "section-42",
        )!!.campos

        assertEquals("section-42", campos["courseId"])
    }

    @Test
    fun `una tarea sin curso local sube courseId nulo`() {
        val campos = construirDocumentoTarea(
            tarea = tarea(cursoId = null),
            cursoExternalId = null,
        )!!.campos

        assertNull(campos["courseId"])
    }

    @Test
    fun `las subtareas se ordenan por posición y viajan como mapas`() {
        val subtareas = listOf(
            Subtarea(id = 11L, tareaId = 7L, titulo = "Buscar fuentes", hecha = true, posicion = 1),
            Subtarea(id = 12L, tareaId = 7L, titulo = "Leer capítulo", hecha = false, posicion = 0),
        )

        val campos = construirDocumentoTarea(tarea(), subtareas = subtareas)!!.campos
        @Suppress("UNCHECKED_CAST")
        val remotas = campos["subtasks"] as List<Map<String, Any?>>

        assertEquals(listOf("12", "11"), remotas.map { it["id"] })
        assertEquals(listOf(0, 1), remotas.map { it["position"] })
        assertEquals(listOf(false, true), remotas.map { it["isDone"] })
    }

    @Test
    fun `el update de tarea conserva el createdAt existente`() {
        val existente = 1_500_000_000_000L

        val campos = construirDocumentoTarea(
            tarea = tarea(),
            creadoEnRemoto = existente,
        )!!.campos

        assertEquals(MarcaTiempo.Existente(existente), campos["createdAt"])
        assertEquals(MarcaTiempo.Servidor, campos["updatedAt"])
    }

    @Test
    fun `createdAt se conserva solo si el documento existía`() {
        assertEquals(MarcaTiempo.Servidor, marcaCreadoEn(null))
        assertEquals(MarcaTiempo.Existente(99L), marcaCreadoEn(99L))
    }

    @Test
    fun `un título fuera del límite no genera documento`() {
        assertThrows(IllegalArgumentException::class.java) {
            construirDocumentoTarea(tarea(titulo = "t".repeat(301)))
        }
    }

    @Test
    fun `un curso fuera del límite no genera documento`() {
        assertThrows(IllegalArgumentException::class.java) {
            construirDocumentoCurso(curso(nombre = "n".repeat(121)), null)
        }
    }
}

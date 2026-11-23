package com.example.notally.ui.today

import com.example.notally.data.model.Curso
import com.example.notally.data.model.Tarea
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodaySearchRulesTest {

    private val zona = ZoneId.of("America/Lima")

    @Test
    fun `saludo segun hora distingue manana tarde y noche`() {
        val manana = LocalDateTime.of(2026, 11, 23, 8, 30).atZone(zona).toInstant().toEpochMilli()
        val tarde = LocalDateTime.of(2026, 11, 23, 15, 0).atZone(zona).toInstant().toEpochMilli()
        val noche = LocalDateTime.of(2026, 11, 23, 21, 15).atZone(zona).toInstant().toEpochMilli()

        assertEquals(SaludoDia.MANANA, saludoSegunHora(manana, zona))
        assertEquals(SaludoDia.TARDE, saludoSegunHora(tarde, zona))
        assertEquals(SaludoDia.NOCHE, saludoSegunHora(noche, zona))
    }

    @Test
    fun `filtrarSeccionesPorConsulta filtra por titulo curso y notas y expande seccion`() {
        val cursoMoviles = Curso(id = 1L, nombre = "Aplicaciones Móviles II")
        val cursoIa = Curso(id = 2L, nombre = "Inteligencia Artificial")
        val e1 = ElementoHoy(
            tarea = Tarea(id = 10L, titulo = "Informe ISO 25010", notas = "Paleta UNAMAD", cursoId = 1L),
            curso = cursoMoviles,
        )
        val e2 = ElementoHoy(
            tarea = Tarea(id = 20L, titulo = "Laboratorio Redes", notas = "PyTorch", cursoId = 2L),
            curso = cursoIa,
        )
        val secciones = listOf(
            SeccionHoyUi(
                tipo = TipoSeccionHoy.PROXIMAS,
                elementos = listOf(e1, e2),
                cantidad = 2,
                contraible = true,
                expandida = false,
            ),
        )

        val porCurso = filtrarSeccionesPorConsulta(secciones, "móviles")
        assertEquals(1, porCurso.size)
        assertEquals(1, porCurso.first().elementos.size)
        assertEquals(10L, porCurso.first().elementos.first().tarea.id)
        assertEquals(true, porCurso.first().expandida)

        val porNota = filtrarSeccionesPorConsulta(secciones, "pytorch")
        assertEquals(1, porNota.size)
        assertEquals(20L, porNota.first().elementos.first().tarea.id)
    }

    @Test
    fun `calcularPromedioNotas ignora nulos y valores fuera de rango vigesimal`() {
        val tareas = listOf(
            Tarea(id = 1L, titulo = "T1", calificacion = 16.0),
            Tarea(id = 2L, titulo = "T2", calificacion = 18.0),
            Tarea(id = 3L, titulo = "T3", calificacion = null),
            Tarea(id = 4L, titulo = "T4", calificacion = 25.0),
        )
        assertEquals(17.0, calcularPromedioNotas(tareas)!!, 0.001)
        assertNull(calcularPromedioNotas(listOf(Tarea(id = 5L, titulo = "Sin nota"))))
    }
}

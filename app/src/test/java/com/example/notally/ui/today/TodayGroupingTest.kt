package com.example.notally.ui.today

import com.example.notally.data.model.Estado
import com.example.notally.data.model.Materia
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Tarea
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayGroupingTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(fecha: String, hora: String = "12:00"): Long =
        LocalDate.parse(fecha)
            .atTime(LocalTime.parse(hora))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    private val ahora = epoch("2026-10-25", "12:00")

    private val materia = Materia(
        id = 1L,
        nombre = "Cálculo diferencial",
        color = 0xFF3A5A9E,
    )
    private val materias = mapOf(1L to materia)

    private fun tarea(
        id: Long,
        titulo: String,
        entregaEn: Long?,
        estado: Estado = Estado.PENDIENTE,
        prioridad: Prioridad = Prioridad.MEDIA,
        creadoEn: Long = epoch("2026-10-01", "08:00"),
        completadoEn: Long? = null,
        materiaId: Long? = 1L,
    ) = Tarea(
        id = id,
        titulo = titulo,
        materiaId = materiaId,
        entregaEn = entregaEn,
        prioridad = prioridad,
        estado = estado,
        creadoEn = creadoEn,
        actualizadoEn = completadoEn ?: creadoEn,
        completadoEn = completadoEn,
    )

    private val atrasadaAntigua = tarea(
        id = 1,
        titulo = "Ensayo atrasado",
        entregaEn = epoch("2026-10-20", "10:00"),
        prioridad = Prioridad.ALTA,
    )
    private val atrasadaReciente = tarea(
        id = 2,
        titulo = "Quiz atrasado",
        entregaEn = epoch("2026-10-24", "09:00"),
        prioridad = Prioridad.BAJA,
    )
    private val hoyTarde = tarea(
        id = 3,
        titulo = "Laboratorio",
        entregaEn = epoch("2026-10-25", "18:00"),
    )
    private val hoyCompletada = tarea(
        id = 4,
        titulo = "Lectura completada",
        entregaEn = epoch("2026-10-25", "08:00"),
        estado = Estado.COMPLETADA,
        completadoEn = epoch("2026-10-25", "10:00"),
    )
    private val sinHora = tarea(
        id = 5,
        titulo = "Renovar carné",
        entregaEn = null,
        prioridad = Prioridad.BAJA,
    )
    private val proximaCercana = tarea(
        id = 6,
        titulo = "Taller de SQL",
        entregaEn = epoch("2026-10-26", "09:00"),
    )
    private val proximaCompletada = tarea(
        id = 7,
        titulo = "Exposición lista",
        entregaEn = epoch("2026-10-27", "14:00"),
        estado = Estado.COMPLETADA,
        completadoEn = epoch("2026-10-24", "12:00"),
    )
    private val proximaLejana = tarea(
        id = 11,
        titulo = "Proyecto final dentro de ocho días",
        entregaEn = epoch("2026-11-02", "16:00"),
    )
    private val proximaMuyLejana = tarea(
        id = 12,
        titulo = "Trámite dentro de treinta días",
        entregaEn = epoch("2026-11-24", "09:00"),
    )
    private val completadaAntigua = tarea(
        id = 8,
        titulo = "Tarea antigua",
        entregaEn = epoch("2026-09-01", "12:00"),
        estado = Estado.COMPLETADA,
        completadoEn = epoch("2026-09-01", "12:00"),
    )
    private val sinHoraCompletadaHoy = tarea(
        id = 9,
        titulo = "Sin hora completada",
        entregaEn = null,
        estado = Estado.COMPLETADA,
        completadoEn = epoch("2026-10-25", "09:00"),
    )
    private val sinHoraCompletadaAntigua = tarea(
        id = 10,
        titulo = "Sin hora antigua",
        entregaEn = null,
        estado = Estado.COMPLETADA,
        completadoEn = epoch("2026-10-01", "09:00"),
    )

    private val todasLasTareas = listOf(
        atrasadaAntigua,
        atrasadaReciente,
        hoyTarde,
        hoyCompletada,
        sinHora,
        proximaCercana,
        proximaCompletada,
        proximaLejana,
        proximaMuyLejana,
        completadaAntigua,
        sinHoraCompletadaHoy,
        sinHoraCompletadaAntigua,
    )

    private fun grupos(): GruposHoy = agruparHoy(
        ahora = ahora,
        zona = utc,
        tareasDelDia = listOf(hoyTarde, hoyCompletada),
        tareasProximas = listOf(
            proximaCercana,
            proximaCompletada,
            proximaLejana,
            proximaMuyLejana,
        ),
        tareasActivas = todasLasTareas,
        materiasPorId = materias,
    )

    @Test
    fun `atrasadas ordena de la mas antigua a la mas reciente y solo pendientes`() {
        val resultado = grupos()

        // La tarea 1 es la focal (§7.3.1) y por eso no se repite en la sección.
        assertEquals(listOf(2L), resultado.atrasadas.map { it.tarea.id })
        assertEquals(1L, resultado.focal?.tareaId)
        assertEquals(TipoSeccionHoy.ATRASADAS, resultado.focal?.grupo)
    }

    @Test
    fun `para hoy ordena por hora y excluye completadas`() {
        val hoy = grupos().hoy

        assertEquals(listOf(3L), hoy.map { it.tarea.id })
        assertEquals(materia, hoy.single().materia)
    }

    @Test
    fun `sin hora usa solo pendientes sin fecha ordenadas por creacion`() {
        val sinHora = grupos().sinHora

        assertEquals(listOf(5L), sinHora.map { it.tarea.id })
    }

    @Test
    fun `proximas incluye todo el futuro pendiente sin recortar a siete dias`() {
        val proximas = grupos().proximas

        assertEquals(listOf(6L, 11L, 12L), proximas.map { it.tarea.id })
    }

    @Test
    fun `proximas conserva tareas a hoy mas ocho y hoy mas treinta`() {
        val soloFuturoLejano = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = emptyList(),
            tareasProximas = listOf(proximaLejana, proximaMuyLejana),
            tareasActivas = listOf(proximaLejana, proximaMuyLejana),
            materiasPorId = materias,
        )

        assertEquals(listOf(12L), soloFuturoLejano.proximas.map { it.tarea.id })
        assertEquals(11L, soloFuturoLejano.focal?.tareaId)
        val seccion = construirSeccionesHoy(soloFuturoLejano, FiltroHoy.TODAS)
            .single { it.tipo == TipoSeccionHoy.PROXIMAS }
        assertTrue(seccion.expandida)
        assertEquals(1, seccion.cantidad)
    }

    @Test
    fun `completadas de hoy ordena por fecha de completado descendente`() {
        val completadas = grupos().completadas

        assertEquals(listOf(4L, 9L), completadas.map { it.tarea.id })
    }

    @Test
    fun `el historial incluye todas las completadas para el filtro`() {
        val historial = grupos().historialCompletadas.map { it.tarea.id }

        assertEquals(listOf(4L, 9L, 7L, 10L, 8L), historial)
    }

    @Test
    fun `el resumen cuenta atrasadas para hoy y sin hora sin incluir proximas`() {
        val resultado = grupos()

        // 2 atrasadas + 2 de hoy + 2 sin hora relevantes (1 pendiente + 1 completada hoy)
        assertEquals(6, resultado.resumenTotal)
        assertEquals(2, resultado.resumenCompletadas)
    }

    @Test
    fun `sin tareas no hay secciones ni resumen`() {
        val vacio = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = emptyList(),
            tareasProximas = emptyList(),
            tareasActivas = emptyList(),
            materiasPorId = emptyMap(),
        )

        assertTrue(vacio.estaVacio)
        assertTrue(construirSeccionesHoy(vacio, FiltroHoy.TODAS).isEmpty())
        assertEquals(0, vacio.resumenTotal)
        assertEquals(0, vacio.resumenCompletadas)
    }

    @Test
    fun `las secciones de todas siguen el orden del documento`() {
        val secciones = construirSeccionesHoy(grupos(), FiltroHoy.TODAS)

        assertEquals(
            listOf(
                TipoSeccionHoy.ATRASADAS,
                TipoSeccionHoy.HOY,
                TipoSeccionHoy.SIN_HORA,
                TipoSeccionHoy.PROXIMAS,
                TipoSeccionHoy.COMPLETADAS,
            ),
            secciones.map { it.tipo },
        )
        assertFalse(secciones.first { it.tipo == TipoSeccionHoy.PROXIMAS }.expandida)
        assertFalse(secciones.first { it.tipo == TipoSeccionHoy.COMPLETADAS }.expandida)
        assertEquals(3, secciones.first { it.tipo == TipoSeccionHoy.PROXIMAS }.cantidad)
    }

    @Test
    fun `el filtro pendientes oculta completadas y conserva proximas`() {
        val secciones = construirSeccionesHoy(grupos(), FiltroHoy.PENDIENTES)

        assertEquals(
            listOf(
                TipoSeccionHoy.ATRASADAS,
                TipoSeccionHoy.HOY,
                TipoSeccionHoy.SIN_HORA,
                TipoSeccionHoy.PROXIMAS,
            ),
            secciones.map { it.tipo },
        )
    }

    @Test
    fun `el filtro completadas no usa secciones`() {
        assertTrue(construirSeccionesHoy(grupos(), FiltroHoy.COMPLETADAS).isEmpty())
    }

    @Test
    fun `completadas se abre por defecto si no queda nada pendiente`() {
        val soloCompletadas = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = listOf(hoyCompletada),
            tareasProximas = emptyList(),
            tareasActivas = listOf(hoyCompletada, completadaAntigua),
            materiasPorId = materias,
        )

        val secciones = construirSeccionesHoy(soloCompletadas, FiltroHoy.TODAS)
        val completadas = secciones.single { it.tipo == TipoSeccionHoy.COMPLETADAS }

        assertTrue(completadas.expandida)
        assertNull(soloCompletadas.focal)
    }

    @Test
    fun `una expansion manual manda sobre el valor por defecto`() {
        val secciones = construirSeccionesHoy(
            grupos = grupos(),
            filtro = FiltroHoy.TODAS,
            expansionManual = mapOf(TipoSeccionHoy.COMPLETADAS to true),
        )

        val completadas = secciones.first { it.tipo == TipoSeccionHoy.COMPLETADAS }
        assertTrue(completadas.expandida)
        assertTrue(completadas.contraible)
        assertNull(secciones.first { it.tipo == TipoSeccionHoy.HOY }.cantidad)
    }

    @Test
    fun `la focal desempata prioridad antes que fecha dentro del grupo`() {
        val altaAntigua = tarea(
            id = 20,
            titulo = "Alta antigua",
            entregaEn = epoch("2026-10-10", "10:00"),
            prioridad = Prioridad.ALTA,
        )
        val bajaReciente = tarea(
            id = 21,
            titulo = "Baja reciente",
            entregaEn = epoch("2026-10-24", "10:00"),
            prioridad = Prioridad.BAJA,
        )

        val resultado = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = emptyList(),
            tareasProximas = emptyList(),
            tareasActivas = listOf(altaAntigua, bajaReciente),
            materiasPorId = materias,
        )

        assertEquals(20L, resultado.focal?.tareaId)
        assertEquals(listOf(21L), resultado.atrasadas.map { it.tarea.id })
    }

    @Test
    fun `la focal respeta el orden de grupo antes que la prioridad`() {
        val atrasadaBaja = tarea(
            id = 22,
            titulo = "Atrasada baja",
            entregaEn = epoch("2026-10-24", "10:00"),
            prioridad = Prioridad.BAJA,
        )
        val hoyAlta = tarea(
            id = 23,
            titulo = "Hoy alta",
            entregaEn = epoch("2026-10-25", "18:00"),
            prioridad = Prioridad.ALTA,
        )

        val resultado = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = listOf(hoyAlta),
            tareasProximas = emptyList(),
            tareasActivas = listOf(atrasadaBaja, hoyAlta),
            materiasPorId = materias,
        )

        assertEquals(22L, resultado.focal?.tareaId)
        assertEquals(TipoSeccionHoy.ATRASADAS, resultado.focal?.grupo)
    }

    @Test
    fun `la focal de para hoy no se repite en su seccion`() {
        val soloHoy = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = listOf(hoyTarde),
            tareasProximas = emptyList(),
            tareasActivas = listOf(hoyTarde),
            materiasPorId = materias,
        )

        assertEquals(TipoSeccionHoy.HOY, soloHoy.focal?.grupo)
        assertEquals(3L, soloHoy.focal?.tareaId)
        assertTrue(soloHoy.hoy.isEmpty())
        assertTrue(
            construirSeccionesHoy(soloHoy, FiltroHoy.TODAS)
                .none { it.tipo == TipoSeccionHoy.HOY },
        )
    }

    @Test
    fun `la focal de sin hora no se repite en su seccion`() {
        val soloSinHora = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = emptyList(),
            tareasProximas = emptyList(),
            tareasActivas = listOf(sinHora),
            materiasPorId = materias,
        )

        assertEquals(TipoSeccionHoy.SIN_HORA, soloSinHora.focal?.grupo)
        assertEquals(5L, soloSinHora.focal?.tareaId)
        assertTrue(soloSinHora.sinHora.isEmpty())
        assertTrue(
            construirSeccionesHoy(soloSinHora, FiltroHoy.TODAS)
                .none { it.tipo == TipoSeccionHoy.SIN_HORA },
        )
    }

    @Test
    fun `las filas de seccion mas la focal igualan el contador de todas`() {
        val resultado = grupos()
        val secciones = construirSeccionesHoy(resultado, FiltroHoy.TODAS)
        val filas = secciones.sumOf { it.elementos.size }
        val filasFocal = if (resultado.focal == null) 0 else 1

        assertEquals(resultado.conteosFiltro.todas, filas + filasFocal)

        val filasPendientes = secciones
            .filter { it.tipo != TipoSeccionHoy.COMPLETADAS }
            .sumOf { it.elementos.size }
        assertEquals(resultado.conteosFiltro.pendientes, filasPendientes + filasFocal)
    }

    @Test
    fun `solo historial completo no vacia la pantalla y deja Completadas alcanzable`() {
        val soloHistorial = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = emptyList(),
            tareasProximas = emptyList(),
            tareasActivas = listOf(completadaAntigua, sinHoraCompletadaAntigua),
            materiasPorId = materias,
        )

        // "Todas" sigue contando solo las secciones de hoy (§7.7)...
        assertEquals(0, soloHistorial.conteosFiltro.todas)
        // ...pero hay algo que mostrar: "Completadas" tiene historial.
        assertFalse(soloHistorial.estaVacio)
        assertFalse(soloHistorial.tieneContenidoSecciones)
        assertEquals(2, soloHistorial.conteosFiltro.completadas)
        assertNull(soloHistorial.focal)
        assertTrue(construirSeccionesHoy(soloHistorial, FiltroHoy.TODAS).isEmpty())
        assertEquals(2, soloHistorial.historialCompletadas.size)
    }

    @Test
    fun `los contadores de los chips coinciden con lo que muestra cada filtro`() {
        val conteos = grupos().conteosFiltro

        assertEquals(9, conteos.todas)
        assertEquals(7, conteos.pendientes)
        assertEquals(5, conteos.completadas)
    }

    @Test
    fun `la tira semanal cuenta pendientes por dia desde el lunes`() {
        val semana = grupos().diasSemana

        assertEquals(7, semana.size)
        assertEquals(LocalDate.parse("2026-10-19"), semana.first().fecha)
        assertEquals(LocalDate.parse("2026-10-25"), semana.last().fecha)
        assertEquals(1, semana.single { it.fecha == LocalDate.parse("2026-10-20") }.pendientes)
        assertEquals(1, semana.single { it.fecha == LocalDate.parse("2026-10-24") }.pendientes)
        assertEquals(1, semana.single { it.fecha == LocalDate.parse("2026-10-25") }.pendientes)
        assertEquals(0, semana.single { it.fecha == LocalDate.parse("2026-10-21") }.pendientes)
    }

    @Test
    fun `la tira semanal puede empezar en domingo`() {
        val semana = agruparHoy(
            ahora = ahora,
            zona = utc,
            tareasDelDia = listOf(hoyTarde, hoyCompletada),
            tareasProximas = emptyList(),
            tareasActivas = todasLasTareas,
            materiasPorId = materias,
            semanaEmpiezaLunes = false,
        ).diasSemana

        assertEquals(LocalDate.parse("2026-10-25"), semana.first().fecha)
        assertEquals(LocalDate.parse("2026-10-31"), semana.last().fecha)
    }
}

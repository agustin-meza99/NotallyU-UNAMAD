package com.example.notally.ui.today

import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Task
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

    private fun epoch(date: String, time: String = "12:00"): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    private val now = epoch("2026-10-25", "12:00")

    private val subject = Subject(
        id = 1L,
        name = "Cálculo diferencial",
        colorArgb = 0xFF3A5A9E,
    )
    private val subjects = mapOf(1L to subject)

    private fun task(
        id: Long,
        title: String,
        dueAt: Long?,
        status: Status = Status.PENDING,
        priority: Priority = Priority.MEDIUM,
        createdAt: Long = epoch("2026-10-01", "08:00"),
        completedAt: Long? = null,
        subjectId: Long? = 1L,
    ) = Task(
        id = id,
        title = title,
        subjectId = subjectId,
        dueAt = dueAt,
        priority = priority,
        status = status,
        createdAt = createdAt,
        updatedAt = completedAt ?: createdAt,
        completedAt = completedAt,
    )

    private val overdueOld = task(
        id = 1,
        title = "Ensayo atrasado",
        dueAt = epoch("2026-10-20", "10:00"),
        priority = Priority.HIGH,
    )
    private val overdueRecent = task(
        id = 2,
        title = "Quiz atrasado",
        dueAt = epoch("2026-10-24", "09:00"),
        priority = Priority.LOW,
    )
    private val todayLate = task(
        id = 3,
        title = "Laboratorio",
        dueAt = epoch("2026-10-25", "18:00"),
    )
    private val todayDone = task(
        id = 4,
        title = "Lectura completada",
        dueAt = epoch("2026-10-25", "08:00"),
        status = Status.DONE,
        completedAt = epoch("2026-10-25", "10:00"),
    )
    private val anytime = task(
        id = 5,
        title = "Renovar carné",
        dueAt = null,
        priority = Priority.LOW,
    )
    private val upcomingNear = task(
        id = 6,
        title = "Taller de SQL",
        dueAt = epoch("2026-10-26", "09:00"),
    )
    private val upcomingDone = task(
        id = 7,
        title = "Exposición lista",
        dueAt = epoch("2026-10-27", "14:00"),
        status = Status.DONE,
        completedAt = epoch("2026-10-24", "12:00"),
    )
    private val upcomingFar = task(
        id = 11,
        title = "Proyecto final dentro de ocho días",
        dueAt = epoch("2026-11-02", "16:00"),
    )
    private val upcomingVeryFar = task(
        id = 12,
        title = "Trámite dentro de treinta días",
        dueAt = epoch("2026-11-24", "09:00"),
    )
    private val oldDone = task(
        id = 8,
        title = "Tarea antigua",
        dueAt = epoch("2026-09-01", "12:00"),
        status = Status.DONE,
        completedAt = epoch("2026-09-01", "12:00"),
    )
    private val noDateDoneToday = task(
        id = 9,
        title = "Sin hora completada",
        dueAt = null,
        status = Status.DONE,
        completedAt = epoch("2026-10-25", "09:00"),
    )
    private val noDateDoneOld = task(
        id = 10,
        title = "Sin hora antigua",
        dueAt = null,
        status = Status.DONE,
        completedAt = epoch("2026-10-01", "09:00"),
    )

    private val allTasks = listOf(
        overdueOld,
        overdueRecent,
        todayLate,
        todayDone,
        anytime,
        upcomingNear,
        upcomingDone,
        upcomingFar,
        upcomingVeryFar,
        oldDone,
        noDateDoneToday,
        noDateDoneOld,
    )

    private fun groups(): TodayGroups = groupToday(
        now = now,
        zone = utc,
        dayTasks = listOf(todayLate, todayDone),
        upcomingTasks = listOf(upcomingNear, upcomingDone, upcomingFar, upcomingVeryFar),
        activeTasks = allTasks,
        subjectsById = subjects,
    )

    @Test
    fun `atrasadas ordena de la mas antigua a la mas reciente y solo pendientes`() {
        val result = groups()

        // La tarea 1 es la focal (§7.3.1) y por eso no se repite en la sección.
        assertEquals(listOf(2L), result.overdue.map { it.task.id })
        assertEquals(1L, result.focal?.taskId)
        assertEquals(TodaySectionType.OVERDUE, result.focal?.bucket)
    }

    @Test
    fun `para hoy ordena por hora y excluye completadas`() {
        val today = groups().today

        assertEquals(listOf(3L), today.map { it.task.id })
        assertEquals(subject, today.single().subject)
    }

    @Test
    fun `sin hora usa solo pendientes sin fecha ordenadas por creacion`() {
        val anytime = groups().anytime

        assertEquals(listOf(5L), anytime.map { it.task.id })
    }

    @Test
    fun `proximas incluye todo el futuro pendiente sin recortar a siete dias`() {
        val upcoming = groups().upcoming

        assertEquals(listOf(6L, 11L, 12L), upcoming.map { it.task.id })
    }

    @Test
    fun `proximas conserva tareas a hoy mas ocho y hoy mas treinta`() {
        val farFutureOnly = groupToday(
            now = now,
            zone = utc,
            dayTasks = emptyList(),
            upcomingTasks = listOf(upcomingFar, upcomingVeryFar),
            activeTasks = listOf(upcomingFar, upcomingVeryFar),
            subjectsById = subjects,
        )

        assertEquals(listOf(12L), farFutureOnly.upcoming.map { it.task.id })
        assertEquals(11L, farFutureOnly.focal?.taskId)
        val section = buildTodaySections(farFutureOnly, TodayFilter.ALL)
            .single { it.type == TodaySectionType.UPCOMING }
        assertTrue(section.expanded)
        assertEquals(1, section.count)
    }

    @Test
    fun `completadas de hoy ordena por fecha de completado descendente`() {
        val completed = groups().completed

        assertEquals(listOf(4L, 9L), completed.map { it.task.id })
    }

    @Test
    fun `historial incluye todas las completadas para el filtro`() {
        val history = groups().completedHistory.map { it.task.id }

        assertEquals(listOf(4L, 9L, 7L, 10L, 8L), history)
    }

    @Test
    fun `resumen cuenta atrasadas para hoy y sin hora sin incluir proximas`() {
        val result = groups()

        // 2 atrasadas + 2 de hoy + 2 sin hora relevantes (1 pendiente + 1 completada hoy)
        assertEquals(6, result.summaryTotal)
        assertEquals(2, result.summaryCompleted)
    }

    @Test
    fun `sin tareas no hay secciones ni resumen`() {
        val empty = groupToday(
            now = now,
            zone = utc,
            dayTasks = emptyList(),
            upcomingTasks = emptyList(),
            activeTasks = emptyList(),
            subjectsById = emptyMap(),
        )

        assertTrue(empty.isEmpty)
        assertTrue(buildTodaySections(empty, TodayFilter.ALL).isEmpty())
        assertEquals(0, empty.summaryTotal)
        assertEquals(0, empty.summaryCompleted)
    }

    @Test
    fun `secciones de todas siguen el orden del documento`() {
        val sections = buildTodaySections(groups(), TodayFilter.ALL)

        assertEquals(
            listOf(
                TodaySectionType.OVERDUE,
                TodaySectionType.TODAY,
                TodaySectionType.ANYTIME,
                TodaySectionType.UPCOMING,
                TodaySectionType.COMPLETED,
            ),
            sections.map { it.type },
        )
        assertFalse(sections.first { it.type == TodaySectionType.UPCOMING }.expanded)
        assertFalse(sections.first { it.type == TodaySectionType.COMPLETED }.expanded)
        assertEquals(3, sections.first { it.type == TodaySectionType.UPCOMING }.count)
    }

    @Test
    fun `filtro pendientes oculta completadas y conserva proximas`() {
        val sections = buildTodaySections(groups(), TodayFilter.PENDING)

        assertEquals(
            listOf(
                TodaySectionType.OVERDUE,
                TodaySectionType.TODAY,
                TodaySectionType.ANYTIME,
                TodaySectionType.UPCOMING,
            ),
            sections.map { it.type },
        )
    }

    @Test
    fun `filtro completadas no usa secciones`() {
        assertTrue(buildTodaySections(groups(), TodayFilter.DONE).isEmpty())
    }

    @Test
    fun `completadas se abre por defecto si no queda nada pendiente`() {
        val onlyCompleted = groupToday(
            now = now,
            zone = utc,
            dayTasks = listOf(todayDone),
            upcomingTasks = emptyList(),
            activeTasks = listOf(todayDone, oldDone),
            subjectsById = subjects,
        )

        val sections = buildTodaySections(onlyCompleted, TodayFilter.ALL)
        val completed = sections.single { it.type == TodaySectionType.COMPLETED }

        assertTrue(completed.expanded)
        assertNull(onlyCompleted.focal)
    }

    @Test
    fun `una expansion manual manda sobre el valor por defecto`() {
        val sections = buildTodaySections(
            groups = groups(),
            filter = TodayFilter.ALL,
            overrides = mapOf(TodaySectionType.COMPLETED to true),
        )

        val completed = sections.first { it.type == TodaySectionType.COMPLETED }
        assertTrue(completed.expanded)
        assertTrue(completed.collapsible)
        assertNull(sections.first { it.type == TodaySectionType.TODAY }.count)
    }

    @Test
    fun `la focal desempata prioridad antes que fecha dentro del bucket`() {
        val highOlder = task(
            id = 20,
            title = "Alta antigua",
            dueAt = epoch("2026-10-10", "10:00"),
            priority = Priority.HIGH,
        )
        val lowNewer = task(
            id = 21,
            title = "Baja reciente",
            dueAt = epoch("2026-10-24", "10:00"),
            priority = Priority.LOW,
        )

        val result = groupToday(
            now = now,
            zone = utc,
            dayTasks = emptyList(),
            upcomingTasks = emptyList(),
            activeTasks = listOf(highOlder, lowNewer),
            subjectsById = subjects,
        )

        assertEquals(20L, result.focal?.taskId)
        assertEquals(listOf(21L), result.overdue.map { it.task.id })
    }

    @Test
    fun `la focal respeta el orden de bucket antes que la prioridad`() {
        val overdueLow = task(
            id = 22,
            title = "Atrasada baja",
            dueAt = epoch("2026-10-24", "10:00"),
            priority = Priority.LOW,
        )
        val todayHigh = task(
            id = 23,
            title = "Hoy alta",
            dueAt = epoch("2026-10-25", "18:00"),
            priority = Priority.HIGH,
        )

        val result = groupToday(
            now = now,
            zone = utc,
            dayTasks = listOf(todayHigh),
            upcomingTasks = emptyList(),
            activeTasks = listOf(overdueLow, todayHigh),
            subjectsById = subjects,
        )

        assertEquals(22L, result.focal?.taskId)
        assertEquals(TodaySectionType.OVERDUE, result.focal?.bucket)
    }

    @Test
    fun `la focal de para hoy no se repite en su seccion`() {
        val onlyToday = groupToday(
            now = now,
            zone = utc,
            dayTasks = listOf(todayLate),
            upcomingTasks = emptyList(),
            activeTasks = listOf(todayLate),
            subjectsById = subjects,
        )

        assertEquals(TodaySectionType.TODAY, onlyToday.focal?.bucket)
        assertEquals(3L, onlyToday.focal?.taskId)
        assertTrue(onlyToday.today.isEmpty())
        assertTrue(
            buildTodaySections(onlyToday, TodayFilter.ALL)
                .none { it.type == TodaySectionType.TODAY },
        )
    }

    @Test
    fun `la focal de sin hora no se repite en su seccion`() {
        val onlyAnytime = groupToday(
            now = now,
            zone = utc,
            dayTasks = emptyList(),
            upcomingTasks = emptyList(),
            activeTasks = listOf(anytime),
            subjectsById = subjects,
        )

        assertEquals(TodaySectionType.ANYTIME, onlyAnytime.focal?.bucket)
        assertEquals(5L, onlyAnytime.focal?.taskId)
        assertTrue(onlyAnytime.anytime.isEmpty())
        assertTrue(
            buildTodaySections(onlyAnytime, TodayFilter.ALL)
                .none { it.type == TodaySectionType.ANYTIME },
        )
    }

    @Test
    fun `filas de seccion mas focal igualan el contador de todas`() {
        val result = groups()
        val sections = buildTodaySections(result, TodayFilter.ALL)
        val rows = sections.sumOf { it.items.size }
        val focalRows = if (result.focal == null) 0 else 1

        assertEquals(result.filterCounts.all, rows + focalRows)

        val pendingRows = sections
            .filter { it.type != TodaySectionType.COMPLETED }
            .sumOf { it.items.size }
        assertEquals(result.filterCounts.pending, pendingRows + focalRows)
    }

    @Test
    fun `solo historial completa no vacia la pantalla y deja Completadas alcanzable`() {
        val onlyHistory = groupToday(
            now = now,
            zone = utc,
            dayTasks = emptyList(),
            upcomingTasks = emptyList(),
            activeTasks = listOf(oldDone, noDateDoneOld),
            subjectsById = subjects,
        )

        // "Todas" sigue contando solo las secciones de hoy (§7.7)...
        assertEquals(0, onlyHistory.filterCounts.all)
        // ...pero hay algo que mostrar: "Completadas" tiene historial.
        assertFalse(onlyHistory.isEmpty)
        assertFalse(onlyHistory.hasSectionContent)
        assertEquals(2, onlyHistory.filterCounts.done)
        assertNull(onlyHistory.focal)
        assertTrue(buildTodaySections(onlyHistory, TodayFilter.ALL).isEmpty())
        assertEquals(2, onlyHistory.completedHistory.size)
    }

    @Test
    fun `los contadores de los chips coinciden con lo que muestra cada filtro`() {
        val counts = groups().filterCounts

        assertEquals(9, counts.all)
        assertEquals(7, counts.pending)
        assertEquals(5, counts.done)
    }

    @Test
    fun `la tira semanal cuenta pendientes por dia desde el lunes`() {
        val week = groups().weekDays

        assertEquals(7, week.size)
        assertEquals(LocalDate.parse("2026-10-19"), week.first().date)
        assertEquals(LocalDate.parse("2026-10-25"), week.last().date)
        assertEquals(1, week.single { it.date == LocalDate.parse("2026-10-20") }.pendingCount)
        assertEquals(1, week.single { it.date == LocalDate.parse("2026-10-24") }.pendingCount)
        assertEquals(1, week.single { it.date == LocalDate.parse("2026-10-25") }.pendingCount)
        assertEquals(0, week.single { it.date == LocalDate.parse("2026-10-21") }.pendingCount)
    }

    @Test
    fun `la tira semanal puede empezar en domingo`() {
        val week = groupToday(
            now = now,
            zone = utc,
            dayTasks = listOf(todayLate, todayDone),
            upcomingTasks = emptyList(),
            activeTasks = allTasks,
            subjectsById = subjects,
            weekStartsMonday = false,
        ).weekDays

        assertEquals(LocalDate.parse("2026-10-25"), week.first().date)
        assertEquals(LocalDate.parse("2026-10-31"), week.last().date)
    }
}

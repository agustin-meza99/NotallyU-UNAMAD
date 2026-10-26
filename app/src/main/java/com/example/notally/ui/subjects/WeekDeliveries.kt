package com.example.notally.ui.subjects

import com.example.notally.data.DateRanges
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Cálculo de "N entregas esta semana" para Materias (§7.4).
 *
 * Es puro (java.time, sin Android) para que el ajuste "La semana inicia en
 * lunes" sea verificable con tests JVM y para que la UI y el test compartan la
 * misma semántica.
 *
 * Semántica decidida: cuenta las tareas no archivadas cuya [dueAt] cae dentro
 * de la semana natural actual, estén pendientes o completadas. "Entrega" es la
 * fecha límite, no el estado: una tarea completada sigue siendo una entrega de
 * esa semana. Las tareas sin fecha nunca cuentan.
 */
object WeekDeliveries {

    /** Primer día de la semana que contiene [now]. */
    fun weekStart(
        now: Long,
        weekStartsMonday: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDate {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val firstDay = if (weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        return today.with(TemporalAdjusters.previousOrSame(firstDay))
    }

    /** Rango inclusivo de siete días que cubre la semana de [now]. */
    fun weekRange(
        now: Long,
        weekStartsMonday: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        val start = weekStart(now, weekStartsMonday, zone)
        val first = DateRanges.dayRange(start, zone).first
        val last = DateRanges.dayRange(start.plusDays(6), zone).last
        return first..last
    }

    /**
     * Cuenta las fechas de entrega que caen en la semana de [now].
     * [dueDates] puede contener nulos (tareas sin fecha), que se ignoran.
     */
    fun count(
        dueDates: Iterable<Long?>,
        now: Long,
        weekStartsMonday: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val range = weekRange(now, weekStartsMonday, zone)
        return dueDates.count { dueAt -> dueAt != null && dueAt in range }
    }
}

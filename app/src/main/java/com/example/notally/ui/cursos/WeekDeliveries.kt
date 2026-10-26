package com.example.notally.ui.cursos

import com.example.notally.data.DateRanges
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Cálculo de "N entregas esta semana" para Cursos (§7.4).
 *
 * Es puro (java.time, sin Android) para que el ajuste "La semana inicia en
 * lunes" sea verificable con tests JVM y para que la UI y el test compartan la
 * misma semántica.
 *
 * Semántica decidida: cuenta las tareas no archivadas cuya [entregaEn] cae
 * dentro de la semana natural actual, estén pendientes o completadas.
 * "Entrega" es la fecha límite, no el estado: una tarea completada sigue siendo
 * una entrega de esa semana. Las tareas sin fecha nunca cuentan.
 */
object WeekDeliveries {

    /** Primer día de la semana que contiene [ahora]. */
    fun inicioSemana(
        ahora: Long,
        semanaEmpiezaLunes: Boolean,
        zona: ZoneId = ZoneId.systemDefault(),
    ): LocalDate {
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val primerDia = if (semanaEmpiezaLunes) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        return hoy.with(TemporalAdjusters.previousOrSame(primerDia))
    }

    /** Rango inclusivo de siete días que cubre la semana de [ahora]. */
    fun rangoSemana(
        ahora: Long,
        semanaEmpiezaLunes: Boolean,
        zona: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        val inicio = inicioSemana(ahora, semanaEmpiezaLunes, zona)
        val primero = DateRanges.rangoDia(inicio, zona).first
        val ultimo = DateRanges.rangoDia(inicio.plusDays(6), zona).last
        return primero..ultimo
    }

    /**
     * Cuenta las fechas de entrega que caen en la semana de [ahora].
     * [fechasEntrega] puede contener nulos (tareas sin fecha), que se ignoran.
     */
    fun contar(
        fechasEntrega: Iterable<Long?>,
        ahora: Long,
        semanaEmpiezaLunes: Boolean,
        zona: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val rango = rangoSemana(ahora, semanaEmpiezaLunes, zona)
        return fechasEntrega.count { entregaEn -> entregaEn != null && entregaEn in rango }
    }
}

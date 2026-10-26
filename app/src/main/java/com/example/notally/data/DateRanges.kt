package com.example.notally.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Cálculo puro de rangos de fechas (sin dependencias de Android) para que la
 * capa de consultas y los tests compartan exactamente la misma semántica.
 *
 * Los rangos son inclusivos en ambos extremos: `start..end`, donde `end` es el
 * último milisegundo del día.
 */
object DateRanges {

    /** Rango del día que contiene [millis] en [zonaId]. */
    fun rangoDia(
        millis: Long,
        zonaId: ZoneId = ZoneId.systemDefault(),
    ): LongRange = rangoDia(fechaLocal(millis, zonaId), zonaId)

    /** Rango del día [fecha] en [zonaId]. */
    fun rangoDia(
        fecha: LocalDate,
        zonaId: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        val inicio = fecha.atStartOfDay(zonaId).toInstant().toEpochMilli()
        val fin = fecha.plusDays(1).atStartOfDay(zonaId).toInstant().toEpochMilli() - 1
        return inicio..fin
    }

    /** Fecha local de [millis] en [zonaId]. */
    fun fechaLocal(
        millis: Long,
        zonaId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate = Instant.ofEpochMilli(millis).atZone(zonaId).toLocalDate()

    /** Primer día de la semana que contiene [fecha], según [semanaEmpiezaLunes]. */
    fun inicioSemana(
        fecha: LocalDate,
        semanaEmpiezaLunes: Boolean,
    ): LocalDate {
        val primerDia = if (semanaEmpiezaLunes) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        return fecha.with(TemporalAdjusters.previousOrSame(primerDia))
    }
}

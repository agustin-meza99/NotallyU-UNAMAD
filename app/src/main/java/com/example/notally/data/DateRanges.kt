package com.example.notally.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cálculo puro de rangos de fechas (sin dependencias de Android) para que la
 * capa de consultas y los tests compartan exactamente la misma semántica.
 *
 * Los rangos son inclusivos en ambos extremos: `start..end`, donde `end` es el
 * último milisegundo del día.
 */
object DateRanges {

    /** Rango del día que contiene [epochMillis] en [zoneId]. */
    fun dayRange(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LongRange = dayRange(toLocalDate(epochMillis, zoneId), zoneId)

    /** Rango del día [date] en [zoneId]. */
    fun dayRange(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return start..end
    }

    /**
     * Rango de [days] días naturales a partir del día que contiene
     * [epochMillis], incluyendo el día actual. Con el valor por defecto (7)
     * cubre hoy y los seis días siguientes.
     */
    fun upcomingRange(
        epochMillis: Long,
        days: Int = 7,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        require(days > 0) { "days debe ser mayor que 0" }
        val startDate = toLocalDate(epochMillis, zoneId)
        val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = startDate.plusDays(days.toLong())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1
        return start..end
    }

    private fun toLocalDate(epochMillis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
}

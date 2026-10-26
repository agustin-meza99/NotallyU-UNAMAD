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

    /** Rango del día que contiene [millis] en [zonaId]. */
    fun rangoDia(
        millis: Long,
        zonaId: ZoneId = ZoneId.systemDefault(),
    ): LongRange = rangoDia(aFechaLocal(millis, zonaId), zonaId)

    /** Rango del día [fecha] en [zonaId]. */
    fun rangoDia(
        fecha: LocalDate,
        zonaId: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        val inicio = fecha.atStartOfDay(zonaId).toInstant().toEpochMilli()
        val fin = fecha.plusDays(1).atStartOfDay(zonaId).toInstant().toEpochMilli() - 1
        return inicio..fin
    }

    /**
     * Rango de [dias] días naturales a partir del día que contiene [millis],
     * incluyendo el día actual. Con el valor por defecto (7) cubre hoy y los
     * seis días siguientes.
     */
    fun rangoProximo(
        millis: Long,
        dias: Int = 7,
        zonaId: ZoneId = ZoneId.systemDefault(),
    ): LongRange {
        require(dias > 0) { "dias debe ser mayor que 0" }
        val fechaInicio = aFechaLocal(millis, zonaId)
        val inicio = fechaInicio.atStartOfDay(zonaId).toInstant().toEpochMilli()
        val fin = fechaInicio.plusDays(dias.toLong())
            .atStartOfDay(zonaId)
            .toInstant()
            .toEpochMilli() - 1
        return inicio..fin
    }

    private fun aFechaLocal(millis: Long, zonaId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zonaId).toLocalDate()
}

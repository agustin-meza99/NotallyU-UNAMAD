package com.example.notally.ui.today

import com.example.notally.data.DateRanges
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Fecha límite al posponer "a mañana": siempre el día siguiente al de [ahora]
 * (no un día más sobre la fecha original, que mandaría una tarea atrasada al
 * pasado). Conserva la hora original si existía; si no, usa las 09:00 locales.
 *
 * Función pura para poder probarla en JVM sin Android.
 */
internal fun posponerEntrega(entregaEn: Long?, ahora: Long, zona: ZoneId): Long {
    val manana = DateRanges.fechaLocal(ahora, zona).plusDays(1)
    val hora = entregaEn
        ?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalTime() }
        ?: LocalTime.of(9, 0)
    return manana.atTime(hora).atZone(zona).toInstant().toEpochMilli()
}

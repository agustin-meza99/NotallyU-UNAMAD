package com.example.notally.ui.today

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Fecha límite al posponer "a mañana": siempre el día siguiente al de [now]
 * (no un día más sobre la fecha original, que mandaría una tarea atrasada al
 * pasado). Conserva la hora original si existía; si no, usa las 09:00 locales.
 *
 * Función pura para poder probarla en JVM sin Android.
 */
internal fun postponeDueAt(dueAt: Long?, now: Long, zone: ZoneId): Long {
    val tomorrow = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
    val time = dueAt
        ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
        ?: LocalTime.of(9, 0)
    return tomorrow.atTime(time).atZone(zone).toInstant().toEpochMilli()
}

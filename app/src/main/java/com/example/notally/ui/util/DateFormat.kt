package com.example.notally.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Etiqueta de fecha resuelta por la lógica pura.
 *
 * El texto visible no vive en Kotlin: [Today], [Tomorrow] y [Yesterday] se
 * traducen con `stringResource` en la capa de Compose, y [Short] / [Full]
 * llevan ya el formato español (día y mes, sin copy de producto).
 */
sealed interface DateLabel {
    data object Today : DateLabel
    data object Tomorrow : DateLabel
    data object Yesterday : DateLabel

    /** "Vie 31 oct" para el mismo año. */
    data class Short(val text: String) : DateLabel

    /** "31 oct 2026" para otro año. */
    data class Full(val text: String) : DateLabel
}

/**
 * Formateo de fechas y horas en español para la UI.
 *
 * Es puro (java.time, sin Android) para poder probarlo en la JVM. El ajuste
 * 12/24 h se recibe como parámetro; la capa de Compose lo obtiene del sistema.
 * Las palabras de fecha ("Hoy", "Mañana", "Ayer") y los plurales de atraso se
 * resuelven con recursos en `DateText.kt`, nunca aquí.
 */
object DateFormat {

    val SpanishLocale: Locale = Locale.forLanguageTag("es")

    private val weekdayShort: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM", SpanishLocale)
    private val dayMonthYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", SpanishLocale)
    private val eyebrowFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SpanishLocale)
    private val time12: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", SpanishLocale)
    private val time24: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", SpanishLocale)

    fun localDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * "Hoy", "Mañana", "Ayer", "Vie 31 oct" o "31 oct 2026".
     *
     * Se capitaliza el día abreviado ("vie" → "Vie") y se omite el punto, tal
     * como exige el documento.
     */
    fun dateLabel(
        dueAt: Long,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): DateLabel {
        val today = localDate(now, zone)
        return dateLabel(localDate(dueAt, zone), today)
    }

    fun dateLabel(date: LocalDate, today: LocalDate): DateLabel = when {
        date == today -> DateLabel.Today
        date == today.plusDays(1) -> DateLabel.Tomorrow
        date == today.minusDays(1) -> DateLabel.Yesterday
        date.year == today.year -> DateLabel.Short(capitalize(weekdayShort.format(date)))
        else -> DateLabel.Full(dayMonthYear.format(date))
    }

    /**
     * Días naturales transcurridos desde [dueAt] hasta hoy. 0 significa que
     * vence hoy (aunque la hora ya haya pasado); solo ≥ 1 se considera
     * "Atrasada" en la UI.
     */
    fun overdueDays(
        dueAt: Long,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int = ChronoUnit.DAYS
        .between(localDate(dueAt, zone), localDate(now, zone))
        .toInt()
        .coerceAtLeast(0)

    /**
     * Hora del sistema: "18:05" en 24 h; "6:05 p. m." en 12 h con espacio
     * fino antes de la marca a. m. / p. m.
     */
    fun formatTime(
        epochMillis: Long,
        is24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val zoned = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return if (is24Hour) {
            time24.format(zoned)
        } else {
            time12.format(zoned).replaceFirst(" ", "\u2009")
        }
    }

    /** "Domingo, 25 de octubre" en mayúsculas: "DOMINGO, 25 DE OCTUBRE". */
    fun eyebrow(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = capitalize(eyebrowFormatter.format(localDate(epochMillis, zone)))
        .uppercase(SpanishLocale)

    /** `true` si el instante no cae exactamente en la medianoche local. */
    fun hasTimeOfDay(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val zoned = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return zoned.hour != 0 || zoned.minute != 0
    }

    private fun capitalize(value: String): String =
        value.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(SpanishLocale) else char.toString()
        }
}

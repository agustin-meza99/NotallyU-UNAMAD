package com.example.notally.ui.subjects

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekDeliveriesTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(date: String, time: String = "12:00"): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    /** Domingo 25 de octubre de 2026. */
    private val now = epoch("2026-10-25", "12:00")

    private val dueDates = listOf(
        epoch("2026-10-18"), // domingo anterior: fuera con inicio en lunes
        epoch("2026-10-19", "08:00"),
        epoch("2026-10-20", "23:59"),
        epoch("2026-10-21", "00:00"),
        epoch("2026-10-25", "23:59"), // ultimo instante del domingo
        epoch("2026-10-26", "00:00"), // lunes siguiente: fuera con inicio en lunes
        epoch("2026-10-31"), // sabado siguiente
        null,
    )

    @Test
    fun `con inicio en lunes cuenta de lunes a domingo`() {
        assertEquals(4, WeekDeliveries.count(dueDates, now, weekStartsMonday = true, zone = utc))
    }

    @Test
    fun `con inicio en domingo cuenta de domingo a sabado`() {
        assertEquals(3, WeekDeliveries.count(dueDates, now, weekStartsMonday = false, zone = utc))
    }

    @Test
    fun `el rango con inicio en lunes empieza el lunes anterior`() {
        val start = WeekDeliveries.weekStart(now, weekStartsMonday = true, zone = utc)
        val range = WeekDeliveries.weekRange(now, weekStartsMonday = true, zone = utc)

        assertEquals(LocalDate.parse("2026-10-19"), start)
        assertEquals(epoch("2026-10-19", "00:00"), range.first)
        assertEquals(epoch("2026-10-25", "23:59") + 59_999, range.last)
    }

    @Test
    fun `el rango con inicio en domingo empieza el domingo anterior`() {
        val start = WeekDeliveries.weekStart(now, weekStartsMonday = false, zone = utc)
        val range = WeekDeliveries.weekRange(now, weekStartsMonday = false, zone = utc)

        assertEquals(LocalDate.parse("2026-10-25"), start)
        assertEquals(epoch("2026-10-25", "00:00"), range.first)
        assertEquals(epoch("2026-10-31", "23:59") + 59_999, range.last)
    }

    @Test
    fun `las tareas sin fecha no cuentan`() {
        assertEquals(0, WeekDeliveries.count(listOf(null, null), now, true, utc))
    }

    @Test
    fun `una semana sin entregas devuelve cero`() {
        val onlyOtherWeek = listOf(epoch("2026-11-10"), epoch("2026-10-01"))
        assertEquals(0, WeekDeliveries.count(onlyOtherWeek, now, true, utc))
    }
}

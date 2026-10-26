package com.example.notally.ui.today

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PostponeRulesTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(date: String, time: String): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    private val now = epoch("2026-10-25", "12:00")

    @Test
    fun `atrasada cinco dias pasa a manana a la misma hora`() {
        val overdue = epoch("2026-10-20", "14:30")

        val result = postponeDueAt(dueAt = overdue, now = now, zone = utc)

        assertEquals(epoch("2026-10-26", "14:30"), result)
    }

    @Test
    fun `sin hora pasa a manana a las nueve`() {
        val result = postponeDueAt(dueAt = null, now = now, zone = utc)

        assertEquals(epoch("2026-10-26", "09:00"), result)
    }

    @Test
    fun `una tarea de hoy tambien se mueve a manana y no dos dias`() {
        val today = epoch("2026-10-25", "23:15")

        val result = postponeDueAt(dueAt = today, now = now, zone = utc)

        assertEquals(epoch("2026-10-26", "23:15"), result)
    }
}

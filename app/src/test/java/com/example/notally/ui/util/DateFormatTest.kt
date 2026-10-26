package com.example.notally.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFormatTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(date: String, time: String = "12:00"): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `hoy se reconoce por dia local aunque cambie la hora`() {
        val now = epoch("2026-10-25", "12:00")

        assertEquals(DateLabel.Today, DateFormat.dateLabel(epoch("2026-10-25", "00:01"), now, utc))
        assertEquals(DateLabel.Today, DateFormat.dateLabel(epoch("2026-10-25", "23:59"), now, utc))
    }

    @Test
    fun `manana y ayer se devuelven como etiquetas traducibles`() {
        val now = epoch("2026-10-25", "12:00")

        assertEquals(DateLabel.Tomorrow, DateFormat.dateLabel(epoch("2026-10-26", "09:00"), now, utc))
        assertEquals(DateLabel.Yesterday, DateFormat.dateLabel(epoch("2026-10-24", "09:00"), now, utc))
    }

    @Test
    fun `fecha corta usa dia abreviado de tres letras sin punto`() {
        val now = epoch("2026-10-25", "12:00")

        assertEquals(
            DateLabel.Short("Sáb 31 oct"),
            DateFormat.dateLabel(epoch("2026-10-31"), now, utc),
        )
        assertEquals(
            DateLabel.Short("Dom 1 nov"),
            DateFormat.dateLabel(epoch("2026-11-01"), now, utc),
        )
    }

    @Test
    fun `otro ano usa dia mes y ano completo`() {
        val now = epoch("2026-01-15", "12:00")

        assertEquals(
            DateLabel.Full("31 oct 2025"),
            DateFormat.dateLabel(epoch("2025-10-31"), now, utc),
        )
    }

    @Test
    fun `atrasada cuenta un dia para ayer y dias para el resto`() {
        val now = epoch("2026-10-25", "12:00")

        assertEquals(1, DateFormat.overdueDays(epoch("2026-10-24", "23:59"), now, utc))
        assertEquals(2, DateFormat.overdueDays(epoch("2026-10-23", "08:00"), now, utc))
    }

    @Test
    fun `una tarea que vence hoy no se marca atrasada`() {
        val now = epoch("2026-10-25", "23:00")

        assertEquals(0, DateFormat.overdueDays(epoch("2026-10-25", "08:00"), now, utc))
    }

    @Test
    fun `los dias atrasados se cuentan por dia natural y nunca son negativos`() {
        val now = epoch("2026-10-25", "00:30")

        assertEquals(5, DateFormat.overdueDays(epoch("2026-10-20", "23:59"), now, utc))
        assertEquals(0, DateFormat.overdueDays(epoch("2026-10-26", "08:00"), now, utc))
    }

    @Test
    fun `eyebrow se escribe en mayusculas con inicial capital`() {
        assertEquals(
            "DOMINGO, 25 DE OCTUBRE",
            DateFormat.eyebrow(epoch("2026-10-25", "12:00"), utc),
        )
    }

    @Test
    fun `hora de 24 horas no lleva sufijo`() {
        assertEquals("23:59", DateFormat.formatTime(epoch("2026-10-25", "23:59"), true, utc))
        assertEquals("07:05", DateFormat.formatTime(epoch("2026-10-25", "07:05"), true, utc))
    }

    @Test
    fun `hora de 12 horas usa espacio fino antes de la marca`() {
        val formatted = DateFormat.formatTime(epoch("2026-10-25", "23:59"), false, utc)

        assertTrue(formatted.startsWith("11:59"))
        assertTrue(formatted.contains("\u2009"))
        assertTrue(formatted.contains("p."))
        assertTrue(formatted.endsWith("m."))
    }
}

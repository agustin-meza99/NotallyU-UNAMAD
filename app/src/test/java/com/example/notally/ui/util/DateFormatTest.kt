package com.example.notally.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFormatTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(fecha: String, hora: String = "12:00"): Long =
        LocalDate.parse(fecha)
            .atTime(LocalTime.parse(hora))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `hoy se reconoce por dia local aunque cambie la hora`() {
        val ahora = epoch("2026-10-25", "12:00")

        assertEquals(
            EtiquetaFecha.Hoy,
            DateFormat.etiquetaFecha(epoch("2026-10-25", "00:01"), ahora, utc),
        )
        assertEquals(
            EtiquetaFecha.Hoy,
            DateFormat.etiquetaFecha(epoch("2026-10-25", "23:59"), ahora, utc),
        )
    }

    @Test
    fun `manana y ayer se devuelven como etiquetas traducibles`() {
        val ahora = epoch("2026-10-25", "12:00")

        assertEquals(
            EtiquetaFecha.Manana,
            DateFormat.etiquetaFecha(epoch("2026-10-26", "09:00"), ahora, utc),
        )
        assertEquals(
            EtiquetaFecha.Ayer,
            DateFormat.etiquetaFecha(epoch("2026-10-24", "09:00"), ahora, utc),
        )
    }

    @Test
    fun `la fecha corta usa dia abreviado de tres letras sin punto`() {
        val ahora = epoch("2026-10-25", "12:00")

        assertEquals(
            EtiquetaFecha.Corta("Sáb 31 oct"),
            DateFormat.etiquetaFecha(epoch("2026-10-31"), ahora, utc),
        )
        assertEquals(
            EtiquetaFecha.Corta("Dom 1 nov"),
            DateFormat.etiquetaFecha(epoch("2026-11-01"), ahora, utc),
        )
    }

    @Test
    fun `otro ano usa dia mes y ano completo`() {
        val ahora = epoch("2026-01-15", "12:00")

        assertEquals(
            EtiquetaFecha.Larga("31 oct 2025"),
            DateFormat.etiquetaFecha(epoch("2025-10-31"), ahora, utc),
        )
    }

    @Test
    fun `atrasada cuenta un dia para ayer y dias para el resto`() {
        val ahora = epoch("2026-10-25", "12:00")

        assertEquals(1, DateFormat.diasAtraso(epoch("2026-10-24", "23:59"), ahora, utc))
        assertEquals(2, DateFormat.diasAtraso(epoch("2026-10-23", "08:00"), ahora, utc))
    }

    @Test
    fun `una tarea que vence hoy no se marca atrasada`() {
        val ahora = epoch("2026-10-25", "23:00")

        assertEquals(0, DateFormat.diasAtraso(epoch("2026-10-25", "08:00"), ahora, utc))
    }

    @Test
    fun `los dias atrasados se cuentan por dia natural y nunca son negativos`() {
        val ahora = epoch("2026-10-25", "00:30")

        assertEquals(5, DateFormat.diasAtraso(epoch("2026-10-20", "23:59"), ahora, utc))
        assertEquals(0, DateFormat.diasAtraso(epoch("2026-10-26", "08:00"), ahora, utc))
    }

    @Test
    fun `el antetitulo se escribe en mayusculas con inicial capital`() {
        assertEquals(
            "DOMINGO, 25 DE OCTUBRE",
            DateFormat.antetitulo(epoch("2026-10-25", "12:00"), utc),
        )
    }

    @Test
    fun `la hora de 24 horas no lleva sufijo`() {
        assertEquals(
            "23:59",
            DateFormat.formatearHora(epoch("2026-10-25", "23:59"), true, utc),
        )
        assertEquals(
            "07:05",
            DateFormat.formatearHora(epoch("2026-10-25", "07:05"), true, utc),
        )
    }

    @Test
    fun `la hora de 12 horas usa espacio fino antes de la marca`() {
        val formateada = DateFormat.formatearHora(epoch("2026-10-25", "23:59"), false, utc)

        assertTrue(formateada.startsWith("11:59"))
        assertTrue(formateada.contains("\u2009"))
        assertTrue(formateada.contains("p."))
        assertTrue(formateada.endsWith("m."))
    }

    @Test
    fun `la abreviatura del dia usa tres letras mayusculas sin punto`() {
        assertEquals("LUN", DateFormat.abreviaturaDia(LocalDate.parse("2026-10-26")))
        assertEquals("DOM", DateFormat.abreviaturaDia(LocalDate.parse("2026-10-25")))
    }

    @Test
    fun `el nombre del dia lleva inicial mayuscula`() {
        assertEquals("Domingo", DateFormat.nombreDia(LocalDate.parse("2026-10-25")))
        assertEquals("Miércoles", DateFormat.nombreDia(LocalDate.parse("2026-10-21")))
    }

    @Test
    fun `el nombre del mes va en minuscula dentro de la frase`() {
        assertEquals("octubre", DateFormat.nombreMes(LocalDate.parse("2026-10-25")))
        assertEquals("noviembre", DateFormat.nombreMes(LocalDate.parse("2026-11-01")))
    }
}

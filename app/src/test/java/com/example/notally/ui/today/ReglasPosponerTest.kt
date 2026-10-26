package com.example.notally.ui.today

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ReglasPosponerTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(fecha: String, hora: String): Long =
        LocalDate.parse(fecha)
            .atTime(LocalTime.parse(hora))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    private val ahora = epoch("2026-10-25", "12:00")

    @Test
    fun `atrasada cinco dias pasa a manana a la misma hora`() {
        val atrasada = epoch("2026-10-20", "14:30")

        val resultado = posponerEntrega(entregaEn = atrasada, ahora = ahora, zona = utc)

        assertEquals(epoch("2026-10-26", "14:30"), resultado)
    }

    @Test
    fun `sin hora pasa a manana a las nueve`() {
        val resultado = posponerEntrega(entregaEn = null, ahora = ahora, zona = utc)

        assertEquals(epoch("2026-10-26", "09:00"), resultado)
    }

    @Test
    fun `una tarea de hoy tambien se mueve a manana y no dos dias`() {
        val hoy = epoch("2026-10-25", "23:15")

        val resultado = posponerEntrega(entregaEn = hoy, ahora = ahora, zona = utc)

        assertEquals(epoch("2026-10-26", "23:15"), resultado)
    }
}

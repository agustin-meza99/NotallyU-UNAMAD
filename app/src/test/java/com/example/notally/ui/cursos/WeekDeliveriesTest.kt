package com.example.notally.ui.cursos

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekDeliveriesTest {

    private val utc = ZoneOffset.UTC

    private fun epoch(fecha: String, hora: String = "12:00"): Long =
        LocalDate.parse(fecha)
            .atTime(LocalTime.parse(hora))
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

    /** Domingo 25 de octubre de 2026. */
    private val ahora = epoch("2026-10-25", "12:00")

    private val fechasEntrega = listOf(
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
        assertEquals(
            4,
            WeekDeliveries.contar(fechasEntrega, ahora, semanaEmpiezaLunes = true, zona = utc),
        )
    }

    @Test
    fun `con inicio en domingo cuenta de domingo a sabado`() {
        assertEquals(
            3,
            WeekDeliveries.contar(fechasEntrega, ahora, semanaEmpiezaLunes = false, zona = utc),
        )
    }

    @Test
    fun `el rango con inicio en lunes empieza el lunes anterior`() {
        val inicio = WeekDeliveries.inicioSemana(ahora, semanaEmpiezaLunes = true, zona = utc)
        val rango = WeekDeliveries.rangoSemana(ahora, semanaEmpiezaLunes = true, zona = utc)

        assertEquals(LocalDate.parse("2026-10-19"), inicio)
        assertEquals(epoch("2026-10-19", "00:00"), rango.first)
        assertEquals(epoch("2026-10-25", "23:59") + 59_999, rango.last)
    }

    @Test
    fun `el rango con inicio en domingo empieza el domingo anterior`() {
        val inicio = WeekDeliveries.inicioSemana(ahora, semanaEmpiezaLunes = false, zona = utc)
        val rango = WeekDeliveries.rangoSemana(ahora, semanaEmpiezaLunes = false, zona = utc)

        assertEquals(LocalDate.parse("2026-10-25"), inicio)
        assertEquals(epoch("2026-10-25", "00:00"), rango.first)
        assertEquals(epoch("2026-10-31", "23:59") + 59_999, rango.last)
    }

    @Test
    fun `las tareas sin fecha no cuentan`() {
        assertEquals(0, WeekDeliveries.contar(listOf(null, null), ahora, true, utc))
    }

    @Test
    fun `una semana sin entregas devuelve cero`() {
        val soloOtraSemana = listOf(epoch("2026-11-10"), epoch("2026-10-01"))
        assertEquals(0, WeekDeliveries.contar(soloOtraSemana, ahora, true, utc))
    }
}

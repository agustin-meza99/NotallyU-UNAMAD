package com.example.notally.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DateRangesTest {

    private val utc = ZoneOffset.UTC

    private fun epochDe(fecha: String, hora: String, zona: ZoneId = utc): Long =
        LocalDate.parse(fecha)
            .atTime(LocalTime.parse(hora))
            .atZone(zona)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `el rango del dia empieza a medianoche y termina en el ultimo milisegundo`() {
        val mediodia = epochDe("2026-03-15", "12:00")

        val rango = DateRanges.rangoDia(mediodia, utc)

        assertEquals(epochDe("2026-03-15", "00:00"), rango.first)
        assertEquals(epochDe("2026-03-16", "00:00") - 1, rango.last)
    }

    @Test
    fun `el rango del dia funciona antes y despues del instante de referencia`() {
        val casiMedianoche = epochDe("2026-03-15", "23:59:59.999")

        val rango = DateRanges.rangoDia(casiMedianoche, utc)

        assertEquals(epochDe("2026-03-15", "00:00"), rango.first)
        assertEquals(epochDe("2026-03-16", "00:00") - 1, rango.last)

        val diaSiguiente = DateRanges.rangoDia(epochDe("2026-03-16", "00:00"), utc)
        assertEquals(epochDe("2026-03-16", "00:00"), diaSiguiente.first)
    }

    @Test
    fun `el rango proximo cubre hoy y seis dias por defecto`() {
        val mediodia = epochDe("2026-03-15", "12:00")

        val rango = DateRanges.rangoProximo(mediodia, zonaId = utc)

        assertEquals(epochDe("2026-03-15", "00:00"), rango.first)
        assertEquals(epochDe("2026-03-22", "00:00") - 1, rango.last)
        assertEquals(7L * 86_400_000L, rango.last - rango.first + 1)
    }

    @Test
    fun `el rango proximo acepta un numero de dias personalizado`() {
        val mediodia = epochDe("2026-03-15", "12:00")

        val rango = DateRanges.rangoProximo(mediodia, dias = 3, zonaId = utc)

        assertEquals(epochDe("2026-03-15", "00:00"), rango.first)
        assertEquals(epochDe("2026-03-18", "00:00") - 1, rango.last)
    }

    @Test
    fun `el rango proximo rechaza dias no positivos`() {
        val mediodia = epochDe("2026-03-15", "12:00")

        assertThrows(IllegalArgumentException::class.java) {
            DateRanges.rangoProximo(mediodia, dias = 0, zonaId = utc)
        }
    }

    @Test
    fun `el rango del dia respeta los cambios de horario de verano`() {
        val nuevaYork = ZoneId.of("America/New_York")
        val cambioPrimavera = epochDe("2026-03-08", "12:00", nuevaYork)

        val rango = DateRanges.rangoDia(cambioPrimavera, nuevaYork)

        // El 8 de marzo de 2026 tiene 23 horas en Nueva York (DST).
        assertEquals(23L * 3_600_000L, rango.last - rango.first + 1)
    }

    @Test
    fun `el rango sirve para filtrar epoch millis de forma inclusiva`() {
        val fecha = LocalDate.parse("2026-03-15")
        val rango = DateRanges.rangoDia(fecha, utc)

        val inicioDentro = Instant.ofEpochMilli(rango.first)
        val finDentro = Instant.ofEpochMilli(rango.last)

        assertEquals(fecha, inicioDentro.atZone(utc).toLocalDate())
        assertEquals(fecha, finDentro.atZone(utc).toLocalDate())
    }
}

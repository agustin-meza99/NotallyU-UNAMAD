package com.example.notally.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
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

    @Test
    fun `fechaLocal devuelve el dia civil de la zona indicada`() {
        assertEquals(
            LocalDate.parse("2026-03-15"),
            DateRanges.fechaLocal(epochDe("2026-03-15", "23:30"), utc),
        )

        // 02:00 UTC del 16 sigue siendo el 15 en Nueva York.
        assertEquals(
            LocalDate.parse("2026-03-15"),
            DateRanges.fechaLocal(
                epochDe("2026-03-16", "02:00"),
                ZoneId.of("America/New_York"),
            ),
        )
    }

    @Test
    fun `inicioSemana con lunes devuelve el lunes anterior y cruza de mes`() {
        // Domingo 1 de noviembre de 2026.
        val domingo = LocalDate.parse("2026-11-01")

        assertEquals(
            LocalDate.parse("2026-10-26"),
            DateRanges.inicioSemana(domingo, semanaEmpiezaLunes = true),
        )
    }

    @Test
    fun `inicioSemana con domingo devuelve el domingo anterior y cruza de ano`() {
        // Viernes 1 de enero de 2027.
        val viernes = LocalDate.parse("2027-01-01")

        assertEquals(
            LocalDate.parse("2026-12-27"),
            DateRanges.inicioSemana(viernes, semanaEmpiezaLunes = false),
        )
    }

    @Test
    fun `inicioSemana con lunes cruza de ano y respeta el mismo dia`() {
        assertEquals(
            LocalDate.parse("2026-12-28"),
            DateRanges.inicioSemana(
                LocalDate.parse("2027-01-01"),
                semanaEmpiezaLunes = true,
            ),
        )
        assertEquals(
            LocalDate.parse("2026-10-26"),
            DateRanges.inicioSemana(
                LocalDate.parse("2026-10-26"),
                semanaEmpiezaLunes = true,
            ),
        )
    }

    @Test
    fun `inicioSemana con domingo mantiene el domingo de inicio de semana`() {
        assertEquals(
            LocalDate.parse("2026-11-01"),
            DateRanges.inicioSemana(
                LocalDate.parse("2026-11-01"),
                semanaEmpiezaLunes = false,
            ),
        )
    }
}

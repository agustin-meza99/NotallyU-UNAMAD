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

    private fun epochOf(date: String, time: String, zone: ZoneId = utc): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `day range starts at midnight and ends at the last millisecond`() {
        val noon = epochOf("2026-03-15", "12:00")

        val range = DateRanges.dayRange(noon, utc)

        assertEquals(epochOf("2026-03-15", "00:00"), range.first)
        assertEquals(epochOf("2026-03-16", "00:00") - 1, range.last)
    }

    @Test
    fun `day range handles dates before and after the reference instant`() {
        val lateNight = epochOf("2026-03-15", "23:59:59.999")

        val range = DateRanges.dayRange(lateNight, utc)

        assertEquals(epochOf("2026-03-15", "00:00"), range.first)
        assertEquals(epochOf("2026-03-16", "00:00") - 1, range.last)

        val nextDay = DateRanges.dayRange(epochOf("2026-03-16", "00:00"), utc)
        assertEquals(epochOf("2026-03-16", "00:00"), nextDay.first)
    }

    @Test
    fun `upcoming range covers today plus six days by default`() {
        val noon = epochOf("2026-03-15", "12:00")

        val range = DateRanges.upcomingRange(noon, zoneId = utc)

        assertEquals(epochOf("2026-03-15", "00:00"), range.first)
        assertEquals(epochOf("2026-03-22", "00:00") - 1, range.last)
        assertEquals(7L * 86_400_000L, range.last - range.first + 1)
    }

    @Test
    fun `upcoming range accepts a custom number of days`() {
        val noon = epochOf("2026-03-15", "12:00")

        val range = DateRanges.upcomingRange(noon, days = 3, zoneId = utc)

        assertEquals(epochOf("2026-03-15", "00:00"), range.first)
        assertEquals(epochOf("2026-03-18", "00:00") - 1, range.last)
    }

    @Test
    fun `upcoming range rejects non positive days`() {
        val noon = epochOf("2026-03-15", "12:00")

        assertThrows(IllegalArgumentException::class.java) {
            DateRanges.upcomingRange(noon, days = 0, zoneId = utc)
        }
    }

    @Test
    fun `day range respects daylight saving transitions`() {
        val newYork = ZoneId.of("America/New_York")
        val springForward = epochOf("2026-03-08", "12:00", newYork)

        val range = DateRanges.dayRange(springForward, newYork)

        // El 8 de marzo de 2026 tiene 23 horas en Nueva York (DST).
        assertEquals(23L * 3_600_000L, range.last - range.first + 1)
    }

    @Test
    fun `range can be used to filter epoch millis inclusively`() {
        val date = LocalDate.parse("2026-03-15")
        val range = DateRanges.dayRange(date, utc)

        val insideStart = Instant.ofEpochMilli(range.first)
        val insideEnd = Instant.ofEpochMilli(range.last)

        assertEquals(date, insideStart.atZone(utc).toLocalDate())
        assertEquals(date, insideEnd.atZone(utc).toLocalDate())
    }
}

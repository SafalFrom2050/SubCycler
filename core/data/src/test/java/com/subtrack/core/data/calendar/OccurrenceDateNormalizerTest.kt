package com.subtrack.core.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OccurrenceDateNormalizerTest {
    @Test
    fun `all day occurrence keeps provider exclusive utc end date`() {
        val result = normalizeOccurrenceDates(
            Instant.parse("2026-09-10T00:00:00Z"),
            Instant.parse("2026-09-12T00:00:00Z"),
            allDay = true,
            zone = ZoneId.of("Asia/Tokyo")
        )

        assertEquals(LocalDate.of(2026, 9, 10), result.startDate)
        assertEquals(LocalDate.of(2026, 9, 12), result.endDateExclusive)
    }

    @Test
    fun `timed event ending at midnight does not include following date`() {
        val result = normalizeOccurrenceDates(
            Instant.parse("2026-09-10T13:00:00Z"),
            Instant.parse("2026-09-10T15:00:00Z"),
            allDay = false,
            zone = ZoneId.of("Asia/Tokyo")
        )

        assertEquals(LocalDate.of(2026, 9, 10), result.startDate)
        assertEquals(LocalDate.of(2026, 9, 11), result.endDateExclusive)
    }

    @Test
    fun `dst transition preserves local calendar dates`() {
        val result = normalizeOccurrenceDates(
            Instant.parse("2026-03-08T06:30:00Z"),
            Instant.parse("2026-03-08T07:30:00Z"),
            allDay = false,
            zone = ZoneId.of("America/New_York")
        )

        assertEquals(LocalDate.of(2026, 3, 8), result.startDate)
        assertEquals(LocalDate.of(2026, 3, 9), result.endDateExclusive)
    }
}

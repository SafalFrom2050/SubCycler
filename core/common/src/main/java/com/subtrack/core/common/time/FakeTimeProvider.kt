package com.subtrack.core.common.time

import java.time.Instant
import java.time.LocalDate

class FakeTimeProvider(
    private var currentDate: LocalDate = LocalDate.of(2026, 8, 18),
    private var currentInstant: Instant = Instant.parse("2026-08-18T10:00:00Z")
) : TimeProvider {

    override fun today(): LocalDate = currentDate

    override fun nowInstant(): Instant = currentInstant

    fun setDate(date: LocalDate) {
        currentDate = date
    }

    fun setInstant(instant: Instant) {
        currentInstant = instant
    }
}

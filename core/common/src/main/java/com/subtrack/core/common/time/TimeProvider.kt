package com.subtrack.core.common.time

import java.time.Instant
import java.time.LocalDate

/**
 * Abstraction for system time to ensure 100% deterministic testing and time-travel debugging.
 */
interface TimeProvider {
    fun today(): LocalDate
    fun nowInstant(): Instant
}

/**
 * Default production implementation backed by system clock.
 */
class DefaultTimeProvider : TimeProvider {
    override fun today(): LocalDate = LocalDate.now()
    override fun nowInstant(): Instant = Instant.now()
}

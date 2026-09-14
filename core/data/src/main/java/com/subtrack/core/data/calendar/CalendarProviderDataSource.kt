package com.subtrack.core.data.calendar

import com.subtrack.core.domain.model.CalendarOccurrence
import com.subtrack.core.domain.model.DeviceCalendar
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CalendarProviderDataSource {
    fun isReadPermissionGranted(): Boolean
    suspend fun getCalendars(): List<DeviceCalendar>
    suspend fun getOccurrences(
        selectedCalendarIds: Set<Long>,
        windowStart: Instant,
        windowEndExclusive: Instant
    ): List<CalendarOccurrence>
    fun observeChanges(): Flow<Unit>
}

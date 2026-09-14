package com.subtrack.core.domain.repository

import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarAwarenessSettings
import com.subtrack.core.domain.model.CalendarSuggestionIdentity
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.DeviceCalendar
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface CalendarAwarenessRepository {
    val settings: Flow<CalendarAwarenessSettings>
    val suggestions: Flow<List<CalendarAwareSuggestion>>

    suspend fun isReadPermissionGranted(): Boolean
    suspend fun getAccessibleCalendars(): List<DeviceCalendar>
    suspend fun enable(selectedCalendarIds: Set<Long>)
    suspend fun updateSelection(selectedCalendarIds: Set<Long>)
    suspend fun disable(issue: CalendarAccessIssue? = null)
    suspend fun refresh(cycleStatuses: List<CycleStatus>, today: LocalDate): List<CalendarAwareSuggestion>
    suspend fun dismiss(identity: CalendarSuggestionIdentity)
    suspend fun claimNotification(identity: CalendarSuggestionIdentity, now: Instant): Boolean
    fun observeCalendarChanges(): Flow<Unit>
}

interface CalendarAwarenessScheduler {
    fun enable()
    fun disable()
}

package com.subtrack.core.domain.testing

import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarAwarenessSettings
import com.subtrack.core.domain.model.CalendarSuggestionIdentity
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.domain.repository.CalendarAwarenessRepository
import com.subtrack.core.domain.repository.CalendarAwarenessScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate

class FakeCalendarAwarenessRepository(
    initialSettings: CalendarAwarenessSettings = CalendarAwarenessSettings(),
    private var permissionGranted: Boolean = false,
    private var availableCalendars: List<DeviceCalendar> = emptyList(),
    initialSuggestions: List<CalendarAwareSuggestion> = emptyList()
) : CalendarAwarenessRepository {
    private val settingsFlow = MutableStateFlow(initialSettings)
    private val suggestionsFlow = MutableStateFlow(initialSuggestions)
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val settings: Flow<CalendarAwarenessSettings> = settingsFlow.asStateFlow()
    override val suggestions: Flow<List<CalendarAwareSuggestion>> = suggestionsFlow.asStateFlow()

    override suspend fun isReadPermissionGranted(): Boolean = permissionGranted
    override suspend fun getAccessibleCalendars(): List<DeviceCalendar> =
        if (permissionGranted) availableCalendars else emptyList()

    override suspend fun enable(selectedCalendarIds: Set<Long>) {
        settingsFlow.value = settingsFlow.value.copy(
            isEnabled = true,
            selectedCalendarIds = selectedCalendarIds,
            generation = settingsFlow.value.generation + 1,
            accessIssue = null
        )
    }

    override suspend fun updateSelection(selectedCalendarIds: Set<Long>) = enable(selectedCalendarIds)

    override suspend fun disable(issue: CalendarAccessIssue?) {
        settingsFlow.value = CalendarAwarenessSettings(
            isEnabled = false,
            generation = settingsFlow.value.generation + 1,
            accessIssue = issue
        )
        suggestionsFlow.value = emptyList()
    }

    override suspend fun refresh(
        cycleStatuses: List<CycleStatus>,
        today: LocalDate
    ): List<CalendarAwareSuggestion> = if (settingsFlow.value.isEnabled && permissionGranted) {
        suggestionsFlow.value
    } else {
        emptyList()
    }

    override suspend fun dismiss(identity: CalendarSuggestionIdentity) {
        suggestionsFlow.value = suggestionsFlow.value.filterNot { it.identity == identity }
    }

    override suspend fun claimNotification(identity: CalendarSuggestionIdentity, now: Instant): Boolean = true
    override fun observeCalendarChanges(): Flow<Unit> = changes

    fun setPermissionGranted(granted: Boolean) {
        permissionGranted = granted
    }

    fun setAvailableCalendars(calendars: List<DeviceCalendar>) {
        availableCalendars = calendars
    }

    fun setSuggestions(suggestions: List<CalendarAwareSuggestion>) {
        suggestionsFlow.value = suggestions
    }

    fun emitChange() {
        changes.tryEmit(Unit)
    }
}

class FakeCalendarAwarenessScheduler : CalendarAwarenessScheduler {
    var isEnabled: Boolean = false
        private set

    override fun enable() {
        isEnabled = true
    }

    override fun disable() {
        isEnabled = false
    }
}

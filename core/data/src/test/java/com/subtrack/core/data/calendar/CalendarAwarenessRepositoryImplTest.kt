package com.subtrack.core.data.calendar

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CalendarOccurrence
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.CalendarAwarenessRepository
import com.subtrack.core.domain.usecase.GetCalendarAwareSuggestionsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.LocalDate

class CalendarAwarenessRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val today = LocalDate.of(2026, 9, 10)

    @Test
    fun `off by default performs zero provider access`() = runTest {
        val provider = FakeCalendarProvider()
        val repository = repository(provider)

        val result = repository.refresh(listOf(status()), today)

        assertTrue(result.isEmpty())
        assertEquals(0, provider.calendarQueries)
        assertEquals(0, provider.occurrenceQueries)
        assertFalse(repository.settings.first().isEnabled)
    }

    @Test
    fun `configuration lists calendars but does not read occurrences`() = runTest {
        val provider = FakeCalendarProvider()
        val repository = repository(provider)

        val calendars = repository.getAccessibleCalendars()

        assertEquals(listOf(7L, 9L), calendars.map { it.id })
        assertEquals(1, provider.calendarQueries)
        assertEquals(0, provider.occurrenceQueries)
        assertFalse(repository.settings.first().isEnabled)
    }

    @Test
    fun `refresh queries only confirmed selected calendar ids`() = runTest {
        val provider = FakeCalendarProvider()
        val repository = repository(provider)
        repository.enable(setOf(9L))

        repository.refresh(listOf(status()), today)

        assertEquals(setOf(9L), provider.lastSelectedIds)
        assertEquals(1, provider.occurrenceQueries)
    }

    @Test
    fun `disable while query is in flight prevents stale suggestion commit`() = runTest {
        val queryStarted = CompletableDeferred<Unit>()
        val releaseQuery = CompletableDeferred<Unit>()
        val provider = FakeCalendarProvider(queryStarted, releaseQuery)
        val repository = repository(provider)
        repository.enable(setOf(7L))

        val refresh = async { repository.refresh(listOf(status()), today) }
        queryStarted.await()
        repository.disable()
        releaseQuery.complete(Unit)

        assertTrue(refresh.await().isEmpty())
        assertFalse(repository.settings.first().isEnabled)
        assertTrue(repository.settings.first().selectedCalendarIds.isEmpty())
        assertTrue(repository.suggestions.first().isEmpty())
    }

    @Test
    fun `notification claim is once per identity and capped for twenty four hours`() = runTest {
        val provider = FakeCalendarProvider()
        val repository = repository(provider)
        repository.enable(setOf(7L))
        val suggestion = repository.refresh(listOf(status()), today).single()
        val now = Instant.parse("2026-09-10T00:00:00Z")

        assertTrue(repository.claimNotification(suggestion.identity, now))
        assertFalse(repository.claimNotification(suggestion.identity, now.plusSeconds(25 * 60 * 60)))
    }

    private fun repository(provider: FakeCalendarProvider): CalendarAwarenessRepository {
        val file = temporaryFolder.newFile("calendar-${System.nanoTime()}.preferences_pb")
        file.delete()
        val dataStore = PreferenceDataStoreFactory.create { file }
        return CalendarAwarenessRepositoryImpl(dataStore, provider, GetCalendarAwareSuggestionsUseCase())
    }

    private fun status() = RotationEngine.calculateCycleStatus(
        Subscription(
            id = 1,
            name = "Netflix",
            cost = 12.0,
            startDate = today.minusDays(25),
            strategy = RotationStrategy.ROTATION_45_DAYS
        ),
        today
    )

    private inner class FakeCalendarProvider(
        private val queryStarted: CompletableDeferred<Unit>? = null,
        private val releaseQuery: CompletableDeferred<Unit>? = null
    ) : CalendarProviderDataSource {
        var permissionGranted = true
        var calendarQueries = 0
        var occurrenceQueries = 0
        var lastSelectedIds: Set<Long> = emptySet()

        override fun isReadPermissionGranted(): Boolean = permissionGranted

        override suspend fun getCalendars(): List<DeviceCalendar> {
            calendarQueries++
            return listOf(
                DeviceCalendar(7, "Personal", "person@example.com", "Google"),
                DeviceCalendar(9, "Personal", "work@example.com", "Exchange")
            )
        }

        override suspend fun getOccurrences(
            selectedCalendarIds: Set<Long>,
            windowStart: Instant,
            windowEndExclusive: Instant
        ): List<CalendarOccurrence> {
            occurrenceQueries++
            lastSelectedIds = selectedCalendarIds
            queryStarted?.complete(Unit)
            releaseQuery?.await()
            return listOf(
                CalendarOccurrence(
                    identity = "7:12:1",
                    calendarId = selectedCalendarIds.first(),
                    eventId = 12,
                    title = "Vacation",
                    startDate = today.plusDays(4),
                    endDateExclusive = today.plusDays(7),
                    isAllDay = true
                )
            )
        }

        override fun observeChanges(): Flow<Unit> = emptyFlow()
    }
}

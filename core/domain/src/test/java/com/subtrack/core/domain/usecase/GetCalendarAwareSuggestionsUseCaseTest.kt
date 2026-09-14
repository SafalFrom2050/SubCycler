package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CalendarOccurrence
import com.subtrack.core.domain.model.CalendarSuggestionDismissal
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetCalendarAwareSuggestionsUseCaseTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val useCase = GetCalendarAwareSuggestionsUseCase()

    @Test
    fun `two day vacation overlapping entertainment decision suggests cancellation review`() {
        val status = activeStatus(category = SubscriptionCategory.STREAMING)
        val occurrence = occurrence("vacation", today.plusDays(4), today.plusDays(7))

        val result = useCase(listOf(status), listOf(occurrence), emptyList(), today)

        assertEquals(1, result.size)
        assertEquals(CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION, result.single().identity.suggestionType)
        assertEquals("Netflix", result.single().subscriptionName)
    }

    @Test
    fun `business travel ambiguous substring short trip and cloud service stay silent`() {
        val entertainment = activeStatus(category = SubscriptionCategory.GAMING)
        val cloud = activeStatus(category = SubscriptionCategory.CLOUD, id = 2, name = "Drive")
        val occurrences = listOf(
            occurrence("Business trip", today.plusDays(4), today.plusDays(7), "business"),
            occurrence("Client travel", today.plusDays(4), today.plusDays(7), "client"),
            occurrence("Roadtrip planning", today.plusDays(4), today.plusDays(7), "substring"),
            occurrence("Vacation", today.plusDays(5), today.plusDays(6), "short")
        )

        assertTrue(useCase(listOf(entertainment, cloud), occurrences, emptyList(), today).isEmpty())
    }

    @Test
    fun `named festival during entertainment blackout creates neutral review`() {
        val status = blackoutStatus()
        val occurrence = occurrence("Dashain", today.plusDays(1), today.plusDays(3))

        val result = useCase(listOf(status), listOf(occurrence), emptyList(), today)

        assertEquals(1, result.size)
        assertEquals(CalendarSuggestionType.HOLIDAY_REVIEW_SUBSCRIPTION, result.single().identity.suggestionType)
    }

    @Test
    fun `dismissal survives a title edit for the same occurrence identity`() {
        val status = activeStatus(category = SubscriptionCategory.STREAMING)
        val original = occurrence("Vacation", today.plusDays(4), today.plusDays(7))
        val first = useCase(listOf(status), listOf(original), emptyList(), today).single()
        val edited = original.copy(title = "Family vacation")
        val dismissal = CalendarSuggestionDismissal(first.identity, original.endDateExclusive)

        val result = useCase(listOf(status), listOf(edited), listOf(dismissal), today)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `separate recurring occurrences have separate identities`() {
        val status = activeStatus(category = SubscriptionCategory.STREAMING)
        val dismissedOccurrence = occurrence("Vacation", today.plusDays(4), today.plusDays(7), "event-1-100")
        val nextOccurrence = occurrence("Vacation", today.plusDays(4), today.plusDays(7), "event-1-200")
        val dismissedSuggestion = useCase(listOf(status), listOf(dismissedOccurrence), emptyList(), today).single()

        val result = useCase(
            listOf(status),
            listOf(nextOccurrence),
            listOf(CalendarSuggestionDismissal(dismissedSuggestion.identity, dismissedOccurrence.endDateExclusive)),
            today
        )

        assertEquals("event-1-200", result.single().identity.occurrenceIdentity)
    }

    private fun activeStatus(
        category: SubscriptionCategory,
        id: Long = 1,
        name: String = "Netflix"
    ) = RotationEngine.calculateCycleStatus(
        Subscription(
            id = id,
            name = name,
            cost = 10.0,
            category = category,
            startDate = today.minusDays(25),
            strategy = RotationStrategy.ROTATION_45_DAYS
        ),
        today
    )

    private fun blackoutStatus() = RotationEngine.calculateCycleStatus(
        Subscription(
            id = 1,
            name = "Disney+",
            cost = 10.0,
            category = SubscriptionCategory.STREAMING,
            startDate = today.minusDays(40),
            strategy = RotationStrategy.ROTATION_45_DAYS
        ),
        today
    )

    private fun occurrence(
        title: String,
        start: LocalDate,
        endExclusive: LocalDate,
        identity: String = "event-1-100"
    ) = CalendarOccurrence(
        identity = identity,
        calendarId = 1,
        eventId = 1,
        title = title,
        startDate = start,
        endDateExclusive = endExclusive,
        isAllDay = true
    )
}

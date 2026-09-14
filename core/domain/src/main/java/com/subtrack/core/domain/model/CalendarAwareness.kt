package com.subtrack.core.domain.model

import java.time.Instant
import java.time.LocalDate

enum class CalendarAccessState {
    DISABLED,
    CONFIGURING,
    ENABLED
}

enum class CalendarAccessIssue {
    PERMISSION_DENIED,
    PERMISSION_REVOKED,
    NO_ACCESSIBLE_CALENDARS,
    PROVIDER_UNAVAILABLE
}

data class CalendarAwarenessSettings(
    val isEnabled: Boolean = false,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val generation: Long = 0L,
    val accessIssue: CalendarAccessIssue? = null
)

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String
) {
    val accountContext: String
        get() = listOf(accountName, accountType)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · ")
}

/**
 * A normalized occurrence. [title] is transient classifier input and must not be persisted.
 * [endDateExclusive] preserves Calendar Provider's exclusive all-day end semantics.
 */
data class CalendarOccurrence(
    val identity: String,
    val calendarId: Long,
    val eventId: Long,
    val title: String,
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val isAllDay: Boolean,
    val startInstant: Instant? = null,
    val endInstant: Instant? = null,
    val timeZoneId: String? = null
)

enum class CalendarSuggestionType {
    TRAVEL_REVIEW_CANCELLATION,
    HOLIDAY_REVIEW_SUBSCRIPTION
}

data class CalendarSuggestionIdentity(
    val subscriptionId: Long,
    val occurrenceIdentity: String,
    val suggestionType: CalendarSuggestionType
)

data class CalendarSuggestionDismissal(
    val identity: CalendarSuggestionIdentity,
    val occurrenceEndDateExclusive: LocalDate
)

data class CalendarAwareSuggestion(
    val identity: CalendarSuggestionIdentity,
    val subscriptionName: String,
    val decisionDate: LocalDate,
    val occurrenceEndDateExclusive: LocalDate,
    val confidence: Int
) {
    val message: String
        get() = when (identity.suggestionType) {
            CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION ->
                "Your upcoming trip overlaps $subscriptionName's next planned cycle. Review whether you'll need it."
            CalendarSuggestionType.HOLIDAY_REVIEW_SUBSCRIPTION ->
                "A holiday on your calendar falls during $subscriptionName's planned break. Review your subscription."
        }

    val primaryActionLabel: String
        get() = when (identity.suggestionType) {
            CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION -> "Review cancellation"
            CalendarSuggestionType.HOLIDAY_REVIEW_SUBSCRIPTION -> "Review subscription"
        }
}

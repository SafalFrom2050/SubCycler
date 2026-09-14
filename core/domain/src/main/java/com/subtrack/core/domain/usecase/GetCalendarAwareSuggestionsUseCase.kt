package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.model.ActionType
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarOccurrence
import com.subtrack.core.domain.model.CalendarSuggestionDismissal
import com.subtrack.core.domain.model.CalendarSuggestionIdentity
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.SubscriptionCategory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetCalendarAwareSuggestionsUseCase @Inject constructor() {

    operator fun invoke(
        cycleStatuses: List<CycleStatus>,
        occurrences: List<CalendarOccurrence>,
        dismissals: List<CalendarSuggestionDismissal>,
        today: LocalDate
    ): List<CalendarAwareSuggestion> {
        val dismissed = dismissals
            .filter { it.occurrenceEndDateExclusive.isAfter(today) }
            .mapTo(mutableSetOf()) { it.identity }

        return cycleStatuses.flatMap { status ->
            occurrences.mapNotNull { occurrence ->
                createSuggestion(status, occurrence, today)
            }
        }
            .filterNot { it.identity in dismissed }
            .distinctBy { it.identity }
            .sortedWith(
                compareBy<CalendarAwareSuggestion> {
                    val status = cycleStatuses.first { candidate ->
                        candidate.subscription.id == it.identity.subscriptionId
                    }
                    when (status.nextAction.urgency) {
                        ActionUrgency.ACTION_DUE -> 0
                        ActionUrgency.WARNING -> 1
                        ActionUrgency.INFO -> 2
                    }
                }
                    .thenBy { it.decisionDate }
                    .thenByDescending { it.confidence }
                    .thenBy { it.identity.subscriptionId }
                    .thenBy { it.identity.occurrenceIdentity }
            )
    }

    private fun createSuggestion(
        status: CycleStatus,
        occurrence: CalendarOccurrence,
        today: LocalDate
    ): CalendarAwareSuggestion? {
        val decisionDate = status.nextAction.targetDate
        if (status.nextAction.actionType == ActionType.NO_ACTION) return null
        if (decisionDate.isBefore(today) || decisionDate.isAfter(today.plusDays(7))) return null

        val classification = CalendarTitleClassifier.classify(occurrence.title) ?: return null
        return when (classification.kind) {
            CalendarEventKind.TRAVEL -> travelSuggestion(status, occurrence, decisionDate, classification.confidence)
            CalendarEventKind.HOLIDAY_OR_FESTIVAL -> holidaySuggestion(status, occurrence, decisionDate, classification.confidence)
        }
    }

    private fun travelSuggestion(
        status: CycleStatus,
        occurrence: CalendarOccurrence,
        decisionDate: LocalDate,
        confidence: Int
    ): CalendarAwareSuggestion? {
        if (status.subscription.category !in ENTERTAINMENT_CATEGORIES) return null
        if (status.nextAction.actionType != ActionType.CANCEL_AUTO_RENEW) return null
        if (ChronoUnit.DAYS.between(occurrence.startDate, occurrence.endDateExclusive) < 2) return null
        if (!occurrence.overlaps(decisionDate)) return null

        return suggestion(
            status = status,
            occurrence = occurrence,
            decisionDate = decisionDate,
            type = CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION,
            confidence = confidence
        )
    }

    private fun holidaySuggestion(
        status: CycleStatus,
        occurrence: CalendarOccurrence,
        decisionDate: LocalDate,
        confidence: Int
    ): CalendarAwareSuggestion? {
        if (status.subscription.category !in ENTERTAINMENT_CATEGORIES) return null
        val overlapsBlackout = status.timelineBlocks.any { block ->
            block.phase == CyclePhase.BLACKOUT &&
                occurrence.startDate.isBefore(block.endDate) &&
                occurrence.endDateExclusive.isAfter(block.startDate)
        }
        if (!overlapsBlackout) return null

        return suggestion(
            status = status,
            occurrence = occurrence,
            decisionDate = decisionDate,
            type = CalendarSuggestionType.HOLIDAY_REVIEW_SUBSCRIPTION,
            confidence = confidence
        )
    }

    private fun suggestion(
        status: CycleStatus,
        occurrence: CalendarOccurrence,
        decisionDate: LocalDate,
        type: CalendarSuggestionType,
        confidence: Int
    ): CalendarAwareSuggestion {
        val identity = CalendarSuggestionIdentity(
            subscriptionId = status.subscription.id,
            occurrenceIdentity = occurrence.identity,
            suggestionType = type
        )
        return CalendarAwareSuggestion(
            identity = identity,
            subscriptionName = status.subscription.name,
            decisionDate = decisionDate,
            occurrenceEndDateExclusive = occurrence.endDateExclusive,
            confidence = confidence
        )
    }

    private fun CalendarOccurrence.overlaps(date: LocalDate): Boolean =
        !date.isBefore(startDate) && date.isBefore(endDateExclusive)

    private companion object {
        val ENTERTAINMENT_CATEGORIES = setOf(SubscriptionCategory.STREAMING, SubscriptionCategory.GAMING)
    }
}

private enum class CalendarEventKind {
    TRAVEL,
    HOLIDAY_OR_FESTIVAL
}

private data class CalendarClassification(
    val kind: CalendarEventKind,
    val confidence: Int
)

private object CalendarTitleClassifier {
    private val workTravel = phraseRegex(
        "business trip", "business travel", "business tour",
        "work trip", "work travel", "work tour",
        "conference trip", "conference travel",
        "client trip", "client travel", "customer trip", "customer travel"
    )
    private val namedFestival = phraseRegex(
        "dashain", "dasain", "tihar", "diwali", "deepavali", "holi", "eid", "christmas", "new year", "festival"
    )
    private val travel = phraseRegex("trip", "vacation", "travel", "getaway", "holiday abroad")
    private val genericHoliday = phraseRegex("holiday", "public holiday")

    fun classify(title: String): CalendarClassification? {
        val normalized = title.trim().lowercase()
        if (normalized.isEmpty()) return null
        if (workTravel.containsMatchIn(normalized)) return null
        if (namedFestival.containsMatchIn(normalized)) {
            return CalendarClassification(CalendarEventKind.HOLIDAY_OR_FESTIVAL, 3)
        }
        if (travel.containsMatchIn(normalized)) {
            return CalendarClassification(CalendarEventKind.TRAVEL, 2)
        }
        if (genericHoliday.containsMatchIn(normalized)) {
            return CalendarClassification(CalendarEventKind.HOLIDAY_OR_FESTIVAL, 1)
        }
        return null
    }

    private fun phraseRegex(vararg phrases: String): Regex {
        val alternatives = phrases.joinToString("|") { Regex.escape(it) }
        return Regex("(?<![\\p{L}\\p{N}])(?:$alternatives)(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    }
}

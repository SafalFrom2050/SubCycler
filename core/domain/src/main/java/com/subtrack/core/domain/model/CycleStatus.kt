package com.subtrack.core.domain.model

import java.time.LocalDate

enum class CyclePhase {
    ACTIVE,
    BLACKOUT
}

enum class ActionUrgency {
    INFO,
    WARNING,     // Cancel auto-renew coming up in <= 3 days
    ACTION_DUE   // Resubscribe today or Cancel today!
}

data class NextAction(
    val title: String,
    val description: String,
    val targetDate: LocalDate,
    val urgency: ActionUrgency,
    val actionType: ActionType
)

enum class ActionType {
    CANCEL_AUTO_RENEW,
    RESUBSCRIBE,
    NO_ACTION
}

data class TimelineBlock(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val phase: CyclePhase,
    val isCurrent: Boolean
)

data class CycleStatus(
    val subscription: Subscription,
    val phase: CyclePhase,
    val currentCycleNumber: Int,
    val dayInCycle: Int,           // e.g. Day 12 of 45
    val daysRemainingInPhase: Int, // e.g. 18 days left in ACTIVE, or 4 days left in BLACKOUT
    val phaseStartDate: LocalDate,
    val phaseEndDate: LocalDate,
    val progressFraction: Float,   // 0.0 to 1.0 of current phase
    val nextAction: NextAction,
    val timelineBlocks: List<TimelineBlock>, // 6-month projected timeline
    
    // Financial savings metrics
    val standardAnnualCost: Double,
    val optimizedAnnualCost: Double,
    val annualSavings: Double,
    val savingsPercentage: Double
)

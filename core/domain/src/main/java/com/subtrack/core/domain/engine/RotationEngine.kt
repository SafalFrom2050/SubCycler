package com.subtrack.core.domain.engine

import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

object RotationEngine {

    fun calculateCycleStatus(
        subscription: Subscription,
        today: LocalDate = LocalDate.now()
    ): CycleStatus {
        val totalCycleDays = subscription.totalCycleDays
        val activeDays = subscription.activeDays
        val blackoutDays = subscription.blackoutDays

        // Handle edge case where subscription starts in the future
        val referenceStart = subscription.startDate
        val daysSinceStart = ChronoUnit.DAYS.between(referenceStart, today)

        val standardAnnualCost = subscription.cost * 12.0

        if (blackoutDays <= 0 || subscription.strategy == RotationStrategy.CONTINUOUS) {
            // Continuous 30-day standard billing cycle
            val cycleDays = if (activeDays > 0) activeDays else 30
            val normalizedDays = if (daysSinceStart >= 0) daysSinceStart else 0
            val cycleIndex = (normalizedDays / cycleDays).toInt()
            val dayInCycle = (normalizedDays % cycleDays).toInt() + 1
            val daysRemaining = cycleDays - dayInCycle
            val cycleStart = referenceStart.plusDays((cycleIndex * cycleDays).toLong())
            val cycleEnd = cycleStart.plusDays(cycleDays.toLong())
            val progressFraction = (dayInCycle.toFloat() / cycleDays).coerceIn(0f, 1f)

            val nextAction = NextAction(
                title = "Continuous Renewal",
                description = "Renews in $daysRemaining days on $cycleEnd (${MoneyFormatter.format(subscription.cost, subscription.currency)})",
                targetDate = cycleEnd,
                urgency = if (daysRemaining <= 3) ActionUrgency.WARNING else ActionUrgency.INFO,
                actionType = ActionType.CANCEL_AUTO_RENEW
            )

            val timeline = generateTimeline(
                startDate = cycleStart,
                activeDays = cycleDays,
                blackoutDays = 0,
                cyclesToGenerate = 6,
                today = today
            )

            return CycleStatus(
                subscription = subscription,
                phase = CyclePhase.ACTIVE,
                currentCycleNumber = cycleIndex + 1,
                dayInCycle = dayInCycle,
                daysRemainingInPhase = daysRemaining,
                phaseStartDate = cycleStart,
                phaseEndDate = cycleEnd,
                progressFraction = progressFraction,
                nextAction = nextAction,
                timelineBlocks = timeline,
                standardAnnualCost = standardAnnualCost,
                optimizedAnnualCost = standardAnnualCost,
                annualSavings = 0.0,
                savingsPercentage = 0.0
            )
        }

        // Rotation Mode (45-Day, 60-Day, or Custom)
        val normalizedDays = if (daysSinceStart >= 0) daysSinceStart else 0
        val cycleIndex = (normalizedDays / totalCycleDays).toInt()
        val dayInCurrentCycle = (normalizedDays % totalCycleDays).toInt() // 0 to (totalCycleDays - 1)
        val currentCycleStart = referenceStart.plusDays((cycleIndex * totalCycleDays).toLong())

        val isCurrentlyActive = dayInCurrentCycle < activeDays

        val phase: CyclePhase
        val dayInCycleDisplay = dayInCurrentCycle + 1
        val daysRemaining: Int
        val phaseStartDate: LocalDate
        val phaseEndDate: LocalDate
        val progressFraction: Float
        val nextAction: NextAction

        if (isCurrentlyActive) {
            phase = CyclePhase.ACTIVE
            phaseStartDate = currentCycleStart
            phaseEndDate = currentCycleStart.plusDays(activeDays.toLong())
            daysRemaining = activeDays - dayInCurrentCycle
            progressFraction = (dayInCurrentCycle + 1).toFloat() / activeDays

            nextAction = when {
                subscription.isAutoRenewCancelled -> NextAction(
                    title = "Auto-Renew Cancelled",
                    description = "Active access active until $phaseEndDate. Blackout period begins on $phaseEndDate.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.INFO,
                    actionType = ActionType.NO_ACTION
                )
                daysRemaining <= 3 -> NextAction(
                    title = "⚡ Action Required: Cancel Auto-Renew!",
                    description = "Cancel auto-renewal before $phaseEndDate to avoid unwanted billing for next month.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.ACTION_DUE,
                    actionType = ActionType.CANCEL_AUTO_RENEW
                )
                daysRemaining <= 7 -> NextAction(
                    title = "Prepare to Cancel",
                    description = "$daysRemaining days left of active access. Remember to turn off auto-renewal before $phaseEndDate.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.WARNING,
                    actionType = ActionType.CANCEL_AUTO_RENEW
                )
                else -> NextAction(
                    title = "Active Window",
                    description = "Enjoy your content! Active access valid until $phaseEndDate ($daysRemaining days remaining).",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.INFO,
                    actionType = ActionType.CANCEL_AUTO_RENEW
                )
            }
        } else {
            phase = CyclePhase.BLACKOUT
            val blackoutDayIndex = dayInCurrentCycle - activeDays
            phaseStartDate = currentCycleStart.plusDays(activeDays.toLong())
            phaseEndDate = currentCycleStart.plusDays(totalCycleDays.toLong())
            daysRemaining = totalCycleDays - dayInCurrentCycle
            progressFraction = (blackoutDayIndex + 1).toFloat() / blackoutDays

            nextAction = when {
                daysRemaining <= 1 -> NextAction(
                    title = "🔄 Resubscribe Ready!",
                    description = "Blackout ends on $phaseEndDate. Re-subscribe to restart your 30-day active window!",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.ACTION_DUE,
                    actionType = ActionType.RESUBSCRIBE
                )
                daysRemaining <= 4 -> NextAction(
                    title = "Blackout Ending Soon",
                    description = "Only $daysRemaining days left in blackout. Prepare to re-subscribe on $phaseEndDate.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.WARNING,
                    actionType = ActionType.RESUBSCRIBE
                )
                else -> NextAction(
                    title = "In Blackout Window (Saving Money)",
                    description = "Paused. Re-subscription opens on $phaseEndDate ($daysRemaining days left).",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.INFO,
                    actionType = ActionType.RESUBSCRIBE
                )
            }
        }

        val timeline = generateTimeline(
            startDate = currentCycleStart,
            activeDays = activeDays,
            blackoutDays = blackoutDays,
            cyclesToGenerate = 5,
            today = today
        )

        // Math: Number of billings per 365 days = (365.0 / totalCycleDays)
        val billingsPerYear = 365.0 / totalCycleDays
        val optimizedAnnualCost = billingsPerYear * subscription.cost
        val annualSavings = max(0.0, standardAnnualCost - optimizedAnnualCost)
        val savingsPercentage = if (standardAnnualCost > 0) (annualSavings / standardAnnualCost) * 100.0 else 0.0

        return CycleStatus(
            subscription = subscription,
            phase = phase,
            currentCycleNumber = cycleIndex + 1,
            dayInCycle = dayInCycleDisplay,
            daysRemainingInPhase = daysRemaining,
            phaseStartDate = phaseStartDate,
            phaseEndDate = phaseEndDate,
            progressFraction = progressFraction.coerceIn(0f, 1f),
            nextAction = nextAction,
            timelineBlocks = timeline,
            standardAnnualCost = standardAnnualCost,
            optimizedAnnualCost = optimizedAnnualCost,
            annualSavings = annualSavings,
            savingsPercentage = savingsPercentage
        )
    }

    private fun generateTimeline(
        startDate: LocalDate,
        activeDays: Int,
        blackoutDays: Int,
        cyclesToGenerate: Int,
        today: LocalDate
    ): List<TimelineBlock> {
        val blocks = mutableListOf<TimelineBlock>()
        var cursor = startDate

        for (i in 0 until cyclesToGenerate) {
            // Active block
            val activeEnd = cursor.plusDays(activeDays.toLong())
            val isCurrentActive = !today.isBefore(cursor) && today.isBefore(activeEnd)
            blocks.add(
                TimelineBlock(
                    startDate = cursor,
                    endDate = activeEnd,
                    phase = CyclePhase.ACTIVE,
                    isCurrent = isCurrentActive
                )
            )
            cursor = activeEnd

            // Blackout block (if any)
            if (blackoutDays > 0) {
                val blackoutEnd = cursor.plusDays(blackoutDays.toLong())
                val isCurrentBlackout = !today.isBefore(cursor) && today.isBefore(blackoutEnd)
                blocks.add(
                    TimelineBlock(
                        startDate = cursor,
                        endDate = blackoutEnd,
                        phase = CyclePhase.BLACKOUT,
                        isCurrent = isCurrentBlackout
                    )
                )
                cursor = blackoutEnd
            }
        }
        return blocks
    }

    /**
     * Compute aggregated portfolio metrics across all subscriptions
     */
    fun calculatePortfolioSavings(statuses: List<CycleStatus>): PortfolioSummary {
        val totalStandardAnnual = statuses.sumOf { it.standardAnnualCost }
        val totalOptimizedAnnual = statuses.sumOf { it.optimizedAnnualCost }
        val totalAnnualSavings = statuses.sumOf { it.annualSavings }
        val overallSavingsPct = if (totalStandardAnnual > 0) (totalAnnualSavings / totalStandardAnnual) * 100.0 else 0.0

        val totalMonthlyStandard = totalStandardAnnual / 12.0
        val totalMonthlyOptimized = totalOptimizedAnnual / 12.0
        val totalMonthlySavings = totalAnnualSavings / 12.0

        val activeCount = statuses.count { it.phase == CyclePhase.ACTIVE }
        val blackoutCount = statuses.count { it.phase == CyclePhase.BLACKOUT }
        val urgentActions = statuses.map { it.nextAction }.filter { it.urgency == ActionUrgency.ACTION_DUE }

        return PortfolioSummary(
            totalSubscriptions = statuses.size,
            activeCount = activeCount,
            blackoutCount = blackoutCount,
            totalStandardAnnualCost = totalStandardAnnual,
            totalOptimizedAnnualCost = totalOptimizedAnnual,
            totalAnnualSavings = totalAnnualSavings,
            totalMonthlyStandardCost = totalMonthlyStandard,
            totalMonthlyOptimizedCost = totalMonthlyOptimized,
            totalMonthlySavings = totalMonthlySavings,
            overallSavingsPercentage = overallSavingsPct,
            urgentActionsCount = urgentActions.size
        )
    }
}

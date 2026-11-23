package com.subtrack.core.domain.engine

import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

object RotationEngine {

    /** Exact annual savings ratio for a 45-day cycle (30d on / 15d off): ~32.41%. */
    const val ROTATION_45_SAVINGS_RATIO: Double = 1.0 - (365.0 / 45.0) / 12.0

    /** Exact annual savings ratio for a 60-day bi-monthly cycle (30d on / 30d off): ~49.31%. */
    const val ROTATION_60_SAVINGS_RATIO: Double = 1.0 - (365.0 / 60.0) / 12.0

    private val readableDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun formatReadableDate(date: LocalDate): String = date.format(readableDateFormatter)

    fun cycleIndexFor(subscription: Subscription, today: LocalDate = LocalDate.now()): Int {
        val daysSinceStart = ChronoUnit.DAYS.between(subscription.startDate, today)
        val normalizedDays = if (daysSinceStart >= 0) daysSinceStart else 0L
        return when {
            subscription.billingPeriod == BillingPeriod.YEARLY -> (normalizedDays / 365).toInt()
            subscription.isRotating -> {
                val baseCycleDays = subscription.totalCycleDays.coerceAtLeast(1)
                val firstCycleTotal = baseCycleDays + subscription.bonusActiveDays.coerceAtLeast(0)
                if (normalizedDays < firstCycleTotal) {
                    0
                } else {
                    1 + ((normalizedDays - firstCycleTotal) / baseCycleDays).toInt()
                }
            }
            else -> {
                val baseCycleDays = subscription.activeDays.coerceAtLeast(1)
                (normalizedDays / baseCycleDays).toInt()
            }
        }
    }

    fun isAutoRenewCancelledForCycle(subscription: Subscription, cycleIndex: Int): Boolean {
        if (!subscription.isAutoRenewCancelled) return false
        val recordedIndex = subscription.cancelledCycleIndex
        return recordedIndex == null || recordedIndex == cycleIndex
    }

    fun perBreakSavings(subscription: Subscription): Double {
        if (!subscription.isRotating) return 0.0
        val totalCycleDays = subscription.totalCycleDays.coerceAtLeast(1)
        val cyclesPerYear = 365.0 / totalCycleDays
        val annualSavings = max(0.0, subscription.annualStandardCost - (cyclesPerYear * subscription.cost))
        return if (cyclesPerYear > 0.0) annualSavings / cyclesPerYear else 0.0
    }

    fun calculateCycleStatus(
        subscription: Subscription,
        today: LocalDate = LocalDate.now()
    ): CycleStatus {
        val totalCycleDays = subscription.totalCycleDays.coerceAtLeast(1)
        val activeDays = subscription.activeDays.coerceAtLeast(1)
        val blackoutDays = subscription.blackoutDays.coerceAtLeast(0)

        val referenceStart = subscription.startDate
        val daysSinceStart = ChronoUnit.DAYS.between(referenceStart, today)

        val standardAnnualCost = subscription.annualStandardCost
        val isYearly = subscription.billingPeriod == BillingPeriod.YEARLY

        if (isYearly || blackoutDays <= 0 || subscription.strategy == RotationStrategy.CONTINUOUS) {
            val cycleDays = when {
                isYearly -> 365
                activeDays > 0 -> activeDays
                else -> 30
            }
            val normalizedDays = if (daysSinceStart >= 0) daysSinceStart else 0L
            val cycleIndex = (normalizedDays / cycleDays).toInt()
            val dayInCycleIndex = (normalizedDays % cycleDays).toInt()
            val dayInCycle = dayInCycleIndex + 1
            val daysRemaining = cycleDays - dayInCycleIndex
            val cycleStart = referenceStart.plusDays(cycleIndex.toLong() * cycleDays)
            val cycleEnd = cycleStart.plusDays(cycleDays.toLong())
            val progressFraction = (dayInCycle.toFloat() / cycleDays).coerceIn(0f, 1f)
            val formattedEnd = formatReadableDate(cycleEnd)

            val nextAction = NextAction(
                title = if (isYearly) "Annual renewal" else "Monthly renewal",
                description = "Renews in $daysRemaining day${if (daysRemaining == 1) "" else "s"} on $formattedEnd (${MoneyFormatter.format(subscription.cost, subscription.currency)}${subscription.billingPeriod.shortSuffix}).",
                targetDate = cycleEnd,
                urgency = if (isYearly && daysRemaining <= 14) ActionUrgency.WARNING else ActionUrgency.INFO,
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
                savingsPercentage = 0.0,
                isAutoRenewCancelledForCurrentCycle = false,
                perBreakSavings = 0.0
            )
        }

        // Rotation Mode (45-Day, 60-Day, or Custom) with optional one-cycle bonusActiveDays extension
        val normalizedDays = if (daysSinceStart >= 0) daysSinceStart else 0L
        val bonusDays = subscription.bonusActiveDays.coerceAtLeast(0)
        val firstCycleActiveDays = activeDays + bonusDays
        val firstCycleTotalDays = firstCycleActiveDays + blackoutDays

        val cycleIndex: Int
        val dayInCurrentCycle: Int
        val currentCycleStart: LocalDate
        val effectiveActiveDays: Int
        val effectiveTotalCycleDays: Int

        if (normalizedDays < firstCycleTotalDays) {
            cycleIndex = 0
            dayInCurrentCycle = normalizedDays.toInt()
            currentCycleStart = referenceStart
            effectiveActiveDays = firstCycleActiveDays
            effectiveTotalCycleDays = firstCycleTotalDays
        } else {
            val daysAfterFirst = normalizedDays - firstCycleTotalDays
            cycleIndex = 1 + (daysAfterFirst / totalCycleDays).toInt()
            dayInCurrentCycle = (daysAfterFirst % totalCycleDays).toInt()
            currentCycleStart = referenceStart.plusDays(
                firstCycleTotalDays.toLong() + (cycleIndex - 1).toLong() * totalCycleDays
            )
            effectiveActiveDays = activeDays
            effectiveTotalCycleDays = totalCycleDays
        }

        val cancelledForCycle = isAutoRenewCancelledForCycle(subscription, cycleIndex)
        val hasUnbankedCompletedBreak = subscription.isRotating &&
            subscription.isAutoRenewCancelled &&
            subscription.cancelledCycleIndex != null &&
            cycleIndex > subscription.cancelledCycleIndex

        val isCurrentlyActive = dayInCurrentCycle < effectiveActiveDays

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
            phaseEndDate = currentCycleStart.plusDays(effectiveActiveDays.toLong())
            daysRemaining = effectiveActiveDays - dayInCurrentCycle
            progressFraction = (dayInCurrentCycle + 1).toFloat() / effectiveActiveDays
            val formattedEnd = formatReadableDate(phaseEndDate)

            nextAction = when {
                cancelledForCycle -> NextAction(
                    title = "Auto-renew cancelled",
                    description = "Active access continues until $formattedEnd, then your ${blackoutDays}-day pause begins.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.INFO,
                    actionType = ActionType.NO_ACTION
                )
                daysRemaining <= 3 -> NextAction(
                    title = "Cancel auto-renew",
                    description = "Cancel auto-renew before $formattedEnd to avoid next month's charge.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.ACTION_DUE,
                    actionType = ActionType.CANCEL_AUTO_RENEW
                )
                daysRemaining <= 7 -> NextAction(
                    title = "Prepare to cancel",
                    description = "$daysRemaining days left in this active window. Turn off auto-renew before $formattedEnd.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.WARNING,
                    actionType = ActionType.CANCEL_AUTO_RENEW
                )
                else -> NextAction(
                    title = "Active window",
                    description = "Enjoy your access until $formattedEnd ($daysRemaining days remaining).",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.INFO,
                    actionType = ActionType.CANCEL_AUTO_RENEW
                )
            }
        } else {
            phase = CyclePhase.BLACKOUT
            val blackoutDayIndex = dayInCurrentCycle - effectiveActiveDays
            phaseStartDate = currentCycleStart.plusDays(effectiveActiveDays.toLong())
            phaseEndDate = currentCycleStart.plusDays(effectiveTotalCycleDays.toLong())
            daysRemaining = effectiveTotalCycleDays - dayInCurrentCycle
            progressFraction = (blackoutDayIndex + 1).toFloat() / blackoutDays
            val formattedEnd = formatReadableDate(phaseEndDate)

            nextAction = when {
                daysRemaining <= 1 -> NextAction(
                    title = "Ready to re-subscribe",
                    description = "Pause ends on $formattedEnd. Re-subscribe to start your ${activeDays}-day active window.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.ACTION_DUE,
                    actionType = ActionType.RESUBSCRIBE
                )
                daysRemaining <= 4 -> NextAction(
                    title = "Pause ending soon",
                    description = "$daysRemaining days left in your break. Re-subscribe opens on $formattedEnd.",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.WARNING,
                    actionType = ActionType.RESUBSCRIBE
                )
                else -> NextAction(
                    title = "Paused · saving money",
                    description = "On break until $formattedEnd ($daysRemaining days left).",
                    targetDate = phaseEndDate,
                    urgency = ActionUrgency.INFO,
                    actionType = ActionType.RESUBSCRIBE
                )
            }
        }

        val timeline = generateTimeline(
            startDate = currentCycleStart,
            activeDays = activeDays,
            firstCycleActiveDays = effectiveActiveDays,
            blackoutDays = blackoutDays,
            cyclesToGenerate = 5,
            today = today
        )

        val billingsPerYear = 365.0 / totalCycleDays
        val optimizedAnnualCost = billingsPerYear * subscription.cost
        val annualSavings = max(0.0, standardAnnualCost - optimizedAnnualCost)
        val savingsPercentage = if (standardAnnualCost > 0) (annualSavings / standardAnnualCost) * 100.0 else 0.0
        val breakSavings = if (billingsPerYear > 0.0) annualSavings / billingsPerYear else 0.0

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
            savingsPercentage = savingsPercentage,
            isAutoRenewCancelledForCurrentCycle = cancelledForCycle,
            perBreakSavings = breakSavings,
            hasUnbankedCompletedBreak = hasUnbankedCompletedBreak
        )
    }

    private fun generateTimeline(
        startDate: LocalDate,
        activeDays: Int,
        blackoutDays: Int,
        cyclesToGenerate: Int,
        today: LocalDate,
        firstCycleActiveDays: Int = activeDays
    ): List<TimelineBlock> {
        val blocks = mutableListOf<TimelineBlock>()
        var cursor = startDate

        for (i in 0 until cyclesToGenerate) {
            val cycleActiveDays = if (i == 0) firstCycleActiveDays else activeDays
            val activeEnd = cursor.plusDays(cycleActiveDays.toLong())
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

package com.subtrack.core.domain.engine

import com.subtrack.core.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RotationEngineTest {

    @Test
    fun test45DayRotation_ActivePhase_Day10() {
        val today = LocalDate.of(2026, 8, 18)
        val startDate = today.minusDays(10) // 10 days since start

        val subscription = Subscription(
            name = "Netflix",
            cost = 15.49,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 15
        )

        val status = RotationEngine.calculateCycleStatus(subscription, today)

        assertEquals(CyclePhase.ACTIVE, status.phase)
        assertEquals(11, status.dayInCycle)
        assertEquals(20, status.daysRemainingInPhase)
        assertTrue(status.annualSavings > 55.0)
        assertTrue(status.savingsPercentage > 30.0)
    }

    @Test
    fun test45DayRotation_UrgentCancel_Day28() {
        val today = LocalDate.of(2026, 8, 18)
        val startDate = today.minusDays(28) // 28 days since start (2 days left to cancel)

        val subscription = Subscription(
            name = "Netflix",
            cost = 15.49,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS
        )

        val status = RotationEngine.calculateCycleStatus(subscription, today)

        assertEquals(CyclePhase.ACTIVE, status.phase)
        assertEquals(2, status.daysRemainingInPhase)
        assertEquals(ActionUrgency.ACTION_DUE, status.nextAction.urgency)
        assertEquals(ActionType.CANCEL_AUTO_RENEW, status.nextAction.actionType)
    }

    @Test
    fun test45DayRotation_BlackoutPhase_Day35() {
        val today = LocalDate.of(2026, 8, 18)
        val startDate = today.minusDays(35) // 35 days since start (5 days into blackout)

        val subscription = Subscription(
            name = "Disney+",
            cost = 13.99,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS
        )

        val status = RotationEngine.calculateCycleStatus(subscription, today)

        assertEquals(CyclePhase.BLACKOUT, status.phase)
        assertEquals(10, status.daysRemainingInPhase) // 45 - 35 = 10 days left
        assertEquals(ActionType.RESUBSCRIBE, status.nextAction.actionType)
    }

    @Test
    fun test45DayRotation_ResubscribeReady_Day44() {
        val today = LocalDate.of(2026, 8, 18)
        val startDate = today.minusDays(44) // 44 days since start (1 day left of blackout)

        val subscription = Subscription(
            name = "Max",
            cost = 16.99,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS
        )

        val status = RotationEngine.calculateCycleStatus(subscription, today)

        assertEquals(CyclePhase.BLACKOUT, status.phase)
        assertEquals(1, status.daysRemainingInPhase)
        assertEquals(ActionUrgency.ACTION_DUE, status.nextAction.urgency)
        assertEquals(ActionType.RESUBSCRIBE, status.nextAction.actionType)
    }

    @Test
    fun testContinuousSubscription_ZeroSavings() {
        val today = LocalDate.of(2026, 8, 18)
        val startDate = today.minusDays(12)

        val subscription = Subscription(
            name = "Spotify",
            cost = 11.99,
            startDate = startDate,
            strategy = RotationStrategy.CONTINUOUS
        )

        val status = RotationEngine.calculateCycleStatus(subscription, today)

        assertEquals(CyclePhase.ACTIVE, status.phase)
        assertEquals(0.0, status.annualSavings, 0.001)
        assertEquals(0.0, status.savingsPercentage, 0.001)
    }

    @Test
    fun testPortfolioSavingsAggregation() {
        val today = LocalDate.of(2026, 8, 18)
        val subs = listOf(
            Subscription(name = "Netflix", cost = 15.0, startDate = today, strategy = RotationStrategy.ROTATION_45_DAYS),
            Subscription(name = "Disney+", cost = 15.0, startDate = today.minusDays(35), strategy = RotationStrategy.ROTATION_45_DAYS),
            Subscription(name = "Spotify", cost = 10.0, startDate = today, strategy = RotationStrategy.CONTINUOUS)
        )

        val statuses = subs.map { RotationEngine.calculateCycleStatus(it, today) }
        val summary = RotationEngine.calculatePortfolioSavings(statuses)

        assertEquals(3, summary.totalSubscriptions)
        assertEquals(2, summary.activeCount)
        assertEquals(1, summary.blackoutCount)
        assertTrue(summary.totalAnnualSavings > 0.0)
    }
}

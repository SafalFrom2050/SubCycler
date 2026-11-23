package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ManageSubscriptionUseCaseTest {

    @Test
    fun addSubscriptionsIfMissingMakesRetriesIdempotent() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val subscriptions = listOf(
            Subscription(
                name = "Netflix",
                cost = 15.49,
                startDate = LocalDate.of(2026, 8, 30)
            ),
            Subscription(
                name = "Spotify",
                cost = 11.99,
                startDate = LocalDate.of(2026, 8, 30)
            )
        )

        useCase.addSubscriptionsIfMissing(subscriptions)
        useCase.addSubscriptionsIfMissing(
            subscriptions.map { it.copy(name = "  ${it.name.uppercase()}  ") }
        )

        assertEquals(2, repository.getAllSubscriptionsList().size)
    }

    @Test
    fun completeUnsubscribeMarksAutoRenewCancelledWithoutChangingStartDate() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val initialStart = LocalDate.of(2026, 8, 1)
        val id = useCase.addSubscription(
            Subscription(
                name = "Netflix",
                cost = 15.49,
                startDate = initialStart,
                isAutoRenewCancelled = false
            )
        )

        useCase.completeSubscriptionAction(
            subscriptionId = id,
            actionType = SubscriptionActionProgressType.UNSUBSCRIBE,
            today = LocalDate.of(2026, 8, 18)
        )

        val updated = repository.getSubscriptionById(id)!!
        assertTrue(updated.isAutoRenewCancelled)
        assertEquals(initialStart, updated.startDate)
    }

    @Test
    fun completeSubscribeStartsNewCycleTodayAndResetsAutoRenewCancelled() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val today = LocalDate.of(2026, 8, 18)
        val id = useCase.addSubscription(
            Subscription(
                name = "Disney+",
                cost = 13.99,
                startDate = LocalDate.of(2026, 7, 1),
                isAutoRenewCancelled = true
            )
        )

        useCase.completeSubscriptionAction(
            subscriptionId = id,
            actionType = SubscriptionActionProgressType.SUBSCRIBE,
            today = today
        )

        val updated = repository.getSubscriptionById(id)!!
        assertFalse(updated.isAutoRenewCancelled)
        assertEquals(today, updated.startDate)
        // Resuming from blackout records completed break in Earned Savings Ledger!
        assertEquals(1, updated.completedBreaksCount)
        assertTrue(updated.earnedSavingsTotal > 0.0)
    }

    @Test
    fun extendPauseAndSwitchToSmartRotationUpdateCycleCorrectly() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val today = LocalDate.of(2026, 8, 18)
        val id = useCase.addSubscription(
            Subscription(
                name = "Spotify",
                cost = 11.99,
                startDate = today.minusDays(20),
                strategy = com.subtrack.core.domain.model.RotationStrategy.CONTINUOUS
            )
        )

        useCase.switchToSmartRotation(id)
        val rotated = repository.getSubscriptionById(id)!!
        assertEquals(com.subtrack.core.domain.model.RotationStrategy.ROTATION_45_DAYS, rotated.strategy)

        useCase.startPauseToday(id, today)
        val paused = repository.getSubscriptionById(id)!!
        assertEquals(today.minusDays(30), paused.startDate)
        assertTrue(paused.isAutoRenewCancelled)

        useCase.extendActiveCycle(id, today)
        val extended = repository.getSubscriptionById(id)!!
        assertEquals(today, extended.startDate)
        assertFalse(extended.isAutoRenewCancelled)
    }

    @Test
    fun extendActiveCycleGenuinelyAddsFullActiveWindowOnDay2() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val start = LocalDate.of(2026, 11, 21)
        val day2 = LocalDate.of(2026, 11, 22)
        val id = useCase.addSubscription(
            Subscription(
                name = "Netflix",
                cost = 17.99,
                startDate = start
            )
        )

        val before = com.subtrack.core.domain.engine.RotationEngine.calculateCycleStatus(
            repository.getSubscriptionById(id)!!,
            day2
        )
        assertEquals(29, before.daysRemainingInPhase)

        useCase.extendActiveCycle(id, day2)
        val after = com.subtrack.core.domain.engine.RotationEngine.calculateCycleStatus(
            repository.getSubscriptionById(id)!!,
            day2
        )
        assertEquals(59, after.daysRemainingInPhase)
        assertEquals(before.phaseEndDate.plusDays(30), after.phaseEndDate)
    }

    @Test
    fun sameDayPauseAndResumeDoesNotBankFakeSavings() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val today = LocalDate.of(2026, 11, 22)
        val id = useCase.addSubscription(
            Subscription(
                name = "Netflix",
                cost = 17.99,
                startDate = today.minusDays(5)
            )
        )

        useCase.startPauseToday(id, today)
        useCase.completeSubscriptionAction(id, SubscriptionActionProgressType.SUBSCRIBE, today)

        val updated = repository.getSubscriptionById(id)!!
        assertEquals(0, updated.completedBreaksCount)
        assertEquals(0.0, updated.earnedSavingsTotal, 0.001)
    }

    @Test
    fun naturalCycleRolloverExposesUnbankedBreakAndCreditsOnce() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val start = LocalDate.of(2026, 7, 1)
        val id = useCase.addSubscription(
            Subscription(
                name = "Max",
                cost = 16.99,
                startDate = start,
                isAutoRenewCancelled = true,
                cancelledCycleIndex = 0
            )
        )

        val cycle2Day1 = start.plusDays(46)
        val status = com.subtrack.core.domain.engine.RotationEngine.calculateCycleStatus(
            repository.getSubscriptionById(id)!!,
            cycle2Day1
        )
        assertTrue(status.hasUnbankedCompletedBreak)

        useCase.completeSubscriptionAction(id, SubscriptionActionProgressType.SUBSCRIBE, cycle2Day1)
        val afterCredit = repository.getSubscriptionById(id)!!
        assertEquals(1, afterCredit.completedBreaksCount)
        assertTrue(afterCredit.earnedSavingsTotal > 0.0)
        assertFalse(
            com.subtrack.core.domain.engine.RotationEngine.calculateCycleStatus(afterCredit, cycle2Day1)
                .hasUnbankedCompletedBreak
        )
    }

    @Test
    fun updateAllSubscriptionsCurrencyUnifiesCurrencyWithoutConvertingAmounts() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ManageSubscriptionUseCase(repository)
        val today = LocalDate.of(2026, 8, 18)
        useCase.addSubscriptions(
            listOf(
                Subscription(name = "Netflix", cost = 17.99, currency = "USD", startDate = today),
                Subscription(name = "Disney+", cost = 1140.0, currency = "JPY", startDate = today)
            )
        )

        useCase.updateAllSubscriptionsCurrency("EUR")

        val all = repository.getAllSubscriptionsList().sortedBy { it.name }
        assertEquals(2, all.size)
        assertTrue(all.all { it.currency == "EUR" })
        assertEquals(1140.0, all.first { it.name == "Disney+" }.cost, 0.001)
        assertEquals(17.99, all.first { it.name == "Netflix" }.cost, 0.001)
    }
}

package com.subtrack.app.worker

import com.subtrack.core.common.time.FakeTimeProvider
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.UserPreferences
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import com.subtrack.core.domain.testing.FakeUserPreferencesRepository
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyCycleEvaluationLogicTest {

    private val fakeRepository = FakeSubscriptionRepository()
    private val fakeTimeProvider = FakeTimeProvider(currentDate = LocalDate.of(2026, 8, 18))
    private val fakeUserPreferencesRepository = FakeUserPreferencesRepository(
        UserPreferences(notificationLeadDays = 3)
    )
    private val getCycleStatusesUseCase = GetSubscriptionCycleStatusesUseCase(fakeRepository, fakeTimeProvider)

    @Test
    fun testEvaluationIdentifiesSubscriptionNeedingCancelAlert() = runTest {
        val today = LocalDate.of(2026, 8, 18)
        // Started 28 days ago with 30d active window -> 2 days remaining (< 3 days lead time)
        val sub = Subscription(
            id = 1,
            name = "Netflix",
            cost = 15.49,
            startDate = today.minusDays(28),
            strategy = RotationStrategy.ROTATION_45_DAYS,
            isAutoRenewCancelled = false
        )
        fakeRepository.resetAllWithSamples(listOf(sub))

        val prefs = fakeUserPreferencesRepository.userPreferences.first()
        val statuses = getCycleStatusesUseCase().first()

        assertEquals(1, statuses.size)
        val status = statuses.first()

        assertEquals(CyclePhase.ACTIVE, status.phase)
        assertEquals(2, status.daysRemainingInPhase)
        assertTrue("Days remaining should trigger cancellation alert", status.daysRemainingInPhase <= prefs.notificationLeadDays)
        assertTrue("Sub is not cancelled yet", !status.subscription.isAutoRenewCancelled)
    }

    @Test
    fun testEvaluationIgnoresAlreadyCancelledSubscription() = runTest {
        val today = LocalDate.of(2026, 8, 18)
        val sub = Subscription(
            id = 2,
            name = "Disney+",
            cost = 13.99,
            startDate = today.minusDays(28),
            strategy = RotationStrategy.ROTATION_45_DAYS,
            isAutoRenewCancelled = true
        )
        fakeRepository.resetAllWithSamples(listOf(sub))

        val statuses = getCycleStatusesUseCase().first()
        val status = statuses.first()

        assertEquals(true, status.subscription.isAutoRenewCancelled)
    }

    @Test
    fun testEvaluationIdentifiesResumingBlackout() = runTest {
        val today = LocalDate.of(2026, 8, 18)
        // 44 days elapsed in 45-day cycle -> 1 day remaining in blackout
        val sub = Subscription(
            id = 3,
            name = "HBO Max",
            cost = 16.99,
            startDate = today.minusDays(44),
            strategy = RotationStrategy.ROTATION_45_DAYS
        )
        fakeRepository.resetAllWithSamples(listOf(sub))

        val statuses = getCycleStatusesUseCase().first()
        val status = statuses.first()

        assertEquals(CyclePhase.BLACKOUT, status.phase)
        assertEquals(1, status.daysRemainingInPhase)
    }
}

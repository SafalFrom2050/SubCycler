package com.subtrack.app.domain.usecase

import com.subtrack.app.data.model.CyclePhase
import com.subtrack.app.data.model.RotationStrategy
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.repository.FakeSubscriptionRepository
import com.subtrack.app.domain.time.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetSubscriptionCycleStatusesUseCaseTest {

    private val fakeRepository = FakeSubscriptionRepository()
    private val fakeTimeProvider = FakeTimeProvider(currentDate = LocalDate.of(2026, 8, 18))
    private val useCase = GetSubscriptionCycleStatusesUseCase(fakeRepository, fakeTimeProvider)

    @Test
    fun testCalculatesCycleStatusesFromRepositoryFlow() = runTest {
        val today = LocalDate.of(2026, 8, 18)
        fakeRepository.setSubscriptions(
            listOf(
                Subscription(
                    id = 1,
                    name = "Netflix",
                    cost = 15.49,
                    startDate = today.minusDays(10),
                    strategy = RotationStrategy.ROTATION_45_DAYS
                ),
                Subscription(
                    id = 2,
                    name = "Disney+",
                    cost = 13.99,
                    startDate = today.minusDays(35),
                    strategy = RotationStrategy.ROTATION_45_DAYS
                )
            )
        )

        val statuses = useCase().first()

        assertEquals(2, statuses.size)
        val netflix = statuses.find { it.subscription.name == "Netflix" }!!
        val disney = statuses.find { it.subscription.name == "Disney+" }!!

        assertEquals(CyclePhase.ACTIVE, netflix.phase)
        assertEquals(20, netflix.daysRemainingInPhase)
        assertTrue(netflix.annualSavings > 0)

        assertEquals(CyclePhase.BLACKOUT, disney.phase)
        assertEquals(10, disney.daysRemainingInPhase)
    }
}

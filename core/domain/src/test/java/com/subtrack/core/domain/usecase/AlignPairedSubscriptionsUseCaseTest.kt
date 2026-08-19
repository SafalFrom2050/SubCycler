package com.subtrack.core.domain.usecase

import com.subtrack.core.common.time.FakeTimeProvider
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AlignPairedSubscriptionsUseCaseTest {

    private val fakeRepository = FakeSubscriptionRepository()
    private val fakeTimeProvider = FakeTimeProvider(currentDate = LocalDate.of(2026, 8, 18))
    private val useCase = AlignPairedSubscriptionsUseCase(fakeRepository, fakeTimeProvider)

    @Test
    fun testAlignsSubscriptionsAlternatingCorrectly() = runTest {
        val today = LocalDate.of(2026, 8, 18)
        val s1 = Subscription(id = 1, name = "Netflix", cost = 15.49, startDate = today.minusDays(5))
        val s2 = Subscription(id = 2, name = "Disney+", cost = 13.99, startDate = today.minusDays(15))

        fakeRepository.resetAllWithSamples(listOf(s1, s2))

        useCase(service1Id = 1L, service2Id = 2L)

        val updatedS1 = fakeRepository.getSubscriptionById(1L)!!
        val updatedS2 = fakeRepository.getSubscriptionById(2L)!!

        assertEquals(today, updatedS1.startDate)
        assertEquals(RotationStrategy.ROTATION_60_DAYS, updatedS1.strategy)
        assertEquals(30, updatedS1.customActiveDays)
        assertEquals(30, updatedS1.customBlackoutDays)
        assertEquals(2L, updatedS1.pairedWithSubscriptionId)

        assertEquals(today.minusDays(30), updatedS2.startDate)
        assertEquals(RotationStrategy.ROTATION_60_DAYS, updatedS2.strategy)
        assertEquals(30, updatedS2.customActiveDays)
        assertEquals(30, updatedS2.customBlackoutDays)
        assertEquals(1L, updatedS2.pairedWithSubscriptionId)
    }
}

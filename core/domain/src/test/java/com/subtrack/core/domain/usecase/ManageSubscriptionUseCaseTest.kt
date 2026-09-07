package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}

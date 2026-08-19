package com.subtrack.app.domain.usecase

import com.subtrack.app.data.model.RotationStrategy
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.domain.time.DefaultTimeProvider
import com.subtrack.app.domain.time.TimeProvider

/**
 * Use case to synchronize two subscriptions into alternating 30d/30d continuous entertainment.
 */
class AlignPairedSubscriptionsUseCase(
    private val repository: SubscriptionRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    suspend operator fun invoke(service1Id: Long, service2Id: Long) {
        val s1 = repository.getSubscriptionById(service1Id) ?: return
        val s2 = repository.getSubscriptionById(service2Id) ?: return
        val today = timeProvider.today()

        // S1 active now (30d active + 30d blackout)
        val updatedS1 = s1.copy(
            startDate = today,
            strategy = RotationStrategy.ROTATION_60_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 30,
            pairedWithSubscriptionId = s2.id
        )

        // S2 blackout now, activates in 30 days
        val updatedS2 = s2.copy(
            startDate = today.minusDays(30),
            strategy = RotationStrategy.ROTATION_60_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 30,
            pairedWithSubscriptionId = s1.id
        )

        repository.updateSubscription(updatedS1)
        repository.updateSubscription(updatedS2)
    }
}

package com.subtrack.app.domain.usecase

import com.subtrack.app.data.model.CycleStatus
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.domain.RotationEngine
import com.subtrack.app.domain.time.DefaultTimeProvider
import com.subtrack.app.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case to observe all subscriptions calculated as real-time cycle statuses.
 */
class GetSubscriptionCycleStatusesUseCase(
    private val repository: SubscriptionRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    operator fun invoke(): Flow<List<CycleStatus>> {
        return repository.getAllSubscriptions().map { subs ->
            val today = timeProvider.today()
            subs.map { RotationEngine.calculateCycleStatus(it, today) }
        }
    }
}

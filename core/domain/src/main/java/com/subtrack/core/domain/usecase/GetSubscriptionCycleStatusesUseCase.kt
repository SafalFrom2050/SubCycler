package com.subtrack.core.domain.usecase

import com.subtrack.core.common.time.DefaultTimeProvider
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.repository.SubscriptionRepository
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

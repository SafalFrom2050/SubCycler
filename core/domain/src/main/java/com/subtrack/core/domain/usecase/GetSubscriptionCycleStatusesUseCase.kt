package com.subtrack.core.domain.usecase

import com.subtrack.core.common.time.DefaultTimeProvider
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Use case to observe all subscriptions calculated as real-time cycle statuses.
 * Supports explicit date refreshes (e.g. on ON_RESUME after midnight) so countdowns never go stale.
 */
class GetSubscriptionCycleStatusesUseCase(
    private val repository: SubscriptionRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    private val dateTick = MutableStateFlow(0L)

    fun refreshToday() {
        dateTick.value += 1L
    }

    operator fun invoke(): Flow<List<CycleStatus>> {
        return combine(repository.getAllSubscriptions(), dateTick) { subs, _ ->
            val today = timeProvider.today()
            subs.map { RotationEngine.calculateCycleStatus(it, today) }
        }
    }
}

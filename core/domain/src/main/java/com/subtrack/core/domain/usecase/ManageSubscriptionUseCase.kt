package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.SubscriptionRepository

/**
 * Use case to manage subscription lifecycle (add, update, delete, toggle auto-renew).
 */
class ManageSubscriptionUseCase(
    private val repository: SubscriptionRepository
) {
    suspend fun addSubscription(subscription: Subscription): Long {
        return repository.addSubscription(subscription)
    }

    suspend fun updateSubscription(subscription: Subscription) {
        repository.updateSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        repository.deleteSubscription(subscription)
    }

    suspend fun toggleAutoRenew(subscriptionId: Long) {
        repository.toggleAutoRenewCancelled(subscriptionId)
    }
}

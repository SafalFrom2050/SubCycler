package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.SubscriptionRepository
import java.util.Locale

/**
 * Use case to manage subscription lifecycle (add, update, delete, toggle auto-renew).
 */
class ManageSubscriptionUseCase(
    private val repository: SubscriptionRepository
) {
    suspend fun addSubscription(subscription: Subscription): Long {
        return repository.addSubscription(subscription)
    }

    suspend fun addSubscriptions(subscriptions: List<Subscription>): List<Long> {
        return repository.addSubscriptions(subscriptions)
    }

    suspend fun addSubscriptionsIfMissing(subscriptions: List<Subscription>): List<Long> {
        val existingNames = repository.getAllSubscriptionsList()
            .map { it.name.trim().lowercase(Locale.ROOT) }
            .toSet()
        val missingSubscriptions = subscriptions.filter { subscription ->
            subscription.name.trim().lowercase(Locale.ROOT) !in existingNames
        }
        return if (missingSubscriptions.isEmpty()) {
            emptyList()
        } else {
            repository.addSubscriptions(missingSubscriptions)
        }
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

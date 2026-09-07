package com.subtrack.core.domain.repository

import com.subtrack.core.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getAllSubscriptions(): Flow<List<Subscription>>
    suspend fun getSubscriptionById(id: Long): Subscription?
    suspend fun getAllSubscriptionsList(): List<Subscription>
    suspend fun addSubscription(subscription: Subscription): Long
    suspend fun addSubscriptions(subscriptions: List<Subscription>): List<Long>
    suspend fun updateSubscription(subscription: Subscription)
    suspend fun deleteSubscription(subscription: Subscription)
    suspend fun deleteById(id: Long)
    suspend fun toggleAutoRenewCancelled(id: Long)
    suspend fun resetAllWithSamples(samples: List<Subscription>)
}

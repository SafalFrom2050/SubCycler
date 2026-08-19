package com.subtrack.app.data.repository

import com.subtrack.app.data.model.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    private var nextId = 1L

    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return subscriptionsFlow.asStateFlow()
    }

    override suspend fun getSubscriptionById(id: Long): Subscription? {
        return subscriptionsFlow.value.find { it.id == id }
    }

    override suspend fun getAllSubscriptionsList(): List<Subscription> {
        return subscriptionsFlow.value
    }

    override suspend fun addSubscription(subscription: Subscription): Long {
        val id = if (subscription.id == 0L) nextId++ else subscription.id
        val newSub = subscription.copy(id = id)
        subscriptionsFlow.value = subscriptionsFlow.value + newSub
        return id
    }

    override suspend fun updateSubscription(subscription: Subscription) {
        subscriptionsFlow.value = subscriptionsFlow.value.map {
            if (it.id == subscription.id) subscription else it
        }
    }

    override suspend fun deleteSubscription(subscription: Subscription) {
        subscriptionsFlow.value = subscriptionsFlow.value.filter { it.id != subscription.id }
    }

    override suspend fun deleteById(id: Long) {
        subscriptionsFlow.value = subscriptionsFlow.value.filter { it.id != id }
    }

    override suspend fun toggleAutoRenewCancelled(id: Long) {
        subscriptionsFlow.value = subscriptionsFlow.value.map {
            if (it.id == id) it.copy(isAutoRenewCancelled = !it.isAutoRenewCancelled) else it
        }
    }

    override suspend fun resetAllWithSamples(samples: List<Subscription>) {
        subscriptionsFlow.value = samples
    }

    fun setSubscriptions(list: List<Subscription>) {
        subscriptionsFlow.value = list
    }
}

package com.subtrack.core.domain.testing

import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    private var nextId: Long = 1L

    override fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionsFlow.asStateFlow()

    override suspend fun getSubscriptionById(id: Long): Subscription? {
        return subscriptionsFlow.value.find { it.id == id }
    }

    override suspend fun getAllSubscriptionsList(): List<Subscription> {
        return subscriptionsFlow.value
    }

    override suspend fun addSubscription(subscription: Subscription): Long {
        val maxExisting = subscriptionsFlow.value.maxOfOrNull { it.id } ?: 0L
        val assignedId = if (subscription.id <= 0) maxOf(nextId, maxExisting + 1L).also { nextId = it + 1L } else subscription.id
        val newSub = subscription.copy(id = assignedId)
        val currentList = subscriptionsFlow.value.toMutableList()
        currentList.removeAll { it.id == assignedId }
        currentList.add(newSub)
        subscriptionsFlow.value = currentList
        return assignedId
    }

    override suspend fun updateSubscription(subscription: Subscription) {
        val currentList = subscriptionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == subscription.id }
        if (index >= 0) {
            currentList[index] = subscription
            subscriptionsFlow.value = currentList
        }
    }

    override suspend fun deleteSubscription(subscription: Subscription) {
        deleteById(subscription.id)
    }

    override suspend fun deleteById(id: Long) {
        val currentList = subscriptionsFlow.value.toMutableList()
        currentList.removeAll { it.id == id }
        subscriptionsFlow.value = currentList
    }

    override suspend fun toggleAutoRenewCancelled(id: Long) {
        val currentList = subscriptionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(isAutoRenewCancelled = !item.isAutoRenewCancelled)
            subscriptionsFlow.value = currentList
        }
    }

    fun setSubscriptions(samples: List<Subscription>) {
        subscriptionsFlow.value = samples
    }

    override suspend fun resetAllWithSamples(samples: List<Subscription>) {
        subscriptionsFlow.value = samples
    }
}

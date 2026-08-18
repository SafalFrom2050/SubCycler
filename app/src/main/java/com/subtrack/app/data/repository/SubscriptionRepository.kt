package com.subtrack.app.data.repository

import com.subtrack.app.data.local.SubscriptionDao
import com.subtrack.app.data.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getAllSubscriptions(): Flow<List<Subscription>>
    suspend fun getSubscriptionById(id: Long): Subscription?
    suspend fun getAllSubscriptionsList(): List<Subscription>
    suspend fun addSubscription(subscription: Subscription): Long
    suspend fun updateSubscription(subscription: Subscription)
    suspend fun deleteSubscription(subscription: Subscription)
    suspend fun deleteById(id: Long)
    suspend fun toggleAutoRenewCancelled(id: Long)
    suspend fun resetAllWithSamples(samples: List<Subscription>)
}

class SubscriptionRepositoryImpl(
    private val dao: SubscriptionDao
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return dao.getAllSubscriptionsFlow()
    }

    override suspend fun getSubscriptionById(id: Long): Subscription? {
        return dao.getSubscriptionById(id)
    }

    override suspend fun getAllSubscriptionsList(): List<Subscription> {
        return dao.getAllSubscriptionsList()
    }

    override suspend fun addSubscription(subscription: Subscription): Long {
        return dao.insertSubscription(subscription)
    }

    override suspend fun updateSubscription(subscription: Subscription) {
        dao.updateSubscription(subscription)
    }

    override suspend fun deleteSubscription(subscription: Subscription) {
        dao.deleteSubscription(subscription)
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun toggleAutoRenewCancelled(id: Long) {
        val current = dao.getSubscriptionById(id) ?: return
        dao.updateSubscription(current.copy(isAutoRenewCancelled = !current.isAutoRenewCancelled))
    }

    override suspend fun resetAllWithSamples(samples: List<Subscription>) {
        dao.deleteAll()
        dao.insertAll(samples)
    }
}

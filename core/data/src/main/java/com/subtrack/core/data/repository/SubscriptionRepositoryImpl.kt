package com.subtrack.core.data.repository

import com.subtrack.core.data.local.SubscriptionDao
import com.subtrack.core.data.local.mapper.toDomain
import com.subtrack.core.data.local.mapper.toEntity
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepositoryImpl(
    private val dao: SubscriptionDao
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return dao.getAllSubscriptionsFlow().map { entities -> entities.toDomain() }
    }

    override suspend fun getSubscriptionById(id: Long): Subscription? {
        return dao.getSubscriptionById(id)?.toDomain()
    }

    override suspend fun getAllSubscriptionsList(): List<Subscription> {
        return dao.getAllSubscriptionsList().toDomain()
    }

    override suspend fun addSubscription(subscription: Subscription): Long {
        return dao.insertSubscription(subscription.toEntity())
    }

    override suspend fun updateSubscription(subscription: Subscription) {
        dao.updateSubscription(subscription.toEntity())
    }

    override suspend fun deleteSubscription(subscription: Subscription) {
        dao.deleteSubscription(subscription.toEntity())
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
        dao.insertAll(samples.toEntity())
    }
}

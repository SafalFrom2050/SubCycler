package com.subtrack.core.domain.repository

import com.subtrack.core.domain.model.PendingSubscriptionAction

interface SubscriptionActionNotifier {
    fun showActiveProgressNotification(action: PendingSubscriptionAction)
    fun dismissProgressNotification(subscriptionId: Long? = null)
}

object NoOpSubscriptionActionNotifier : SubscriptionActionNotifier {
    override fun showActiveProgressNotification(action: PendingSubscriptionAction) = Unit
    override fun dismissProgressNotification(subscriptionId: Long?) = Unit
}

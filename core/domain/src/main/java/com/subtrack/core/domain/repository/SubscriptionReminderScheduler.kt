package com.subtrack.core.domain.repository

import com.subtrack.core.domain.model.Subscription

/**
 * Platform-agnostic scheduler for local on-device subscription reminders & home-screen widget sync.
 */
interface SubscriptionReminderScheduler {
    fun scheduleForSubscription(subscription: Subscription, leadDays: Int)
    fun cancelForSubscription(subscriptionId: Long)
    fun rescheduleAll(subscriptions: List<Subscription>, leadDays: Int)
    fun notifyPortfolioChanged() {}
}

object NoOpSubscriptionReminderScheduler : SubscriptionReminderScheduler {
    override fun scheduleForSubscription(subscription: Subscription, leadDays: Int) = Unit
    override fun cancelForSubscription(subscriptionId: Long) = Unit
    override fun rescheduleAll(subscriptions: List<Subscription>, leadDays: Int) = Unit
}

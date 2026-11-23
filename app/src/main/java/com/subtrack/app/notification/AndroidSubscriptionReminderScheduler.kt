package com.subtrack.app.notification

import android.content.Context
import com.subtrack.app.widget.SubCyclerWidgetProvider
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.SubscriptionReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSubscriptionReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : SubscriptionReminderScheduler {

    override fun scheduleForSubscription(subscription: Subscription, leadDays: Int) {
        AlarmScheduler.scheduleSubscriptionAlert(context, subscription, leadDays)
    }

    override fun cancelForSubscription(subscriptionId: Long) {
        AlarmScheduler.cancelSubscriptionAlert(context, subscriptionId)
    }

    override fun rescheduleAll(subscriptions: List<Subscription>, leadDays: Int) {
        subscriptions.forEach { subscription ->
            AlarmScheduler.scheduleSubscriptionAlert(context, subscription, leadDays)
        }
    }

    override fun notifyPortfolioChanged() {
        SubCyclerWidgetProvider.requestUpdate(context)
    }
}

package com.subtrack.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.app.notification.ReminderDeduplicator

class SubscriptionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dedupKey = intent.getStringExtra(EXTRA_DEDUP_KEY)
        if (!dedupKey.isNullOrBlank() && ReminderDeduplicator.hasFired(context, dedupKey)) {
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "SubCycler Reminder"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have a subscription action due today."
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
        val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1L).takeIf { it > 0L }

        NotificationHelper.showNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message,
            subscriptionId = subscriptionId
        )
        if (!dedupKey.isNullOrBlank()) {
            ReminderDeduplicator.markFired(context, dedupKey)
        }
    }

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SUBSCRIPTION_ID = "EXTRA_SUBSCRIPTION_ID"
        const val EXTRA_DEDUP_KEY = "EXTRA_DEDUP_KEY"
    }
}

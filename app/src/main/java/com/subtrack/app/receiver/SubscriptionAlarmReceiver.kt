package com.subtrack.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subtrack.app.notification.NotificationHelper

class SubscriptionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "SubTrack Reminder"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "You have a subscription action due today."
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 1001)

        NotificationHelper.showNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message
        )
    }
}

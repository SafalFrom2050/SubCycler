package com.subtrack.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.subtrack.app.MainActivity
import com.subtrack.app.R
import com.subtrack.app.receiver.SubscriptionActionReceiver
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.SubscriptionActionProgressType

object NotificationHelper {
    const val CHANNEL_ID = "subtrack_alerts_channel"
    private const val CHANNEL_NAME = "SubCycler Rotation Alerts"
    private const val CHANNEL_DESC = "Local reminders for subscription cancellation deadlines and re-subscription dates"

    const val PROGRESS_CHANNEL_ID = "subtrack_action_progress_channel"
    private const val PROGRESS_CHANNEL_NAME = "Active Subscribe & Unsubscribe Progress"
    private const val PROGRESS_CHANNEL_DESC = "Silent active notification while you complete subscribing or unsubscribing"

    const val CALENDAR_NOTIFICATION_ID = 2_000_001
    const val ACTION_PROGRESS_NOTIFICATION_ID = 3_000_001
    const val EXTRA_OPEN_SUBSCRIPTION_ID = "com.subtrack.app.extra.OPEN_SUBSCRIPTION_ID"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(alertChannel)

            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                PROGRESS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = PROGRESS_CHANNEL_DESC
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(progressChannel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        subscriptionId: Long? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (subscriptionId != null && subscriptionId > 0L) {
                putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, subscriptionId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted on Android 13+.
        }
    }

    fun showActionProgressNotification(
        context: Context,
        action: PendingSubscriptionAction
    ) {
        val isUnsubscribe = action.actionType == SubscriptionActionProgressType.UNSUBSCRIBE
        val title = if (isUnsubscribe) {
            "Unsubscribing from ${action.subscriptionName}"
        } else {
            "Subscribing to ${action.subscriptionName}"
        }
        val message = if (isUnsubscribe) {
            "Tap Completed once you finish unsubscribing, or Cancel."
        } else {
            "Tap Completed once you finish subscribing, or Cancel."
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, action.subscriptionId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            ACTION_PROGRESS_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            this.action = SubscriptionActionReceiver.ACTION_COMPLETE_PROGRESS
            putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, action.subscriptionId)
            putExtra(SubscriptionActionReceiver.EXTRA_ACTION_TYPE, action.actionType.name)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_PROGRESS_NOTIFICATION_ID + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            this.action = SubscriptionActionReceiver.ACTION_CANCEL_PROGRESS
            putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, action.subscriptionId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_PROGRESS_NOTIFICATION_ID + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Completed", completePendingIntent)
            .addAction(0, "Cancel", cancelPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ACTION_PROGRESS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // DataStore persistence handles recovery on next launch.
        }
    }

    fun dismissActionProgressNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(ACTION_PROGRESS_NOTIFICATION_ID)
    }

    fun showCalendarSuggestion(context: Context, subscriptionId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, subscriptionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            CALENDAR_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = "A calendar event overlaps an upcoming subscription decision. Open SubCycler to review."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setContentTitle("Subscription review")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(CALENDAR_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Calendar opt-in never substitutes for notification permission.
        }
    }
}

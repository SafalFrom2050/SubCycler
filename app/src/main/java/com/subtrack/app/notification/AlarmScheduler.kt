package com.subtrack.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.ActionType
import com.subtrack.core.domain.model.Subscription
import com.subtrack.app.receiver.SubscriptionAlarmReceiver
import java.time.LocalDate
import java.time.ZoneId

object AlarmScheduler {

    fun scheduleSubscriptionAlert(context: Context, subscription: Subscription) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cycleStatus = RotationEngine.calculateCycleStatus(subscription, LocalDate.now())
        val nextAction = cycleStatus.nextAction

        if (nextAction.actionType == ActionType.NO_ACTION) return

        val targetDate = nextAction.targetDate
        val targetDateTime = targetDate.atTime(9, 0) // Alert at 9:00 AM on target date
        val triggerEpochMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (triggerEpochMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", "${subscription.name}: ${nextAction.title}")
            putExtra("EXTRA_MESSAGE", nextAction.description)
            putExtra("EXTRA_NOTIFICATION_ID", subscription.id.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subscription.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelSubscriptionAlert(context: Context, subscriptionId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SubscriptionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subscriptionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

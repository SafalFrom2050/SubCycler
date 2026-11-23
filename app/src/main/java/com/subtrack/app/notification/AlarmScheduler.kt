package com.subtrack.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.subtrack.app.receiver.SubscriptionAlarmReceiver
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.ActionType
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.Subscription
import java.time.LocalDate
import java.time.ZoneId

object AlarmScheduler {

    fun scheduleSubscriptionAlert(
        context: Context,
        subscription: Subscription,
        leadDays: Int = 3
    ) {
        if (subscription.id <= 0L) return
        cancelSubscriptionAlert(context, subscription.id)

        // Only schedule cancellation / re-subscribe alerts for complete, actively rotating monthly services.
        if (!subscription.isRotating || subscription.isIncomplete) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val today = LocalDate.now()
        val cycleStatus = RotationEngine.calculateCycleStatus(subscription, today)
        val nextAction = cycleStatus.nextAction

        if (nextAction.actionType == ActionType.NO_ACTION) return
        if (cycleStatus.phase == CyclePhase.ACTIVE && cycleStatus.isAutoRenewCancelledForCurrentCycle) return

        val safeLead = leadDays.coerceIn(1, 14)
        val idealDate = when (cycleStatus.phase) {
            CyclePhase.ACTIVE -> nextAction.targetDate.minusDays(safeLead.toLong())
            CyclePhase.BLACKOUT -> nextAction.targetDate.minusDays(1L)
        }
        val zone = ZoneId.systemDefault()
        val nowMillis = System.currentTimeMillis()
        val candidateDates = listOfNotNull(
            idealDate.takeUnless { it.isBefore(today) },
            today.takeUnless { it.isAfter(nextAction.targetDate) },
            today.plusDays(1L).takeUnless { it.isAfter(nextAction.targetDate) },
            nextAction.targetDate
        ).distinct()

        val triggerEpochMillis = candidateDates
            .map { date -> date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli() }
            .firstOrNull { it > nowMillis }
            ?: return

        val dedupKey = ReminderDeduplicator.dedupKey(
            subscriptionId = subscription.id,
            phase = cycleStatus.phase,
            phaseEndDate = cycleStatus.phaseEndDate
        )
        if (ReminderDeduplicator.hasFired(context, dedupKey)) return

        val notificationId = ReminderDeduplicator.notificationIdFor(subscription.id, cycleStatus.phase)

        val (title, message) = when (cycleStatus.phase) {
            CyclePhase.ACTIVE -> {
                val formattedEnd = RotationEngine.formatReadableDate(cycleStatus.phaseEndDate)
                val savingsStr = MoneyFormatter.format(cycleStatus.annualSavings, subscription.currency, maximumFractionDigits = 0)
                "Cancel auto-renew: ${subscription.name}" to
                    "Turn off auto-renew before $formattedEnd to keep saving ~$savingsStr/yr."
            }
            CyclePhase.BLACKOUT -> {
                val watchlistNote = subscription.notes.trim()
                val base = "Your ${subscription.name} break ends ${RotationEngine.formatReadableDate(cycleStatus.phaseEndDate)}."
                val body = if (watchlistNote.isNotEmpty()) {
                    "$base Queued: $watchlistNote"
                } else {
                    "$base Ready to start your next ${subscription.activeDays}-day binge window."
                }
                "Ready to re-subscribe: ${subscription.name}" to body
            }
        }

        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            putExtra(SubscriptionAlarmReceiver.EXTRA_TITLE, title)
            putExtra(SubscriptionAlarmReceiver.EXTRA_MESSAGE, message)
            putExtra(SubscriptionAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SubscriptionAlarmReceiver.EXTRA_SUBSCRIPTION_ID, subscription.id)
            putExtra(SubscriptionAlarmReceiver.EXTRA_DEDUP_KEY, dedupKey)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subscription.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelSubscriptionAlert(context: Context, subscriptionId: Long) {
        if (subscriptionId <= 0L) return
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

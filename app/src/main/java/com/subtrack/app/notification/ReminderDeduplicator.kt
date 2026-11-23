package com.subtrack.app.notification

import android.content.Context
import com.subtrack.core.domain.model.CyclePhase
import java.time.LocalDate

/**
 * Shared local deduplication and canonical notification-ID registry for both
 * [AlarmScheduler]/[com.subtrack.app.receiver.SubscriptionAlarmReceiver] and
 * [com.subtrack.app.worker.DailyCycleEvaluationWorker], preventing duplicate notifications
 * for the same subscription phase deadline.
 */
object ReminderDeduplicator {
    private const val PREFS_NAME = "subcycler_daily_alert_dedup"

    fun notificationIdFor(subscriptionId: Long, phase: CyclePhase): Int = when (phase) {
        CyclePhase.ACTIVE -> subscriptionId.toInt()
        CyclePhase.BLACKOUT -> subscriptionId.toInt() + 10_000
    }

    fun dedupKey(subscriptionId: Long, phase: CyclePhase, phaseEndDate: LocalDate): String =
        "$subscriptionId:${phase.name}:$phaseEndDate"

    fun hasFired(context: Context, key: String): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key, false)

    fun markFired(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, true)
            .apply()
    }
}

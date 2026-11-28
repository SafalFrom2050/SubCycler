package com.subtrack.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.app.notification.ReminderDeduplicator
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.data.local.AppDatabase
import com.subtrack.core.data.local.mapper.toDomain
import com.subtrack.core.data.preferences.UserPreferencesRepositoryImpl
import com.subtrack.core.data.preferences.userDataStore
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CyclePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

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

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = runCatching {
                    UserPreferencesRepositoryImpl(
                        dataStore = context.userDataStore,
                        defaultCurrencyCode = MoneyFormatter.currencyCodeForLocale()
                    ).userPreferences.first()
                }.getOrNull()
                val launcherIcon = prefs?.launcherIcon ?: AppLauncherIcon.BOTANICAL
                val themeMode = prefs?.themeMode ?: AppThemeMode.SYSTEM
                val useStackMood = prefs?.useStackHeroMood ?: true

                val allSubs = runCatching {
                    AppDatabase.getDatabase(context).subscriptionDao().getAllSubscriptionsList().toDomain()
                }.getOrDefault(emptyList())
                val today = LocalDate.now()
                val activeSubsForMood = allSubs.filter { subItem ->
                    !subItem.isIncomplete &&
                        RotationEngine.calculateCycleStatus(subItem, today).phase == CyclePhase.ACTIVE
                }
                val sub = if (subscriptionId != null) {
                    allSubs.firstOrNull { it.id == subscriptionId }
                } else {
                    null
                }

                if (sub != null) {
                    val status = RotationEngine.calculateCycleStatus(sub, today)
                    NotificationHelper.showCycleReminderNotification(
                        context = context,
                        cycleStatus = status,
                        notificationId = notificationId,
                        title = title,
                        message = message,
                        launcherIcon = launcherIcon,
                        themeMode = themeMode,
                        useStackMood = useStackMood,
                        activeSubscriptions = activeSubsForMood
                    )
                } else {
                    NotificationHelper.showNotification(
                        context = context,
                        notificationId = notificationId,
                        title = title,
                        message = message,
                        subscriptionId = subscriptionId,
                        launcherIcon = launcherIcon,
                        themeMode = themeMode,
                        useStackMood = useStackMood,
                        activeSubscriptions = activeSubsForMood
                    )
                }

                if (!dedupKey.isNullOrBlank()) {
                    ReminderDeduplicator.markFired(context, dedupKey)
                }
            } finally {
                pendingResult.finish()
            }
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

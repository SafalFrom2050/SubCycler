package com.subtrack.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subtrack.app.notification.AlarmScheduler
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.app.notification.ReminderDeduplicator
import com.subtrack.app.widget.SubCyclerWidgets
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class DailyCycleEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getSubscriptionCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userPrefs = userPreferencesRepository.userPreferences.first()
            getSubscriptionCycleStatusesUseCase.refreshToday()
            val statuses = getSubscriptionCycleStatusesUseCase().first()

            statuses.forEach { cycleStatus ->
                val sub = cycleStatus.subscription
                // Never fire cancel-to-save alerts for incomplete, continuous, or yearly non-rotating subscriptions.
                if (!sub.isRotating || sub.isIncomplete) return@forEach

                val daysRemaining = cycleStatus.daysRemainingInPhase
                val dedupKey = ReminderDeduplicator.dedupKey(
                    subscriptionId = sub.id,
                    phase = cycleStatus.phase,
                    phaseEndDate = cycleStatus.phaseEndDate
                )
                val notificationId = ReminderDeduplicator.notificationIdFor(sub.id, cycleStatus.phase)

                when (cycleStatus.phase) {
                    CyclePhase.ACTIVE -> {
                        if (
                            daysRemaining <= userPrefs.notificationLeadDays &&
                            !cycleStatus.isAutoRenewCancelledForCurrentCycle &&
                            !ReminderDeduplicator.hasFired(applicationContext, dedupKey)
                        ) {
                            val endFormatted = RotationEngine.formatReadableDate(cycleStatus.phaseEndDate)
                            val annualSaveFormatted = MoneyFormatter.format(
                                cycleStatus.annualSavings,
                                sub.currency,
                                maximumFractionDigits = 0
                            )
                            val whenText = if (daysRemaining <= 0) {
                                "Active window ends today ($endFormatted)"
                            } else {
                                "Active window ends in $daysRemaining day${if (daysRemaining == 1) "" else "s"} ($endFormatted)"
                            }
                            NotificationHelper.showCycleReminderNotification(
                                context = applicationContext,
                                cycleStatus = cycleStatus,
                                notificationId = notificationId,
                                title = "Cancel auto-renew: ${sub.name}",
                                message = "$whenText. Turn off auto-renew to save ~$annualSaveFormatted/yr.",
                                launcherIcon = userPrefs.launcherIcon,
                                themeMode = userPrefs.themeMode
                            )
                            ReminderDeduplicator.markFired(applicationContext, dedupKey)
                        }
                    }
                    CyclePhase.BLACKOUT -> {
                        if (daysRemaining <= 1 && !ReminderDeduplicator.hasFired(applicationContext, dedupKey)) {
                            val watchlist = sub.notes.trim()
                            val timing = if (daysRemaining <= 0) "today" else "tomorrow"
                            val body = if (watchlist.isNotEmpty()) {
                                "${sub.name} break ends $timing. Watchlist: $watchlist"
                            } else {
                                "${sub.name} break ends $timing. Ready for your next ${sub.activeDays}-day binge window."
                            }
                            NotificationHelper.showCycleReminderNotification(
                                context = applicationContext,
                                cycleStatus = cycleStatus,
                                notificationId = notificationId,
                                title = "Ready to re-subscribe: ${sub.name}",
                                message = body,
                                launcherIcon = userPrefs.launcherIcon,
                                themeMode = userPrefs.themeMode
                            )
                            ReminderDeduplicator.markFired(applicationContext, dedupKey)
                        }
                    }
                }

                // Keep upcoming phase/cycle alarms armed automatically across rollovers.
                AlarmScheduler.scheduleSubscriptionAlert(
                    context = applicationContext,
                    subscription = sub,
                    leadDays = userPrefs.notificationLeadDays
                )
            }

            SubCyclerWidgets.requestUpdateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "com.subtrack.app.worker.DailyCycleEvaluationWorker"
    }
}

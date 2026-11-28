package com.subtrack.app.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.repository.CalendarAwarenessRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit

@HiltWorker
class CalendarReconciliationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarRepository: CalendarAwarenessRepository,
    private val statusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!calendarRepository.settings.first().isEnabled) return Result.success()

            val today = timeProvider.today()
            val statuses = statusesUseCase().first()
            val suggestions = calendarRepository.refresh(statuses, today)
            if (!calendarRepository.settings.first().isEnabled) {
                WorkSchedulerManager.cancelCalendarReconciliation(applicationContext)
                return Result.success()
            }

        // Ordinary reminders own the configured lead window. Calendar-only notices are used
        // only earlier, avoiding two notifications for the same decision.
            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                val userPrefs = userPreferencesRepository.userPreferences.first()
                val ordinaryLeadDays = userPrefs.notificationLeadDays
                val candidate = suggestions.firstOrNull {
                    ChronoUnit.DAYS.between(today, it.decisionDate) > ordinaryLeadDays
                }
                if (candidate != null && calendarRepository.claimNotification(candidate.identity, timeProvider.nowInstant())) {
                    val matchedSub = statuses.firstOrNull { it.subscription.id == candidate.identity.subscriptionId }?.subscription
                    val activeForMood = statuses
                        .filter { !it.subscription.isIncomplete && it.phase == CyclePhase.ACTIVE }
                        .map { it.subscription }
                    NotificationHelper.showCalendarSuggestion(
                        context = applicationContext,
                        subscriptionId = candidate.identity.subscriptionId,
                        subscriptionName = matchedSub?.name,
                        colorHex = matchedSub?.colorHex,
                        launcherIcon = userPrefs.launcherIcon,
                        themeMode = userPrefs.themeMode,
                        useStackMood = userPrefs.useStackHeroMood,
                        activeSubscriptions = activeForMood
                    )
                }
            }
            Result.success()
        } catch (_: SecurityException) {
            WorkSchedulerManager.cancelCalendarReconciliation(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "com.subtrack.app.worker.CalendarReconciliationWorker"
        const val UNIQUE_IMMEDIATE_WORK_NAME = "${UNIQUE_PERIODIC_WORK_NAME}_immediate"
    }
}

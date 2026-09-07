package com.subtrack.app.worker

import android.content.Context
import com.subtrack.core.common.currency.MoneyFormatter
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import com.subtrack.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class DailyCycleEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SubscriptionRepository,
    private val getSubscriptionCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userPrefs = userPreferencesRepository.userPreferences.first()
            val statuses = getSubscriptionCycleStatusesUseCase().first()

            statuses.forEach { cycleStatus ->
                val sub = cycleStatus.subscription
                val daysRemaining = cycleStatus.daysRemainingInPhase

                when (cycleStatus.phase) {
                    CyclePhase.ACTIVE -> {
                        // Alert user when approaching blackout transition
                        if (daysRemaining <= userPrefs.notificationLeadDays && !sub.isAutoRenewCancelled) {
                            NotificationHelper.showNotification(
                                context = applicationContext,
                                notificationId = sub.id.toInt(),
                                title = "Cancel Auto-Renew: ${sub.name}",
                                message = "${sub.name} active window ends in $daysRemaining day(s). Cancel auto-renew to save ${MoneyFormatter.format(cycleStatus.annualSavings, sub.currency)}/yr!"
                            )
                        }
                    }
                    CyclePhase.BLACKOUT -> {
                        // Alert user when blackout is about to conclude
                        if (daysRemaining <= 1) {
                            NotificationHelper.showNotification(
                                context = applicationContext,
                                notificationId = sub.id.toInt() + 10000,
                                title = "Service Resuming: ${sub.name}",
                                message = "${sub.name} blackout ends tomorrow! Prepare for your upcoming active rotation cycle."
                            )
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "com.subtrack.app.worker.DailyCycleEvaluationWorker"
    }
}

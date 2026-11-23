package com.subtrack.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subtrack.app.notification.AlarmScheduler
import com.subtrack.app.widget.SubCyclerWidgetProvider
import com.subtrack.app.worker.WorkSchedulerManager
import com.subtrack.core.common.dispatcher.IoDispatcher
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SubscriptionRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            WorkSchedulerManager.scheduleDailyCycleEvaluation(context)
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + ioDispatcher).launch {
                try {
                    val leadDays = userPreferencesRepository.userPreferences.first().notificationLeadDays
                    val subs = repository.getAllSubscriptionsList()
                    subs.forEach { sub ->
                        AlarmScheduler.scheduleSubscriptionAlert(context, sub, leadDays)
                    }
                    SubCyclerWidgetProvider.requestUpdate(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

package com.subtrack.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var manageSubscriptionUseCase: ManageSubscriptionUseCase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var timeProvider: TimeProvider

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_COMPLETE_PROGRESS && action != ACTION_CANCEL_PROGRESS) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val persisted = userPreferencesRepository.userPreferences.first().pendingSubscriptionAction
                val extraSubId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1L).takeIf { it > 0 }
                val extraType = intent.getStringExtra(EXTRA_ACTION_TYPE)
                    ?.let { raw -> runCatching { SubscriptionActionProgressType.valueOf(raw) }.getOrNull() }

                val targetSubId = persisted?.subscriptionId ?: extraSubId
                val targetActionType = persisted?.actionType ?: extraType

                if (action == ACTION_COMPLETE_PROGRESS && targetSubId != null && targetActionType != null) {
                    manageSubscriptionUseCase.completeSubscriptionAction(
                        subscriptionId = targetSubId,
                        actionType = targetActionType,
                        today = timeProvider.today()
                    )
                }

                userPreferencesRepository.setPendingSubscriptionAction(null)
                NotificationHelper.dismissActionProgressNotification(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_PROGRESS = "com.subtrack.app.action.COMPLETE_SUBSCRIPTION_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.subtrack.app.action.CANCEL_SUBSCRIPTION_PROGRESS"
        const val EXTRA_SUBSCRIPTION_ID = "com.subtrack.app.extra.PROGRESS_SUBSCRIPTION_ID"
        const val EXTRA_ACTION_TYPE = "com.subtrack.app.extra.PROGRESS_ACTION_TYPE"
    }
}

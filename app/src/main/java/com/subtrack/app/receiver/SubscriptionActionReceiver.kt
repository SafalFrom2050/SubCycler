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
        val supported = setOf(
            ACTION_COMPLETE_PROGRESS,
            ACTION_CANCEL_PROGRESS,
            ACTION_MARK_CANCELLED,
            ACTION_SKIP_BREAK,
            ACTION_BANK_AND_RESUME
        )
        if (action !in supported) return

        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            NotificationHelper.ACTION_PROGRESS_NOTIFICATION_ID
        )
        val isPreview = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)
        if (isPreview) {
            NotificationHelper.dismissNotification(context, notificationId)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val extraSubId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1L).takeIf { it > 0L }
                when (action) {
                    ACTION_COMPLETE_PROGRESS, ACTION_CANCEL_PROGRESS -> {
                        val persisted = userPreferencesRepository.userPreferences.first().pendingSubscriptionAction
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
                        NotificationHelper.dismissActionProgressNotification(context, notificationId)
                    }

                    ACTION_MARK_CANCELLED -> {
                        if (extraSubId != null) {
                            manageSubscriptionUseCase.completeSubscriptionAction(
                                subscriptionId = extraSubId,
                                actionType = SubscriptionActionProgressType.UNSUBSCRIBE,
                                today = timeProvider.today()
                            )
                        }
                        NotificationHelper.dismissNotification(context, notificationId)
                    }

                    ACTION_SKIP_BREAK -> {
                        if (extraSubId != null) {
                            manageSubscriptionUseCase.extendActiveCycle(
                                subscriptionId = extraSubId,
                                today = timeProvider.today()
                            )
                        }
                        NotificationHelper.dismissNotification(context, notificationId)
                    }

                    ACTION_BANK_AND_RESUME -> {
                        if (extraSubId != null) {
                            manageSubscriptionUseCase.completeSubscriptionAction(
                                subscriptionId = extraSubId,
                                actionType = SubscriptionActionProgressType.SUBSCRIBE,
                                today = timeProvider.today()
                            )
                        }
                        NotificationHelper.dismissNotification(context, notificationId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_PROGRESS = "com.subtrack.app.action.COMPLETE_SUBSCRIPTION_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.subtrack.app.action.CANCEL_SUBSCRIPTION_PROGRESS"
        const val ACTION_MARK_CANCELLED = "com.subtrack.app.action.MARK_AUTO_RENEW_CANCELLED"
        const val ACTION_SKIP_BREAK = "com.subtrack.app.action.SKIP_UPCOMING_BREAK"
        const val ACTION_BANK_AND_RESUME = "com.subtrack.app.action.BANK_AND_RESUME"
        const val EXTRA_SUBSCRIPTION_ID = "com.subtrack.app.extra.PROGRESS_SUBSCRIPTION_ID"
        const val EXTRA_ACTION_TYPE = "com.subtrack.app.extra.PROGRESS_ACTION_TYPE"
        const val EXTRA_NOTIFICATION_ID = "com.subtrack.app.extra.NOTIFICATION_ID"
        const val EXTRA_IS_PREVIEW = "com.subtrack.app.extra.IS_PREVIEW"
    }
}

package com.subtrack.app.notification

import android.content.Context
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.repository.SubscriptionActionNotifier
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSubscriptionActionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionRepository: SubscriptionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : SubscriptionActionNotifier {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun showActiveProgressNotification(action: PendingSubscriptionAction) {
        scope.launch {
            val subColor = runCatching {
                subscriptionRepository.getSubscriptionById(action.subscriptionId)?.colorHex
            }.getOrNull()
            val prefs = runCatching { userPreferencesRepository.userPreferences.first() }.getOrNull()
            NotificationHelper.showActionProgressNotification(
                context = context,
                action = action,
                serviceColorHex = subColor,
                launcherIcon = prefs?.launcherIcon ?: AppLauncherIcon.BOTANICAL,
                themeMode = prefs?.themeMode ?: AppThemeMode.SYSTEM
            )
        }
    }

    override fun dismissProgressNotification(subscriptionId: Long?) {
        NotificationHelper.dismissActionProgressNotification(context)
    }
}

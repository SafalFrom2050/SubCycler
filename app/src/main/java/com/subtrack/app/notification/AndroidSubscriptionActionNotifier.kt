package com.subtrack.app.notification

import android.content.Context
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.repository.SubscriptionActionNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSubscriptionActionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : SubscriptionActionNotifier {

    override fun showActiveProgressNotification(action: PendingSubscriptionAction) {
        NotificationHelper.showActionProgressNotification(context, action)
    }

    override fun dismissProgressNotification(subscriptionId: Long?) {
        NotificationHelper.dismissActionProgressNotification(context)
    }
}

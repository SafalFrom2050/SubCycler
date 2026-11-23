package com.subtrack.core.domain.testing

import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.repository.SubscriptionActionNotifier

class FakeSubscriptionActionNotifier : SubscriptionActionNotifier {
    var activeNotification: PendingSubscriptionAction? = null
        private set

    var dismissCount: Int = 0
        private set

    override fun showActiveProgressNotification(action: PendingSubscriptionAction) {
        activeNotification = action
    }

    override fun dismissProgressNotification(subscriptionId: Long?) {
        activeNotification = null
        dismissCount++
    }
}

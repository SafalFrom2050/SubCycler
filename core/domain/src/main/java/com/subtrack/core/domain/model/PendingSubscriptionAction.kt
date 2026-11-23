package com.subtrack.core.domain.model

enum class SubscriptionActionProgressType {
    SUBSCRIBE,
    UNSUBSCRIBE
}

data class PendingSubscriptionAction(
    val subscriptionId: Long,
    val subscriptionName: String,
    val actionType: SubscriptionActionProgressType,
    val startedAtEpochMillis: Long = 0L
)

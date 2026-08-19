package com.subtrack.app.data.local.mapper

import com.subtrack.app.data.local.entity.SubscriptionEntity
import com.subtrack.app.data.model.Subscription

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    cost = cost,
    currency = currency,
    category = category,
    startDate = startDate,
    strategy = strategy,
    customActiveDays = customActiveDays,
    customBlackoutDays = customBlackoutDays,
    colorHex = colorHex,
    iconName = iconName,
    notes = notes,
    isAutoRenewCancelled = isAutoRenewCancelled,
    pairedWithSubscriptionId = pairedWithSubscriptionId
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    name = name,
    cost = cost,
    currency = currency,
    category = category,
    startDate = startDate,
    strategy = strategy,
    customActiveDays = customActiveDays,
    customBlackoutDays = customBlackoutDays,
    colorHex = colorHex,
    iconName = iconName,
    notes = notes,
    isAutoRenewCancelled = isAutoRenewCancelled,
    pairedWithSubscriptionId = pairedWithSubscriptionId
)

fun List<SubscriptionEntity>.toDomain(): List<Subscription> = map { it.toDomain() }
fun List<Subscription>.toEntity(): List<SubscriptionEntity> = map { it.toEntity() }

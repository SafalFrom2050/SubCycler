package com.subtrack.core.data.local.mapper

import com.subtrack.core.data.local.entity.SubscriptionEntity
import com.subtrack.core.domain.model.Subscription

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    cost = cost,
    currency = currency,
    billingPeriod = billingPeriod,
    category = category,
    startDate = startDate,
    strategy = strategy,
    customActiveDays = customActiveDays,
    customBlackoutDays = customBlackoutDays,
    colorHex = colorHex,
    iconName = iconName,
    notes = notes,
    isAutoRenewCancelled = isAutoRenewCancelled,
    pairedWithSubscriptionId = pairedWithSubscriptionId,
    isBillingDateConfirmed = isBillingDateConfirmed,
    isStrategyConfirmed = isStrategyConfirmed,
    cancelledCycleIndex = cancelledCycleIndex,
    completedBreaksCount = completedBreaksCount,
    earnedSavingsTotal = earnedSavingsTotal,
    bonusActiveDays = bonusActiveDays
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    name = name,
    cost = cost,
    currency = currency,
    billingPeriod = billingPeriod,
    category = category,
    startDate = startDate,
    strategy = strategy,
    customActiveDays = customActiveDays,
    customBlackoutDays = customBlackoutDays,
    colorHex = colorHex,
    iconName = iconName,
    notes = notes,
    isAutoRenewCancelled = isAutoRenewCancelled,
    pairedWithSubscriptionId = pairedWithSubscriptionId,
    isBillingDateConfirmed = isBillingDateConfirmed,
    isStrategyConfirmed = isStrategyConfirmed,
    cancelledCycleIndex = cancelledCycleIndex,
    completedBreaksCount = completedBreaksCount,
    earnedSavingsTotal = earnedSavingsTotal,
    bonusActiveDays = bonusActiveDays
)

fun List<SubscriptionEntity>.toDomain(): List<Subscription> = map { it.toDomain() }
fun List<Subscription>.toEntity(): List<SubscriptionEntity> = map { it.toEntity() }

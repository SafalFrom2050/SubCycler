package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.repository.NoOpSubscriptionReminderScheduler
import com.subtrack.core.domain.repository.SubscriptionReminderScheduler
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Use case to manage subscription lifecycle, flexible cycle controls, earned savings recording,
 * offline backup import, and local on-device reminder synchronization.
 */
class ManageSubscriptionUseCase(
    private val repository: SubscriptionRepository,
    private val reminderScheduler: SubscriptionReminderScheduler = NoOpSubscriptionReminderScheduler,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) {
    private suspend fun leadDays(): Int =
        userPreferencesRepository?.userPreferences?.first()?.notificationLeadDays ?: 3

    suspend fun addSubscription(subscription: Subscription): Long {
        val id = repository.addSubscription(subscription)
        val saved = subscription.copy(id = id)
        reminderScheduler.scheduleForSubscription(saved, leadDays())
        reminderScheduler.notifyPortfolioChanged()
        return id
    }

    suspend fun addSubscriptions(subscriptions: List<Subscription>): List<Long> {
        val ids = repository.addSubscriptions(subscriptions)
        val lead = leadDays()
        subscriptions.zip(ids).forEach { (sub, id) ->
            reminderScheduler.scheduleForSubscription(sub.copy(id = id), lead)
        }
        reminderScheduler.notifyPortfolioChanged()
        return ids
    }

    suspend fun addSubscriptionsIfMissing(subscriptions: List<Subscription>): List<Long> {
        val existingNames = repository.getAllSubscriptionsList()
            .map { it.name.trim().lowercase(Locale.ROOT) }
            .toSet()
        val missingSubscriptions = subscriptions.filter { subscription ->
            subscription.name.trim().lowercase(Locale.ROOT) !in existingNames
        }
        return if (missingSubscriptions.isEmpty()) {
            emptyList()
        } else {
            addSubscriptions(missingSubscriptions)
        }
    }

    suspend fun updateSubscription(subscription: Subscription) {
        repository.updateSubscription(subscription)
        reminderScheduler.scheduleForSubscription(subscription, leadDays())
        reminderScheduler.notifyPortfolioChanged()
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        repository.deleteSubscription(subscription)
        reminderScheduler.cancelForSubscription(subscription.id)
        reminderScheduler.notifyPortfolioChanged()
    }

    suspend fun toggleAutoRenew(subscriptionId: Long, today: LocalDate = LocalDate.now()) {
        val existing = repository.getSubscriptionById(subscriptionId) ?: return
        val cycleIndex = RotationEngine.cycleIndexFor(existing, today)
        val currentlyCancelled = RotationEngine.isAutoRenewCancelledForCycle(existing, cycleIndex)
        val updated = if (currentlyCancelled) {
            existing.copy(isAutoRenewCancelled = false, cancelledCycleIndex = null)
        } else {
            existing.copy(isAutoRenewCancelled = true, cancelledCycleIndex = cycleIndex)
        }
        updateSubscription(updated)
    }

    suspend fun completeSubscriptionAction(
        subscriptionId: Long,
        actionType: SubscriptionActionProgressType,
        today: LocalDate
    ) {
        val existing = repository.getSubscriptionById(subscriptionId) ?: return
        val currentStatus = RotationEngine.calculateCycleStatus(existing, today)
        val currentCycleIndex = currentStatus.currentCycleNumber - 1

        val updated = when (actionType) {
            SubscriptionActionProgressType.UNSUBSCRIBE -> existing.copy(
                isAutoRenewCancelled = true,
                cancelledCycleIndex = currentCycleIndex
            )
            SubscriptionActionProgressType.SUBSCRIBE -> {
                val daysElapsed = ChronoUnit.DAYS.between(existing.startDate, today)
                val earnedDelta = when {
                    !existing.isRotating -> 0.0
                    currentStatus.hasUnbankedCompletedBreak -> currentStatus.perBreakSavings
                    existing.isAutoRenewCancelled &&
                        existing.cancelledCycleIndex == null &&
                        daysElapsed >= existing.activeDays -> currentStatus.perBreakSavings
                    currentStatus.phase == CyclePhase.BLACKOUT -> {
                        // Prevent 0-day Pause -> Resume exploit: require >= 1 real day paused
                        val daysPaused = ChronoUnit.DAYS.between(currentStatus.phaseStartDate, today)
                        if (daysPaused >= 1L) {
                            val fraction = (daysPaused.toDouble() / existing.blackoutDays.coerceAtLeast(1))
                                .coerceIn(0.0, 1.0)
                            currentStatus.perBreakSavings * fraction
                        } else {
                            0.0
                        }
                    }
                    else -> 0.0
                }
                val completedBreak = earnedDelta > 0.0
                existing.copy(
                    startDate = today,
                    bonusActiveDays = 0,
                    isAutoRenewCancelled = false,
                    cancelledCycleIndex = null,
                    isBillingDateConfirmed = true,
                    completedBreaksCount = existing.completedBreaksCount + if (completedBreak) 1 else 0,
                    earnedSavingsTotal = existing.earnedSavingsTotal + earnedDelta
                )
            }
        }
        updateSubscription(updated)
    }

    /**
     * Skip the upcoming break and genuinely extend the current active window by +[Subscription.activeDays] days.
     */
    suspend fun extendActiveCycle(subscriptionId: Long, today: LocalDate) {
        val existing = repository.getSubscriptionById(subscriptionId) ?: return
        val status = RotationEngine.calculateCycleStatus(existing, today)
        val bankDelta = if (status.hasUnbankedCompletedBreak) status.perBreakSavings else 0.0
        val bankBreaks = if (status.hasUnbankedCompletedBreak) 1 else 0
        val updated = if (existing.isRotating && status.phase == CyclePhase.ACTIVE) {
            if (status.currentCycleNumber == 1) {
                existing.copy(
                    bonusActiveDays = existing.bonusActiveDays + existing.activeDays,
                    isAutoRenewCancelled = false,
                    cancelledCycleIndex = null,
                    isBillingDateConfirmed = true,
                    completedBreaksCount = existing.completedBreaksCount + bankBreaks,
                    earnedSavingsTotal = existing.earnedSavingsTotal + bankDelta
                )
            } else {
                existing.copy(
                    startDate = status.phaseStartDate,
                    bonusActiveDays = existing.activeDays,
                    isAutoRenewCancelled = false,
                    cancelledCycleIndex = null,
                    isBillingDateConfirmed = true,
                    completedBreaksCount = existing.completedBreaksCount + bankBreaks,
                    earnedSavingsTotal = existing.earnedSavingsTotal + bankDelta
                )
            }
        } else {
            existing.copy(
                startDate = today,
                bonusActiveDays = 0,
                isAutoRenewCancelled = false,
                cancelledCycleIndex = null,
                isBillingDateConfirmed = true,
                completedBreaksCount = existing.completedBreaksCount + bankBreaks,
                earnedSavingsTotal = existing.earnedSavingsTotal + bankDelta
            )
        }
        updateSubscription(updated)
    }

    /**
     * Immediately begin the pause/break window today.
     */
    suspend fun startPauseToday(subscriptionId: Long, today: LocalDate) {
        val existing = repository.getSubscriptionById(subscriptionId) ?: return
        if (!existing.isRotating) return
        val updated = existing.copy(
            startDate = today.minusDays(existing.activeDays.toLong()),
            bonusActiveDays = 0,
            isAutoRenewCancelled = true,
            cancelledCycleIndex = 0,
            isBillingDateConfirmed = true
        )
        updateSubscription(updated)
    }

    /**
     * 1-tap switch from Continuous monthly plan to 45-Day Smart Rotation.
     */
    suspend fun switchToSmartRotation(subscriptionId: Long) {
        val existing = repository.getSubscriptionById(subscriptionId) ?: return
        if (existing.billingPeriod != BillingPeriod.MONTHLY) return
        val updated = existing.copy(
            strategy = RotationStrategy.ROTATION_45_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 15,
            isStrategyConfirmed = true,
            bonusActiveDays = 0,
            isAutoRenewCancelled = false,
            cancelledCycleIndex = null
        )
        updateSubscription(updated)
    }

    /**
     * Switches all stored subscriptions to the app's single currency without performing
     * exchange-rate conversion (numeric prices remain unchanged for manual updates).
     */
    suspend fun updateAllSubscriptionsCurrency(newCurrencyCode: String) {
        val normalized = newCurrencyCode.trim().uppercase(Locale.ROOT)
        if (normalized.isBlank()) return
        val existing = repository.getAllSubscriptionsList()
        val updatedList = existing.map { sub ->
            if (sub.currency == normalized) {
                sub
            } else {
                sub.copy(currency = normalized).also { repository.updateSubscription(it) }
            }
        }
        reminderScheduler.rescheduleAll(updatedList, leadDays())
        reminderScheduler.notifyPortfolioChanged()
    }

    suspend fun getAllSubscriptionsSnapshot(): List<Subscription> =
        repository.getAllSubscriptionsList()

    suspend fun rescheduleAllReminders(leadDays: Int) {
        val all = repository.getAllSubscriptionsList()
        reminderScheduler.rescheduleAll(all, leadDays)
        reminderScheduler.notifyPortfolioChanged()
    }
}

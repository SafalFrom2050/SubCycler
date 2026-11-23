package com.subtrack.core.domain.usecase

import com.subtrack.core.common.time.DefaultTimeProvider
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.repository.NoOpSubscriptionReminderScheduler
import com.subtrack.core.domain.repository.SubscriptionReminderScheduler
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Use case to synchronize two monthly subscriptions into alternating 30d/30d continuous entertainment.
 */
class AlignPairedSubscriptionsUseCase(
    private val repository: SubscriptionRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider(),
    private val reminderScheduler: SubscriptionReminderScheduler = NoOpSubscriptionReminderScheduler,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) {
    suspend operator fun invoke(service1Id: Long, service2Id: Long) {
        val s1 = repository.getSubscriptionById(service1Id) ?: return
        val s2 = repository.getSubscriptionById(service2Id) ?: return
        if (s1.billingPeriod != BillingPeriod.MONTHLY || s2.billingPeriod != BillingPeriod.MONTHLY) return
        val today = timeProvider.today()

        // S1 active now (30d active + 30d pause)
        val updatedS1 = s1.copy(
            startDate = today,
            strategy = RotationStrategy.ROTATION_60_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 30,
            bonusActiveDays = 0,
            isAutoRenewCancelled = false,
            cancelledCycleIndex = null,
            isBillingDateConfirmed = true,
            pairedWithSubscriptionId = s2.id
        )

        // S2 paused now, activates in 30 days
        val updatedS2 = s2.copy(
            startDate = today.minusDays(30),
            strategy = RotationStrategy.ROTATION_60_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 30,
            bonusActiveDays = 0,
            isAutoRenewCancelled = true,
            cancelledCycleIndex = 0,
            isBillingDateConfirmed = true,
            pairedWithSubscriptionId = s1.id
        )

        repository.updateSubscription(updatedS1)
        repository.updateSubscription(updatedS2)

        val leadDays = userPreferencesRepository?.userPreferences?.first()?.notificationLeadDays ?: 3
        reminderScheduler.scheduleForSubscription(updatedS1, leadDays)
        reminderScheduler.scheduleForSubscription(updatedS2, leadDays)
        reminderScheduler.notifyPortfolioChanged()
    }
}

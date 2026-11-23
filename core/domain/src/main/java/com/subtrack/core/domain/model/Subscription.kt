package com.subtrack.core.domain.model

import java.time.LocalDate

enum class RotationStrategy(
    val displayName: String,
    val description: String,
    val activeDays: Int,
    val blackoutDays: Int
) {
    ROTATION_45_DAYS(
        displayName = "45-Day Smart Rotation (Recommended)",
        description = "30d Active + 15d Blackout. Saves 33.3% annually.",
        activeDays = 30,
        blackoutDays = 15
    ),
    ROTATION_60_DAYS(
        displayName = "60-Day Bi-Monthly",
        description = "30d Active + 30d Blackout. Saves 50% annually.",
        activeDays = 30,
        blackoutDays = 30
    ),
    CUSTOM(
        displayName = "Custom Cycle",
        description = "Define your own active and blackout days.",
        activeDays = 30,
        blackoutDays = 15
    ),
    CONTINUOUS(
        displayName = "Continuous (Standard)",
        description = "Always active without blackout periods.",
        activeDays = 30,
        blackoutDays = 0
    )
}

enum class SubscriptionCategory(val displayName: String, val iconName: String) {
    STREAMING("Streaming Video", "Tv"),
    MUSIC("Music & Audio", "Music"),
    GAMING("Gaming", "Gamepad"),
    AI_TOOLS("AI & Software", "Cpu"),
    CLOUD("Cloud & Storage", "Cloud"),
    FITNESS("Fitness & Health", "Activity"),
    NEWS("News & Reading", "BookOpen"),
    OTHER("Other", "Layers")
}

enum class BillingPeriod(val displayName: String, val shortSuffix: String) {
    MONTHLY("Monthly", "/mo"),
    YEARLY("Yearly", "/yr")
}

enum class SetupMissingItem(val label: String) {
    PRICE("Price"),
    BILLING_DATE("Billing date"),
    CYCLE("Cycle strategy")
}

data class Subscription(
    val id: Long = 0,
    val name: String,
    val cost: Double,
    val currency: String = "USD",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val category: SubscriptionCategory = SubscriptionCategory.STREAMING,
    val startDate: LocalDate,
    val strategy: RotationStrategy = RotationStrategy.ROTATION_45_DAYS,
    val customActiveDays: Int = 30,
    val customBlackoutDays: Int = 15,
    val colorHex: String = "#10B981",
    val iconName: String = "Tv",
    val notes: String = "",
    val isAutoRenewCancelled: Boolean = false,
    val pairedWithSubscriptionId: Long? = null,
    val isBillingDateConfirmed: Boolean = true,
    val isStrategyConfirmed: Boolean = true,
    val cancelledCycleIndex: Int? = null,
    val completedBreaksCount: Int = 0,
    val earnedSavingsTotal: Double = 0.0,
    val bonusActiveDays: Int = 0
) {
    val activeDays: Int
        get() = if (strategy == RotationStrategy.CUSTOM) customActiveDays.coerceAtLeast(1) else strategy.activeDays

    val blackoutDays: Int
        get() = if (strategy == RotationStrategy.CUSTOM) customBlackoutDays.coerceAtLeast(0) else strategy.blackoutDays

    val totalCycleDays: Int
        get() = activeDays + blackoutDays

    val isRotating: Boolean
        get() = billingPeriod == BillingPeriod.MONTHLY &&
            isStrategyConfirmed &&
            strategy != RotationStrategy.CONTINUOUS &&
            blackoutDays > 0

    val missingSetupItems: List<SetupMissingItem>
        get() = buildList {
            if (cost <= 0.0) add(SetupMissingItem.PRICE)
            if (!isBillingDateConfirmed) add(SetupMissingItem.BILLING_DATE)
            if (billingPeriod == BillingPeriod.MONTHLY && !isStrategyConfirmed) add(SetupMissingItem.CYCLE)
        }

    val isIncomplete: Boolean
        get() = missingSetupItems.isNotEmpty()

    val setupTotalSteps: Int
        get() = if (billingPeriod == BillingPeriod.MONTHLY) 4 else 3

    val setupCompletedSteps: Int
        get() = (setupTotalSteps - missingSetupItems.size).coerceAtLeast(1)

    val setupCompletionFraction: Float
        get() = (setupCompletedSteps.toFloat() / setupTotalSteps.coerceAtLeast(1)).coerceIn(0.25f, 1f)

    val firstMissingSetupStepIndex: Int
        get() = when (missingSetupItems.firstOrNull()) {
            SetupMissingItem.PRICE -> 0
            SetupMissingItem.BILLING_DATE, SetupMissingItem.CYCLE -> 1
            null -> 0
        }

    val monthlyEquivalentCost: Double
        get() = if (billingPeriod == BillingPeriod.YEARLY) cost / 12.0 else cost

    val annualStandardCost: Double
        get() = if (billingPeriod == BillingPeriod.YEARLY) cost else cost * 12.0
}

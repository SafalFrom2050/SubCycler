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

data class Subscription(
    val id: Long = 0,
    val name: String,
    val cost: Double,
    val currency: String = "USD",
    val category: SubscriptionCategory = SubscriptionCategory.STREAMING,
    val startDate: LocalDate,
    val strategy: RotationStrategy = RotationStrategy.ROTATION_45_DAYS,
    val customActiveDays: Int = 30,
    val customBlackoutDays: Int = 15,
    val colorHex: String = "#10B981",
    val iconName: String = "Tv",
    val notes: String = "",
    val isAutoRenewCancelled: Boolean = false,
    val pairedWithSubscriptionId: Long? = null
) {
    val activeDays: Int
        get() = if (strategy == RotationStrategy.CUSTOM) customActiveDays else strategy.activeDays

    val blackoutDays: Int
        get() = if (strategy == RotationStrategy.CUSTOM) customBlackoutDays else strategy.blackoutDays

    val totalCycleDays: Int
        get() = activeDays + blackoutDays
}

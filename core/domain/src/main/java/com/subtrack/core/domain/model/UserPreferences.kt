package com.subtrack.core.domain.model

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserPreferences(
    val currency: String = "USD",
    val notificationLeadDays: Int = 3,
    val isAutoCancelApprovalRequired: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val pendingSubscriptionAction: PendingSubscriptionAction? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

package com.subtrack.core.domain.model

data class UserPreferences(
    val currency: String = "USD",
    val notificationLeadDays: Int = 3,
    val isAutoCancelApprovalRequired: Boolean = true,
    val isOnboardingCompleted: Boolean = false
)

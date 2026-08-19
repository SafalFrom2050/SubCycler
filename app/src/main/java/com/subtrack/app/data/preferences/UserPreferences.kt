package com.subtrack.app.data.preferences

data class UserPreferences(
    val currency: String = "USD",
    val notificationLeadDays: Int = 3,
    val isAutoCancelApprovalRequired: Boolean = true,
    val isDarkTheme: Boolean? = null // null means system default
)

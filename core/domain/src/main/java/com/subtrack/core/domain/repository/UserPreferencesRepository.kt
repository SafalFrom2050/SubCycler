package com.subtrack.core.domain.repository

import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setCurrency(currency: String)
    suspend fun setNotificationLeadDays(days: Int)
    suspend fun setAutoCancelApprovalRequired(required: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setPendingSubscriptionAction(action: PendingSubscriptionAction?) {}
    suspend fun setThemeMode(themeMode: AppThemeMode) {}
}

package com.subtrack.core.domain.testing

import com.subtrack.core.domain.model.UserPreferences
import com.subtrack.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences()
) : UserPreferencesRepository {

    private val preferencesFlow = MutableStateFlow(initialPreferences)

    override val userPreferences: Flow<UserPreferences> = preferencesFlow.asStateFlow()

    override suspend fun setCurrency(currency: String) {
        preferencesFlow.value = preferencesFlow.value.copy(currency = currency)
    }

    override suspend fun setNotificationLeadDays(days: Int) {
        preferencesFlow.value = preferencesFlow.value.copy(notificationLeadDays = days)
    }

    override suspend fun setAutoCancelApprovalRequired(required: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(isAutoCancelApprovalRequired = required)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(isOnboardingCompleted = completed)
    }
}

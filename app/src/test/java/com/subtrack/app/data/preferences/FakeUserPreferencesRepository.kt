package com.subtrack.app.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences()
) : UserPreferencesRepository {

    private val _preferencesFlow = MutableStateFlow(initialPreferences)
    override val userPreferences: Flow<UserPreferences> = _preferencesFlow.asStateFlow()

    override suspend fun setCurrency(currency: String) {
        _preferencesFlow.value = _preferencesFlow.value.copy(currency = currency)
    }

    override suspend fun setNotificationLeadDays(days: Int) {
        _preferencesFlow.value = _preferencesFlow.value.copy(notificationLeadDays = days)
    }

    override suspend fun setAutoCancelApprovalRequired(required: Boolean) {
        _preferencesFlow.value = _preferencesFlow.value.copy(isAutoCancelApprovalRequired = required)
    }

    override suspend fun setDarkTheme(isDark: Boolean?) {
        _preferencesFlow.value = _preferencesFlow.value.copy(isDarkTheme = isDark)
    }
}

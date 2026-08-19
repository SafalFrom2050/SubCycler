package com.subtrack.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setCurrency(currency: String)
    suspend fun setNotificationLeadDays(days: Int)
    suspend fun setAutoCancelApprovalRequired(required: Boolean)
    suspend fun setDarkTheme(isDark: Boolean?)
}

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val NOTIFICATION_LEAD_DAYS = intPreferencesKey("notification_lead_days")
        val AUTO_CANCEL_APPROVAL = booleanPreferencesKey("auto_cancel_approval")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                currency = preferences[PreferencesKeys.CURRENCY] ?: "USD",
                notificationLeadDays = preferences[PreferencesKeys.NOTIFICATION_LEAD_DAYS] ?: 3,
                isAutoCancelApprovalRequired = preferences[PreferencesKeys.AUTO_CANCEL_APPROVAL] ?: true,
                isDarkTheme = preferences[PreferencesKeys.IS_DARK_THEME]
            )
        }

    override suspend fun setCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    override suspend fun setNotificationLeadDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_LEAD_DAYS] = days
        }
    }

    override suspend fun setAutoCancelApprovalRequired(required: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CANCEL_APPROVAL] = required
        }
    }

    override suspend fun setDarkTheme(isDark: Boolean?) {
        dataStore.edit { preferences ->
            if (isDark == null) {
                preferences.remove(PreferencesKeys.IS_DARK_THEME)
            } else {
                preferences[PreferencesKeys.IS_DARK_THEME] = isDark
            }
        }
    }
}

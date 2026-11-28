package com.subtrack.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.model.UserPreferences
import com.subtrack.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val defaultCurrencyCode: String
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val NOTIFICATION_LEAD_DAYS = intPreferencesKey("notification_lead_days")
        val AUTO_CANCEL_APPROVAL = booleanPreferencesKey("auto_cancel_approval")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_STACK_HERO_MOOD = booleanPreferencesKey("use_stack_hero_mood")
        val LAUNCHER_ICON = stringPreferencesKey("launcher_icon")
        val PENDING_ACTION_SUB_ID = longPreferencesKey("pending_action_subscription_id")
        val PENDING_ACTION_SUB_NAME = stringPreferencesKey("pending_action_subscription_name")
        val PENDING_ACTION_TYPE = stringPreferencesKey("pending_action_type")
        val PENDING_ACTION_STARTED_AT = longPreferencesKey("pending_action_started_at")
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
            val pendingSubId = preferences[PreferencesKeys.PENDING_ACTION_SUB_ID]
            val pendingSubName = preferences[PreferencesKeys.PENDING_ACTION_SUB_NAME]
            val pendingTypeRaw = preferences[PreferencesKeys.PENDING_ACTION_TYPE]
            val pendingStartedAt = preferences[PreferencesKeys.PENDING_ACTION_STARTED_AT] ?: 0L
            val pendingAction = if (pendingSubId != null && !pendingSubName.isNullOrBlank() && !pendingTypeRaw.isNullOrBlank()) {
                runCatching { SubscriptionActionProgressType.valueOf(pendingTypeRaw) }
                    .getOrNull()
                    ?.let { actionType ->
                        PendingSubscriptionAction(
                            subscriptionId = pendingSubId,
                            subscriptionName = pendingSubName,
                            actionType = actionType,
                            startedAtEpochMillis = pendingStartedAt
                        )
                    }
            } else {
                null
            }

            val themeMode = preferences[PreferencesKeys.THEME_MODE]
                ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM

            val launcherIcon = preferences[PreferencesKeys.LAUNCHER_ICON]
                ?.let { runCatching { AppLauncherIcon.valueOf(it) }.getOrNull() }
                ?: AppLauncherIcon.BOTANICAL

            UserPreferences(
                currency = preferences[PreferencesKeys.CURRENCY] ?: defaultCurrencyCode,
                notificationLeadDays = preferences[PreferencesKeys.NOTIFICATION_LEAD_DAYS] ?: 3,
                isAutoCancelApprovalRequired = preferences[PreferencesKeys.AUTO_CANCEL_APPROVAL] ?: true,
                isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                pendingSubscriptionAction = pendingAction,
                themeMode = themeMode,
                useStackHeroMood = preferences[PreferencesKeys.USE_STACK_HERO_MOOD] ?: true,
                launcherIcon = launcherIcon
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

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setPendingSubscriptionAction(action: PendingSubscriptionAction?) {
        dataStore.edit { preferences ->
            if (action == null) {
                preferences.remove(PreferencesKeys.PENDING_ACTION_SUB_ID)
                preferences.remove(PreferencesKeys.PENDING_ACTION_SUB_NAME)
                preferences.remove(PreferencesKeys.PENDING_ACTION_TYPE)
                preferences.remove(PreferencesKeys.PENDING_ACTION_STARTED_AT)
            } else {
                preferences[PreferencesKeys.PENDING_ACTION_SUB_ID] = action.subscriptionId
                preferences[PreferencesKeys.PENDING_ACTION_SUB_NAME] = action.subscriptionName
                preferences[PreferencesKeys.PENDING_ACTION_TYPE] = action.actionType.name
                preferences[PreferencesKeys.PENDING_ACTION_STARTED_AT] = action.startedAtEpochMillis
            }
        }
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setUseStackHeroMood(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_STACK_HERO_MOOD] = enabled
        }
    }

    override suspend fun setLauncherIcon(icon: AppLauncherIcon) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAUNCHER_ICON] = icon.name
        }
    }
}

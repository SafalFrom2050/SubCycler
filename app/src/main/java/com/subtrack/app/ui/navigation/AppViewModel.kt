package com.subtrack.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppLaunchState {
    LOADING,
    ONBOARDING,
    MAIN_CONTENT
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLaunchConfig: AppLaunchConfig
) : ViewModel() {

    private val _launchState = MutableStateFlow(AppLaunchState.LOADING)
    val launchState: StateFlow<AppLaunchState> = _launchState.asStateFlow()

    val themeMode: StateFlow<AppThemeMode> = userPreferencesRepository.userPreferences
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeMode.SYSTEM
        )

    init {
        resolveLaunchState()
    }

    fun onOnboardingFinished() {
        _launchState.value = AppLaunchState.MAIN_CONTENT
    }

    fun openOnboarding() {
        _launchState.value = AppLaunchState.ONBOARDING
    }

    private fun resolveLaunchState() {
        if (appLaunchConfig.forceOnboardingOnEveryLaunch) {
            _launchState.value = AppLaunchState.ONBOARDING
            return
        }

        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.userPreferences.first()
                if (preferences.isOnboardingCompleted) {
                    _launchState.value = AppLaunchState.MAIN_CONTENT
                    return@launch
                }

                // Compatibility path for existing installations created before onboarding existed.
                val existingSubscriptions = subscriptionRepository.getAllSubscriptionsList()
                if (existingSubscriptions.isNotEmpty()) {
                    userPreferencesRepository.setOnboardingCompleted(true)
                    _launchState.value = AppLaunchState.MAIN_CONTENT
                } else {
                    _launchState.value = AppLaunchState.ONBOARDING
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _launchState.value = AppLaunchState.ONBOARDING
            }
        }
    }
}

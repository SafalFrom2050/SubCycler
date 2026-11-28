package com.subtrack.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AppLaunchState {
    LOADING,
    ONBOARDING,
    MAIN_CONTENT
}

data class DockMoodState(
    val activeSubscriptions: List<Subscription> = emptyList(),
    val useStackHeroMood: Boolean = true,
    val launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
    val isLoaded: Boolean = false
)

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

    val launcherIcon: StateFlow<AppLauncherIcon> = userPreferencesRepository.userPreferences
        .map { it.launcherIcon }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLauncherIcon.BOTANICAL
        )

    val dockMood: StateFlow<DockMoodState> = combine(
        subscriptionRepository.getAllSubscriptions(),
        userPreferencesRepository.userPreferences
    ) { subscriptions, preferences ->
        val today = LocalDate.now()
        val active = subscriptions.filter { sub ->
            !sub.isIncomplete && RotationEngine.calculateCycleStatus(sub, today).phase == CyclePhase.ACTIVE
        }
        DockMoodState(
            activeSubscriptions = active,
            useStackHeroMood = preferences.useStackHeroMood,
            launcherIcon = preferences.launcherIcon,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DockMoodState()
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

    fun setLauncherIcon(icon: AppLauncherIcon) {
        viewModelScope.launch {
            userPreferencesRepository.setLauncherIcon(icon)
        }
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

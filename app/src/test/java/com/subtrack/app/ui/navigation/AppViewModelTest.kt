package com.subtrack.app.ui.navigation

import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.UserPreferences
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import com.subtrack.core.domain.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyFreshInstallShowsOnboarding() = runTest(dispatcher) {
        val viewModel = AppViewModel(
            subscriptionRepository = FakeSubscriptionRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            appLaunchConfig = AppLaunchConfig(forceOnboardingOnEveryLaunch = false)
        )

        advanceUntilIdle()

        assertEquals(AppLaunchState.ONBOARDING, viewModel.launchState.value)
    }

    @Test
    fun completedInstallShowsMainContent() = runTest(dispatcher) {
        val viewModel = AppViewModel(
            subscriptionRepository = FakeSubscriptionRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(
                initialPreferences = UserPreferences(isOnboardingCompleted = true)
            ),
            appLaunchConfig = AppLaunchConfig(forceOnboardingOnEveryLaunch = false)
        )

        advanceUntilIdle()

        assertEquals(AppLaunchState.MAIN_CONTENT, viewModel.launchState.value)
    }

    @Test
    fun legacyInstallWithSubscriptionsIsMigratedPastOnboarding() = runTest(dispatcher) {
        val subscriptions = FakeSubscriptionRepository().apply {
            setSubscriptions(
                listOf(
                    Subscription(
                        name = "Existing service",
                        cost = 9.99,
                        startDate = LocalDate.of(2026, 8, 1)
                    )
                )
            )
        }
        val preferences = FakeUserPreferencesRepository()
        val viewModel = AppViewModel(
            subscriptionRepository = subscriptions,
            userPreferencesRepository = preferences,
            appLaunchConfig = AppLaunchConfig(forceOnboardingOnEveryLaunch = false)
        )

        advanceUntilIdle()

        assertEquals(AppLaunchState.MAIN_CONTENT, viewModel.launchState.value)
        assertTrue(preferences.userPreferences.first().isOnboardingCompleted)
    }

    @Test
    fun testingOverrideShowsOnboardingForCompletedPopulatedInstall() = runTest(dispatcher) {
        val subscriptions = FakeSubscriptionRepository().apply {
            setSubscriptions(
                listOf(
                    Subscription(
                        name = "Existing service",
                        cost = 9.99,
                        startDate = LocalDate.of(2026, 8, 1)
                    )
                )
            )
        }
        val viewModel = AppViewModel(
            subscriptionRepository = subscriptions,
            userPreferencesRepository = FakeUserPreferencesRepository(
                initialPreferences = UserPreferences(isOnboardingCompleted = true)
            ),
            appLaunchConfig = AppLaunchConfig(forceOnboardingOnEveryLaunch = true)
        )

        assertEquals(AppLaunchState.ONBOARDING, viewModel.launchState.value)

        viewModel.onOnboardingFinished()

        assertEquals(AppLaunchState.MAIN_CONTENT, viewModel.launchState.value)
    }

    @Test
    fun navigationItemsUseCompactSubsPlannerSavingsOrder() {
        assertEquals(
            listOf("Subs", "Planner", "Savings"),
            NavigationItems.items.map { it.title }
        )
    }

    @Test
    fun dockMoodTracksActiveSubscriptionsAndPreferenceToggle() = runTest(dispatcher) {
        val subscriptions = FakeSubscriptionRepository().apply {
            setSubscriptions(
                listOf(
                    Subscription(
                        id = 1L,
                        name = "Netflix",
                        cost = 15.49,
                        startDate = LocalDate.now(),
                        isBillingDateConfirmed = true,
                        isStrategyConfirmed = true
                    ),
                    Subscription(
                        id = 2L,
                        name = "Incomplete Draft",
                        cost = 0.0,
                        startDate = LocalDate.now(),
                        isBillingDateConfirmed = false,
                        isStrategyConfirmed = false
                    )
                )
            )
        }
        val preferences = FakeUserPreferencesRepository(
            initialPreferences = UserPreferences(
                isOnboardingCompleted = true,
                useStackHeroMood = true
            )
        )
        val viewModel = AppViewModel(
            subscriptionRepository = subscriptions,
            userPreferencesRepository = preferences,
            appLaunchConfig = AppLaunchConfig(forceOnboardingOnEveryLaunch = false)
        )

        val state = viewModel.dockMood.first { it.activeSubscriptions.isNotEmpty() }
        assertTrue(state.useStackHeroMood)
        assertEquals(listOf("Netflix"), state.activeSubscriptions.map { it.name })
    }

    @Test
    fun setLauncherIconPersistsSyncedIconToPreferences() = runTest(dispatcher) {
        val preferences = FakeUserPreferencesRepository(
            initialPreferences = UserPreferences(
                isOnboardingCompleted = true,
                useStackHeroMood = true
            )
        )
        val viewModel = AppViewModel(
            subscriptionRepository = FakeSubscriptionRepository(),
            userPreferencesRepository = preferences,
            appLaunchConfig = AppLaunchConfig(forceOnboardingOnEveryLaunch = false)
        )

        viewModel.setLauncherIcon(com.subtrack.core.domain.model.AppLauncherIcon.CINEMA)
        advanceUntilIdle()

        assertEquals(
            com.subtrack.core.domain.model.AppLauncherIcon.CINEMA,
            preferences.userPreferences.first().launcherIcon
        )
    }
}

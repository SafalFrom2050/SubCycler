package com.subtrack.feature.onboarding

import com.subtrack.core.common.time.FakeTimeProvider
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.SubscriptionCategory
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import com.subtrack.core.domain.testing.FakeUserPreferencesRepository
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var preferencesRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        subscriptionRepository = FakeSubscriptionRepository()
        preferencesRepository = FakeUserPreferencesRepository()
        viewModel = OnboardingViewModel(
            manageSubscriptionUseCase = ManageSubscriptionUseCase(subscriptionRepository),
            userPreferencesRepository = preferencesRepository,
            timeProvider = FakeTimeProvider(currentDate = LocalDate.of(2026, 8, 30))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsAtWelcomeAndNavigatesToServiceSelectionAndBack() {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)

        viewModel.onAction(OnboardingAction.BeginSelection)
        assertEquals(OnboardingStep.SELECT_SERVICES, viewModel.uiState.value.step)

        viewModel.onAction(OnboardingAction.Back)
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }

    @Test
    fun selectionSearchesAcrossCategoriesAndAdvances() {
        viewModel.onAction(OnboardingAction.SetCategory(OnboardingCategory.VIDEO))
        viewModel.onAction(OnboardingAction.SetSearchQuery("Google One"))

        assertEquals(listOf("Google One"), viewModel.uiState.value.visibleServices.map { it.preset.name })

        viewModel.onAction(OnboardingAction.ToggleService("Google One"))
        viewModel.onAction(OnboardingAction.ContinueToReview)

        assertEquals(OnboardingStep.REVIEW_DETAILS, viewModel.uiState.value.step)
        assertEquals(1, viewModel.uiState.value.selectedServices.size)
    }

    @Test
    fun customServiceIsSelectedAndSavedWithUserEnteredDetails() = runTest(dispatcher) {
        viewModel.onAction(OnboardingAction.SetCurrency("JPY"))
        viewModel.onAction(
            OnboardingAction.AddCustomService(
                serviceName = "My Private VPN",
                category = SubscriptionCategory.OTHER,
                costInput = "475"
            )
        )

        val draft = viewModel.uiState.value.selectedServices.single()
        assertEquals("My Private VPN", draft.preset.name)
        assertEquals("MP", draft.initials)
        assertEquals(SubscriptionCategory.OTHER, draft.preset.category)
        assertEquals(RotationStrategy.CONTINUOUS, draft.preset.suggestedStrategy)

        viewModel.onAction(OnboardingAction.ContinueToReview)
        viewModel.onAction(OnboardingAction.ShowStarterPlan)
        viewModel.onAction(OnboardingAction.CompleteOnboarding)
        advanceUntilIdle()

        val saved = subscriptionRepository.getAllSubscriptionsList().single()
        assertEquals("My Private VPN", saved.name)
        assertEquals(475.0, saved.cost, 0.001)
        assertEquals(SubscriptionCategory.OTHER, saved.category)
        assertEquals("JPY", saved.currency)
        assertEquals("JPY", preferencesRepository.userPreferences.first().currency)
    }

    @Test
    fun uncataloguedCurrencyDoesNotRelabelUsdPresetPrices() = runTest(dispatcher) {
        viewModel.onAction(OnboardingAction.SetCurrency("CHF"))
        advanceUntilIdle()

        assertEquals("CHF", viewModel.uiState.value.currencyCode)
        assertTrue(viewModel.uiState.value.services.all { it.costInput.isBlank() })
        assertEquals("CHF", preferencesRepository.userPreferences.first().currency)
    }

    @Test
    fun regionalCurrencyAutoFillsLocalizedTiersAndSupportsYearlySelection() = runTest(dispatcher) {
        viewModel.onAction(OnboardingAction.SetCurrency("JPY"))
        advanceUntilIdle()

        val prime = viewModel.uiState.value.services.first { it.preset.name == "Amazon Prime Video" }
        assertEquals("600", prime.costInput)
        val yearlyTier = prime.availableTiers.first { it.billingPeriod == com.subtrack.core.domain.model.BillingPeriod.YEARLY }

        viewModel.onAction(OnboardingAction.ToggleService("Amazon Prime Video"))
        viewModel.onAction(OnboardingAction.SelectPlanTier("Amazon Prime Video", yearlyTier))

        val updatedPrime = viewModel.uiState.value.selectedServices.single()
        assertEquals("5900", updatedPrime.costInput)
        assertEquals(com.subtrack.core.domain.model.BillingPeriod.YEARLY, updatedPrime.billingPeriod)
        assertEquals(RotationStrategy.CONTINUOUS, updatedPrime.effectiveStrategy)
    }

    @Test
    fun completeOnboardingBatchAddsSelectedSubscriptions() = runTest(dispatcher) {
        val nextBill = LocalDate.of(2026, 9, 30)

        viewModel.onAction(OnboardingAction.ToggleService("Netflix"))
        viewModel.onAction(OnboardingAction.ToggleService("Spotify"))
        viewModel.onAction(OnboardingAction.ContinueToReview)
        viewModel.onAction(OnboardingAction.UpdateCost("Netflix", "19.99"))
        viewModel.onAction(OnboardingAction.SetNextBillingDate("Netflix", nextBill))
        viewModel.onAction(OnboardingAction.ShowStarterPlan)
        viewModel.onAction(OnboardingAction.CompleteOnboarding)
        advanceUntilIdle()

        val subscriptions = subscriptionRepository.getAllSubscriptionsList()
        assertEquals(2, subscriptions.size)
        assertEquals(19.99, subscriptions.first { it.name == "Netflix" }.cost, 0.001)
        assertEquals(nextBill.minusDays(30), subscriptions.first { it.name == "Netflix" }.startDate)
        assertEquals(LocalDate.of(2026, 8, 30), subscriptions.first { it.name == "Spotify" }.startDate)
        assertTrue(preferencesRepository.userPreferences.first().isOnboardingCompleted)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertTrue(viewModel.uiState.value.shouldRequestNotificationPermission)
    }

    @Test
    fun invalidPriceDoesNotAdvanceToStarterPlan() {
        viewModel.onAction(OnboardingAction.ToggleService("Netflix"))
        viewModel.onAction(OnboardingAction.ContinueToReview)
        viewModel.onAction(OnboardingAction.UpdateCost("Netflix", ""))
        viewModel.onAction(OnboardingAction.ShowStarterPlan)

        assertEquals(OnboardingStep.REVIEW_DETAILS, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.errorMessage.orEmpty().contains("Netflix"))
    }

    @Test
    fun skipCompletesWithoutCreatingSubscriptionsOrRequestingNotifications() = runTest(dispatcher) {
        viewModel.onAction(OnboardingAction.SkipOnboarding)
        advanceUntilIdle()

        assertTrue(preferencesRepository.userPreferences.first().isOnboardingCompleted)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.shouldRequestNotificationPermission)
        assertTrue(subscriptionRepository.getAllSubscriptionsList().isEmpty())
    }
}

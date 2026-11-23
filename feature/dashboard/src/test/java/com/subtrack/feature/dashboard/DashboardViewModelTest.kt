package com.subtrack.feature.dashboard

import com.subtrack.core.common.time.FakeTimeProvider
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.testing.FakeCalendarAwarenessRepository
import com.subtrack.core.domain.testing.FakeCalendarAwarenessScheduler
import com.subtrack.core.domain.testing.FakeSubscriptionActionNotifier
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import com.subtrack.core.domain.testing.FakeUserPreferencesRepository
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeSubscriptionRepository()
    private val fakeTimeProvider = FakeTimeProvider(currentDate = LocalDate.of(2026, 8, 18))
    private val getCycleStatusesUseCase = GetSubscriptionCycleStatusesUseCase(fakeRepository, fakeTimeProvider)
    private val getPortfolioSummaryUseCase = GetPortfolioSummaryUseCase()
    private val manageSubscriptionUseCase = ManageSubscriptionUseCase(fakeRepository)

    private lateinit var viewModel: DashboardViewModel
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var calendarRepository: FakeCalendarAwarenessRepository
    private lateinit var calendarScheduler: FakeCalendarAwarenessScheduler
    private lateinit var actionNotifier: FakeSubscriptionActionNotifier

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val today = LocalDate.of(2026, 8, 18)
        fakeRepository.setSubscriptions(
            listOf(
                Subscription(id = 1, name = "Netflix", cost = 15.49, startDate = today.minusDays(10), strategy = RotationStrategy.ROTATION_45_DAYS),
                Subscription(id = 2, name = "Disney+", cost = 13.99, startDate = today.minusDays(35), strategy = RotationStrategy.ROTATION_45_DAYS)
            )
        )
        userPreferencesRepository = FakeUserPreferencesRepository()
        calendarRepository = FakeCalendarAwarenessRepository()
        calendarScheduler = FakeCalendarAwarenessScheduler()
        actionNotifier = FakeSubscriptionActionNotifier()
        viewModel = DashboardViewModel(
            getCycleStatusesUseCase = getCycleStatusesUseCase,
            getPortfolioSummaryUseCase = getPortfolioSummaryUseCase,
            manageSubscriptionUseCase = manageSubscriptionUseCase,
            userPreferencesRepository = userPreferencesRepository,
            calendarAwarenessRepository = calendarRepository,
            calendarAwarenessScheduler = calendarScheduler,
            timeProvider = fakeTimeProvider,
            defaultDispatcher = testDispatcher,
            subscriptionActionNotifier = actionNotifier
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiStateEmission() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionFilter.ALL, state.selectedFilter)
        assertEquals(2, state.cycleStatuses.size)
        assertEquals(2, state.totalSubscriptionCount)
        assertEquals(2, state.portfolioSummary.totalSubscriptions)
    }

    @Test
    fun testFilterActionUpdatesFilteredStatusesAndPreservesTotalCount() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.SetFilter(SubscriptionFilter.ACTIVE))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionFilter.ACTIVE, state.selectedFilter)
        assertEquals(1, state.cycleStatuses.size)
        assertEquals(2, state.totalSubscriptionCount)
        assertEquals(CyclePhase.ACTIVE, state.cycleStatuses.first().phase)
    }

    @Test
    fun mixedCurrenciesAreNotAddedIntoOnePortfolioTotal() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val today = LocalDate.of(2026, 8, 18)
        fakeRepository.setSubscriptions(
            listOf(
                Subscription(id = 1, name = "Netflix", cost = 15.49, currency = "USD", startDate = today),
                Subscription(id = 2, name = "Music", cost = 1_000.0, currency = "JPY", startDate = today)
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("USD", state.currencyCode)
        assertEquals(1, state.portfolioSummary.totalSubscriptions)
        assertEquals(1, state.excludedCurrencyCount)
    }

    @Test
    fun testAddSubscriptionAction() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val newSub = Subscription(name = "HBO Max", cost = 16.99, startDate = LocalDate.of(2026, 8, 1))
        viewModel.onAction(DashboardAction.AddSubscription(newSub))
        advanceUntilIdle()

        val subs = fakeRepository.getAllSubscriptionsList()
        assertEquals(3, subs.size)
    }

    @Test
    fun testDeleteSubscriptionAction() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val subToDelete = fakeRepository.getSubscriptionById(1L)!!
        viewModel.onAction(DashboardAction.DeleteSubscription(subToDelete))
        advanceUntilIdle()

        val subs = fakeRepository.getAllSubscriptionsList()
        assertEquals(1, subs.size)
        assertEquals(2L, subs.first().id)
    }

    @Test
    fun startingUnsubscribePersistsProgressAndShowsActiveNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(
            DashboardAction.StartSubscriptionActionProgress(
                subscriptionId = 1L,
                subscriptionName = "Netflix",
                actionType = SubscriptionActionProgressType.UNSUBSCRIBE
            )
        )
        advanceUntilIdle()

        val pending = viewModel.uiState.value.pendingSubscriptionAction
        assertNotNull(pending)
        assertEquals(1L, pending!!.subscriptionId)
        assertEquals("Netflix", pending.subscriptionName)
        assertEquals(SubscriptionActionProgressType.UNSUBSCRIBE, pending.actionType)
        assertEquals(pending, actionNotifier.activeNotification)
    }

    @Test
    fun completingPendingUnsubscribeMarksAutoRenewCancelledClearsPersistenceAndDismissesNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(
            DashboardAction.StartSubscriptionActionProgress(
                subscriptionId = 1L,
                subscriptionName = "Netflix",
                actionType = SubscriptionActionProgressType.UNSUBSCRIBE
            )
        )
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.ResolvePendingSubscriptionAction(completed = true))
        advanceUntilIdle()

        val netflix = fakeRepository.getSubscriptionById(1L)!!
        assertTrue(netflix.isAutoRenewCancelled)
        assertNull(viewModel.uiState.value.pendingSubscriptionAction)
        assertNull(userPreferencesRepository.userPreferences.first().pendingSubscriptionAction)
        assertNull(actionNotifier.activeNotification)
        assertEquals(1, actionNotifier.dismissCount)
    }

    @Test
    fun completingPendingSubscribeRestartsActiveCycleTodayAndDismissesNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(
            DashboardAction.StartSubscriptionActionProgress(
                subscriptionId = 2L,
                subscriptionName = "Disney+",
                actionType = SubscriptionActionProgressType.SUBSCRIBE
            )
        )
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.ResolvePendingSubscriptionAction(completed = true))
        advanceUntilIdle()

        val disney = fakeRepository.getSubscriptionById(2L)!!
        assertEquals(fakeTimeProvider.today(), disney.startDate)
        assertFalse(disney.isAutoRenewCancelled)
        assertNull(viewModel.uiState.value.pendingSubscriptionAction)
        assertNull(actionNotifier.activeNotification)
    }

    @Test
    fun cancellingPendingActionLeavesSubscriptionUnchangedAndClearsProgress() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(
            DashboardAction.StartSubscriptionActionProgress(
                subscriptionId = 1L,
                subscriptionName = "Netflix",
                actionType = SubscriptionActionProgressType.UNSUBSCRIBE
            )
        )
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.ResolvePendingSubscriptionAction(completed = false))
        advanceUntilIdle()

        val netflix = fakeRepository.getSubscriptionById(1L)!!
        assertFalse(netflix.isAutoRenewCancelled)
        assertNull(viewModel.uiState.value.pendingSubscriptionAction)
        assertNull(actionNotifier.activeNotification)
    }

    @Test
    fun persistedPendingActionSurvivesToNextAppLaunchForRecoveryPopup() = runTest {
        userPreferencesRepository.setPendingSubscriptionAction(
            PendingSubscriptionAction(
                subscriptionId = 2L,
                subscriptionName = "Disney+",
                actionType = SubscriptionActionProgressType.SUBSCRIBE,
                startedAtEpochMillis = 12345L
            )
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val recovered = viewModel.uiState.value.pendingSubscriptionAction
        assertNotNull(recovered)
        assertEquals(2L, recovered!!.subscriptionId)
        assertEquals(SubscriptionActionProgressType.SUBSCRIBE, recovered.actionType)
    }

    @Test
    fun calendarSetupDoesNotEnableUntilASelectionIsConfirmed() = runTest {
        calendarRepository.setPermissionGranted(true)
        calendarRepository.setAvailableCalendars(
            listOf(DeviceCalendar(7, "Personal", "person@example.com", "Google"))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAction(DashboardAction.BeginCalendarSetup)
        advanceUntilIdle()
        assertEquals(CalendarSetupStep.EXPLAINING, viewModel.uiState.value.calendarSetupStep)
        assertFalse(calendarScheduler.isEnabled)

        viewModel.onAction(DashboardAction.CalendarPermissionResult(true))
        advanceUntilIdle()
        assertEquals(CalendarSetupStep.SELECTING_CALENDARS, viewModel.uiState.value.calendarSetupStep)
        assertFalse(calendarRepository.settings.first().isEnabled)

        viewModel.onAction(DashboardAction.ConfirmCalendarSelection(setOf(7L)))
        advanceUntilIdle()
        assertTrue(calendarRepository.settings.first().isEnabled)
        assertTrue(calendarScheduler.isEnabled)
    }

    @Test
    fun disablingCalendarAwarenessClearsSelectionAndStopsScheduler() = runTest {
        calendarRepository.setPermissionGranted(true)
        calendarRepository.enable(setOf(7L))
        calendarScheduler.enable()

        viewModel.onAction(DashboardAction.DisableCalendarAwareness)
        advanceUntilIdle()

        val settings = calendarRepository.settings.first()
        assertFalse(settings.isEnabled)
        assertTrue(settings.selectedCalendarIds.isEmpty())
        assertFalse(calendarScheduler.isEnabled)
    }

    @Test
    fun setDefaultCurrencyUnifiesAllSubscriptionsWithoutChangingNumericPrices() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.SetDefaultCurrency("JPY"))
        advanceUntilIdle()

        assertEquals("JPY", userPreferencesRepository.userPreferences.first().currency)
        val subs = fakeRepository.getAllSubscriptionsList()
        assertTrue(subs.all { it.currency == "JPY" })
        assertEquals(15.49, subs.first { it.name == "Netflix" }.cost, 0.001)
        assertEquals("JPY", viewModel.uiState.value.currencyCode)
        assertEquals(0, viewModel.uiState.value.excludedCurrencyCount)
    }

    @Test
    fun incompleteSubscriptionIsCountedAndExcludedFromUrgentAlerts() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val today = LocalDate.of(2026, 8, 18)
        fakeRepository.setSubscriptions(
            listOf(
                Subscription(
                    id = 10,
                    name = "Draft Stream",
                    cost = 0.0,
                    startDate = today.minusDays(29),
                    strategy = RotationStrategy.ROTATION_45_DAYS,
                    isBillingDateConfirmed = false,
                    isStrategyConfirmed = false
                )
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.incompleteCount)
        assertTrue(state.urgentStatuses.isEmpty())
    }

    @Test
    fun autoSaveInsertsNewDraftOnceAndUpdatesInPlaceWithoutDuplicates() = runTest {
        val today = LocalDate.of(2026, 8, 18)
        val draftId = viewModel.saveSubscription(
            Subscription(
                name = "Spotify",
                cost = 0.0,
                currency = "JPY",
                startDate = today,
                isBillingDateConfirmed = false,
                isStrategyConfirmed = false
            )
        )
        assertTrue(draftId > 0L)
        assertEquals(3, fakeRepository.getAllSubscriptionsList().size)

        val updatedId = viewModel.saveSubscription(
            Subscription(
                id = draftId,
                name = "Spotify",
                cost = 980.0,
                currency = "JPY",
                startDate = today,
                isBillingDateConfirmed = true,
                isStrategyConfirmed = true
            )
        )
        assertEquals(draftId, updatedId)
        assertEquals(3, fakeRepository.getAllSubscriptionsList().size)
        val saved = fakeRepository.getSubscriptionById(draftId)!!
        assertEquals(980.0, saved.cost, 0.001)
        assertFalse(saved.isIncomplete)
    }
}

package com.subtrack.feature.dashboard

import com.subtrack.core.common.time.FakeTimeProvider
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import com.subtrack.core.domain.testing.FakeUserPreferencesRepository
import com.subtrack.core.domain.testing.FakeCalendarAwarenessRepository
import com.subtrack.core.domain.testing.FakeCalendarAwarenessScheduler
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
    private lateinit var calendarRepository: FakeCalendarAwarenessRepository
    private lateinit var calendarScheduler: FakeCalendarAwarenessScheduler

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
        calendarRepository = FakeCalendarAwarenessRepository()
        calendarScheduler = FakeCalendarAwarenessScheduler()
        viewModel = DashboardViewModel(
            getCycleStatusesUseCase = getCycleStatusesUseCase,
            getPortfolioSummaryUseCase = getPortfolioSummaryUseCase,
            manageSubscriptionUseCase = manageSubscriptionUseCase,
            userPreferencesRepository = FakeUserPreferencesRepository(),
            calendarAwarenessRepository = calendarRepository,
            calendarAwarenessScheduler = calendarScheduler,
            timeProvider = fakeTimeProvider,
            defaultDispatcher = testDispatcher
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
        assertEquals(2, state.portfolioSummary.totalSubscriptions)
    }

    @Test
    fun testFilterActionUpdatesFilteredStatuses() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onAction(DashboardAction.SetFilter(SubscriptionFilter.ACTIVE))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionFilter.ACTIVE, state.selectedFilter)
        assertEquals(1, state.cycleStatuses.size)
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
}

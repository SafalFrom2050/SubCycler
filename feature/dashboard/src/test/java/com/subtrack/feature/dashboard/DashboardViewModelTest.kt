package com.subtrack.feature.dashboard

import com.subtrack.core.common.time.FakeTimeProvider
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.testing.FakeSubscriptionRepository
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        viewModel = DashboardViewModel(
            getCycleStatusesUseCase = getCycleStatusesUseCase,
            getPortfolioSummaryUseCase = getPortfolioSummaryUseCase,
            manageSubscriptionUseCase = manageSubscriptionUseCase,
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
}

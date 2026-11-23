package com.subtrack.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.dispatcher.DefaultDispatcher
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.AlignPairedSubscriptionsUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class PlannerTab(val displayName: String) {
    TIMELINE("Cycle Timeline"),
    PAIRED_SWAP("Paired Swap")
}

data class PlannerUiState(
    val selectedTab: PlannerTab = PlannerTab.TIMELINE,
    val cycleStatuses: List<CycleStatus> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val selectedTargetDate: LocalDate = LocalDate.now(),
    val activeOnSelectedDate: List<String> = emptyList(),
    val blackoutOnSelectedDate: List<String> = emptyList(),
    val monthlySubscriptions: List<Subscription> = emptyList(),
    val service1: Subscription? = null,
    val service2: Subscription? = null,
    val combinedMonthlyCostBefore: Double = 0.0,
    val combinedMonthlyCostAfter: Double = 0.0,
    val combinedAnnualSavings: Double = 0.0,
    val currencyCode: String = "USD",
    val hasMixedCurrencies: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface PlannerAction {
    data class SelectTab(val tab: PlannerTab) : PlannerAction
    data class SetSelectedDate(val date: LocalDate) : PlannerAction
    data class StepDate(val days: Long) : PlannerAction
    data object ResetToToday : PlannerAction
    data class SelectService1(val id: Long) : PlannerAction
    data class SelectService2(val id: Long) : PlannerAction
    data object SwapServiceOrder : PlannerAction
    data object AlignAndActivate : PlannerAction
    data object RefreshToday : PlannerAction
}

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val alignPairedSubscriptionsUseCase: AlignPairedSubscriptionsUseCase,
    private val timeProvider: TimeProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PlannerTab.TIMELINE)
    private val _selectedTargetDate = MutableStateFlow(timeProvider.today())
    val selectedTargetDate: StateFlow<LocalDate> = _selectedTargetDate
    private val _selectedId1 = MutableStateFlow<Long?>(null)
    private val _selectedId2 = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PlannerUiState> = combine(
        getCycleStatusesUseCase(),
        _selectedTab,
        _selectedTargetDate,
        combine(_selectedId1, _selectedId2) { id1, id2 -> id1 to id2 },
        userPreferencesRepository.userPreferences
    ) { statuses, tab, targetDate, pairedIds, preferences ->
        val today = timeProvider.today()
        val activeList = mutableListOf<String>()
        val blackoutList = mutableListOf<String>()

        statuses.forEach { cycleStatus ->
            val statusAtDate = RotationEngine.calculateCycleStatus(cycleStatus.subscription, targetDate)
            if (statusAtDate.phase == CyclePhase.ACTIVE) {
                activeList.add(cycleStatus.subscription.name)
            } else {
                blackoutList.add(cycleStatus.subscription.name)
            }
        }

        // Only monthly subscriptions can alternate on a 30d/30d paired swap (exclude yearly plans!)
        val monthlySubs = statuses.map { it.subscription }.filter { it.billingPeriod == BillingPeriod.MONTHLY }
        val (id1, id2) = pairedIds
        val s1 = monthlySubs.find { it.id == id1 } ?: monthlySubs.firstOrNull()
        val s2 = monthlySubs.find { it.id == id2 && it.id != s1?.id } ?: monthlySubs.firstOrNull { it.id != s1?.id }

        val hasMixedCurrencies = s1 != null && s2 != null && s1.currency != s2.currency
        val costBefore = if (hasMixedCurrencies) 0.0 else (s1?.monthlyEquivalentCost ?: 0.0) + (s2?.monthlyEquivalentCost ?: 0.0)
        // Use exact RotationEngine 60-day ratio (365/60 billings/yr) so Planner, Dashboard, and Savings match to the cent.
        val annualSavings = if (!hasMixedCurrencies && s1 != null && s2 != null) {
            (costBefore * 12.0) * RotationEngine.ROTATION_60_SAVINGS_RATIO
        } else {
            0.0
        }
        val costAfter = if (!hasMixedCurrencies && s1 != null && s2 != null) {
            costBefore * (1.0 - RotationEngine.ROTATION_60_SAVINGS_RATIO)
        } else {
            costBefore
        }

        PlannerUiState(
            selectedTab = tab,
            cycleStatuses = statuses,
            today = today,
            selectedTargetDate = targetDate,
            activeOnSelectedDate = activeList,
            blackoutOnSelectedDate = blackoutList,
            monthlySubscriptions = monthlySubs,
            service1 = s1,
            service2 = s2,
            combinedMonthlyCostBefore = costBefore,
            combinedMonthlyCostAfter = costAfter,
            combinedAnnualSavings = annualSavings,
            currencyCode = s1?.currency ?: s2?.currency ?: preferences.currency,
            hasMixedCurrencies = hasMixedCurrencies,
            isLoading = false
        )
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState(
            today = timeProvider.today(),
            selectedTargetDate = timeProvider.today()
        )
    )

    fun onAction(action: PlannerAction) {
        when (action) {
            is PlannerAction.SelectTab -> _selectedTab.value = action.tab
            is PlannerAction.SetSelectedDate -> _selectedTargetDate.value = action.date
            is PlannerAction.StepDate -> _selectedTargetDate.value = _selectedTargetDate.value.plusDays(action.days)
            PlannerAction.ResetToToday -> _selectedTargetDate.value = timeProvider.today()
            is PlannerAction.SelectService1 -> _selectedId1.value = action.id
            is PlannerAction.SelectService2 -> _selectedId2.value = action.id
            PlannerAction.SwapServiceOrder -> {
                val s1 = uiState.value.service1 ?: return
                val s2 = uiState.value.service2 ?: return
                _selectedId1.value = s2.id
                _selectedId2.value = s1.id
            }
            PlannerAction.RefreshToday -> getCycleStatusesUseCase.refreshToday()
            PlannerAction.AlignAndActivate -> {
                val s1 = uiState.value.service1 ?: return
                val s2 = uiState.value.service2 ?: return
                viewModelScope.launch {
                    alignPairedSubscriptionsUseCase(s1.id, s2.id)
                }
            }
        }
    }
}

package com.subtrack.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.dispatcher.DefaultDispatcher
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AnalyticsUiState(
    val summary: PortfolioSummary = PortfolioSummary(
        totalSubscriptions = 0,
        activeCount = 0,
        blackoutCount = 0,
        totalStandardAnnualCost = 0.0,
        totalOptimizedAnnualCost = 0.0,
        totalAnnualSavings = 0.0,
        totalMonthlyStandardCost = 0.0,
        totalMonthlyOptimizedCost = 0.0,
        totalMonthlySavings = 0.0,
        overallSavingsPercentage = 0.0,
        urgentActionsCount = 0
    ),
    val threeYearSavings: Double = 0.0,
    val fiveYearSavings: Double = 0.0,
    val currencyCode: String = "USD",
    val excludedCurrencyCount: Int = 0,
    val simulatedRotationPct: Float = 0.75f, // Slider 0% to 100%
    val simulatedAnnualSavings: Double = 0.0,
    val isLoading: Boolean = true
)

sealed interface AnalyticsAction {
    data class SetSimulatedRotationPct(val pct: Float) : AnalyticsAction
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val getPortfolioSummaryUseCase: GetPortfolioSummaryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _simulatedRotationPct = MutableStateFlow(0.75f)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getCycleStatusesUseCase(),
        _simulatedRotationPct,
        userPreferencesRepository.userPreferences
    ) { statuses, simPct, preferences ->
        val currencies = statuses.map { it.subscription.currency }.distinct()
        val summaryCurrencyCode = when {
            currencies.isEmpty() -> preferences.currency
            currencies.size == 1 -> currencies.single()
            preferences.currency in currencies -> preferences.currency
            else -> currencies.first()
        }
        val summaryStatuses = statuses.filter { it.subscription.currency == summaryCurrencyCode }
        val summary = getPortfolioSummaryUseCase(summaryStatuses)

        // Simulation: If simPct of all subs used 45-day rotation (saving 33.3%)
        val totalStandardAnnual = summaryStatuses.sumOf { it.subscription.cost * 12.0 }
        val simulatedAnnualSavings = totalStandardAnnual * simPct * (15.0 / 45.0)

        AnalyticsUiState(
            summary = summary,
            threeYearSavings = summary.totalAnnualSavings * 3.0,
            fiveYearSavings = summary.totalAnnualSavings * 5.0,
            currencyCode = summaryCurrencyCode,
            excludedCurrencyCount = statuses.size - summaryStatuses.size,
            simulatedRotationPct = simPct,
            simulatedAnnualSavings = simulatedAnnualSavings,
            isLoading = false
        )
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onAction(action: AnalyticsAction) {
        when (action) {
            is AnalyticsAction.SetSimulatedRotationPct -> _simulatedRotationPct.value = action.pct
        }
    }
}

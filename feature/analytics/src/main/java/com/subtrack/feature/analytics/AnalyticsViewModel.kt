package com.subtrack.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.dispatcher.DefaultDispatcher
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RotationOpportunity(
    val subscription: Subscription,
    val estimatedAnnualSavings: Double
)

data class SavingsContribution(
    val subscription: Subscription,
    val annualSavings: Double,
    val shareFraction: Float
)

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
    val totalEarnedSavings: Double = 0.0,
    val totalCompletedBreaks: Int = 0,
    val threeYearSavings: Double = 0.0,
    val fiveYearSavings: Double = 0.0,
    val currencyCode: String = "USD",
    val excludedCurrencyCount: Int = 0,
    val simulatedRotationPct: Float = 0.75f,
    val simulatedAnnualSavings: Double = 0.0,
    val additionalPotentialAnnualSavings: Double = 0.0,
    val opportunities: List<RotationOpportunity> = emptyList(),
    val rotatingContributions: List<SavingsContribution> = emptyList(),
    val monthlySubscriptionCount: Int = 0,
    val isLoading: Boolean = true
)

sealed interface AnalyticsAction {
    data class SetSimulatedRotationPct(val pct: Float) : AnalyticsAction
    data class SwitchToSmartRotation(val subscriptionId: Long) : AnalyticsAction
    data object RefreshToday : AnalyticsAction
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val getPortfolioSummaryUseCase: GetPortfolioSummaryUseCase,
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
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

        val totalEarned = summaryStatuses.sumOf { it.subscription.earnedSavingsTotal }
        val completedBreaks = summaryStatuses.sumOf { it.subscription.completedBreaksCount }

        // Actionable opportunities: complete monthly continuous plans with a price set
        val opportunities = summaryStatuses
            .map { it.subscription }
            .filter { !it.isIncomplete && it.cost > 0.0 && it.billingPeriod == BillingPeriod.MONTHLY && !it.isRotating }
            .map { sub ->
                RotationOpportunity(
                    subscription = sub,
                    estimatedAnnualSavings = sub.annualStandardCost * RotationEngine.ROTATION_45_SAVINGS_RATIO
                )
            }
            .sortedByDescending { it.estimatedAnnualSavings }

        // Exact, contradiction-free simulator: current real savings + simPct of continuous monthly upside
        val maxContinuousUpside = opportunities.sumOf { it.estimatedAnnualSavings }
        val additionalUpside = maxContinuousUpside * simPct
        val simulatedTotalAnnual = summary.totalAnnualSavings + additionalUpside

        val monthlyCount = summaryStatuses.count { it.subscription.billingPeriod == BillingPeriod.MONTHLY }
        val totalAnnual = summary.totalAnnualSavings.coerceAtLeast(1.0)
        val contributions = summaryStatuses
            .filter { it.annualSavings > 0.0 && !it.subscription.isIncomplete }
            .sortedByDescending { it.annualSavings }
            .map { status ->
                SavingsContribution(
                    subscription = status.subscription,
                    annualSavings = status.annualSavings,
                    shareFraction = (status.annualSavings / totalAnnual).toFloat().coerceIn(0.08f, 1f)
                )
            }

        AnalyticsUiState(
            summary = summary,
            totalEarnedSavings = totalEarned,
            totalCompletedBreaks = completedBreaks,
            threeYearSavings = summary.totalAnnualSavings * 3.0,
            fiveYearSavings = summary.totalAnnualSavings * 5.0,
            currencyCode = summaryCurrencyCode,
            excludedCurrencyCount = statuses.size - summaryStatuses.size,
            simulatedRotationPct = simPct,
            simulatedAnnualSavings = simulatedTotalAnnual,
            additionalPotentialAnnualSavings = additionalUpside,
            opportunities = opportunities,
            rotatingContributions = contributions,
            monthlySubscriptionCount = monthlyCount,
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
            is AnalyticsAction.SetSimulatedRotationPct -> _simulatedRotationPct.value = action.pct.coerceIn(0f, 1f)
            is AnalyticsAction.SwitchToSmartRotation -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.switchToSmartRotation(action.subscriptionId)
                }
            }
            AnalyticsAction.RefreshToday -> {
                getCycleStatusesUseCase.refreshToday()
            }
        }
    }
}

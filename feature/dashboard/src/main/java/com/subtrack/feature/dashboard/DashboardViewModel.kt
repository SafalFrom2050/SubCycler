package com.subtrack.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.dispatcher.DefaultDispatcher
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAccessState
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarAwarenessSettings
import com.subtrack.core.domain.model.CalendarSuggestionIdentity
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.repository.CalendarAwarenessRepository
import com.subtrack.core.domain.repository.CalendarAwarenessScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val cycleStatuses: List<CycleStatus> = emptyList(),
    val urgentStatuses: List<CycleStatus> = emptyList(),
    val portfolioSummary: PortfolioSummary = PortfolioSummary(
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
    val currencyCode: String = "USD",
    val preferredCurrencyCode: String = "USD",
    val excludedCurrencyCount: Int = 0,
    val selectedFilter: SubscriptionFilter = SubscriptionFilter.ALL,
    val calendarSettings: CalendarAwarenessSettings = CalendarAwarenessSettings(),
    val calendarAccessState: CalendarAccessState = CalendarAccessState.DISABLED,
    val calendarSetupStep: CalendarSetupStep = CalendarSetupStep.IDLE,
    val accessibleCalendars: List<DeviceCalendar> = emptyList(),
    val calendarSelectionSeed: Set<Long> = emptySet(),
    val isCalendarPermissionGranted: Boolean = false,
    val calendarSuggestion: CalendarAwareSuggestion? = null,
    val isLoading: Boolean = true
)

enum class CalendarSetupStep {
    IDLE,
    EXPLAINING,
    LOADING_CALENDARS,
    SELECTING_CALENDARS
}

private data class CalendarConfigurationUiState(
    val step: CalendarSetupStep = CalendarSetupStep.IDLE,
    val calendars: List<DeviceCalendar> = emptyList(),
    val selectionSeed: Set<Long> = emptySet(),
    val permissionGranted: Boolean = false
)

enum class SubscriptionFilter(val displayName: String) {
    ALL("All"),
    ACTIVE("Active Now"),
    BLACKOUT("In Blackout"),
    ROTATING("45d Rotators")
}

sealed interface DashboardAction {
    data class SetFilter(val filter: SubscriptionFilter) : DashboardAction
    data class AddSubscription(val subscription: Subscription) : DashboardAction
    data class UpdateSubscription(val subscription: Subscription) : DashboardAction
    data class DeleteSubscription(val subscription: Subscription) : DashboardAction
    data class ToggleAutoRenew(val subscriptionId: Long) : DashboardAction
    data object BeginCalendarSetup : DashboardAction
    data object CancelCalendarSetup : DashboardAction
    data class CalendarPermissionResult(val granted: Boolean) : DashboardAction
    data object OpenCalendarSelection : DashboardAction
    data class ConfirmCalendarSelection(val calendarIds: Set<Long>) : DashboardAction
    data object DisableCalendarAwareness : DashboardAction
    data class DismissCalendarSuggestion(val identity: CalendarSuggestionIdentity) : DashboardAction
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val getPortfolioSummaryUseCase: GetPortfolioSummaryUseCase,
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calendarAwarenessRepository: CalendarAwarenessRepository,
    private val calendarAwarenessScheduler: CalendarAwarenessScheduler,
    private val timeProvider: TimeProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(SubscriptionFilter.ALL)
    val selectedFilter: StateFlow<SubscriptionFilter> = _selectedFilter
    private val calendarConfiguration = MutableStateFlow(CalendarConfigurationUiState())

    private val dashboardContent = combine(
        getCycleStatusesUseCase(),
        _selectedFilter,
        userPreferencesRepository.userPreferences
    ) { statuses, filter, preferences ->
        val currencies = statuses.map { it.subscription.currency }.distinct()
        val summaryCurrencyCode = when {
            currencies.isEmpty() -> preferences.currency
            currencies.size == 1 -> currencies.single()
            preferences.currency in currencies -> preferences.currency
            else -> currencies.first()
        }
        val summaryStatuses = statuses.filter { it.subscription.currency == summaryCurrencyCode }
        val portfolio = getPortfolioSummaryUseCase(summaryStatuses)
        val urgent = statuses.filter { it.nextAction.urgency != ActionUrgency.INFO }

        val filteredStatuses = when (filter) {
            SubscriptionFilter.ALL -> statuses
            SubscriptionFilter.ACTIVE -> statuses.filter { it.phase == CyclePhase.ACTIVE }
            SubscriptionFilter.BLACKOUT -> statuses.filter { it.phase == CyclePhase.BLACKOUT }
            SubscriptionFilter.ROTATING -> statuses.filter { it.subscription.blackoutDays > 0 }
        }

        DashboardUiState(
            cycleStatuses = filteredStatuses,
            urgentStatuses = urgent,
            portfolioSummary = portfolio,
            currencyCode = summaryCurrencyCode,
            preferredCurrencyCode = preferences.currency,
            excludedCurrencyCount = statuses.size - summaryStatuses.size,
            selectedFilter = filter,
            isLoading = false
        )
    }
    .flowOn(defaultDispatcher)

    val uiState: StateFlow<DashboardUiState> = combine(
        dashboardContent,
        calendarAwarenessRepository.settings,
        calendarAwarenessRepository.suggestions,
        calendarConfiguration
    ) { dashboard, calendarSettings, calendarSuggestions, configuration ->
        val accessState = when {
            configuration.step != CalendarSetupStep.IDLE -> CalendarAccessState.CONFIGURING
            calendarSettings.isEnabled -> CalendarAccessState.ENABLED
            else -> CalendarAccessState.DISABLED
        }
        dashboard.copy(
            calendarSettings = calendarSettings,
            calendarAccessState = accessState,
            calendarSetupStep = configuration.step,
            accessibleCalendars = configuration.calendars,
            calendarSelectionSeed = configuration.selectionSeed,
            isCalendarPermissionGranted = configuration.permissionGranted,
            calendarSuggestion = calendarSuggestions.firstOrNull()
                ?.takeIf { calendarSettings.isEnabled && configuration.permissionGranted }
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.SetFilter -> _selectedFilter.value = action.filter
            is DashboardAction.AddSubscription -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.addSubscription(action.subscription)
                }
            }
            is DashboardAction.UpdateSubscription -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.updateSubscription(action.subscription)
                }
            }
            is DashboardAction.DeleteSubscription -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.deleteSubscription(action.subscription)
                }
            }
            is DashboardAction.ToggleAutoRenew -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.toggleAutoRenew(action.subscriptionId)
                }
            }
            DashboardAction.BeginCalendarSetup -> beginCalendarSetup()
            DashboardAction.CancelCalendarSetup -> cancelCalendarSetup()
            is DashboardAction.CalendarPermissionResult -> handleCalendarPermission(action.granted)
            DashboardAction.OpenCalendarSelection -> openCalendarSelection()
            is DashboardAction.ConfirmCalendarSelection -> confirmCalendarSelection(action.calendarIds)
            DashboardAction.DisableCalendarAwareness -> disableCalendarAwareness()
            is DashboardAction.DismissCalendarSuggestion -> {
                viewModelScope.launch { calendarAwarenessRepository.dismiss(action.identity) }
            }
        }
    }

    /** Called from a STARTED lifecycle scope. OFF maps to an empty flow, so no observer is registered. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    suspend fun monitorCalendarWhileForeground() {
        calendarAwarenessRepository.settings
            .map { it.isEnabled }
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (!enabled) emptyFlow()
                else merge(flowOf(Unit), calendarAwarenessRepository.observeCalendarChanges().debounce(750))
            }
            .collectLatest { refreshCalendarSuggestions() }
    }

    private fun beginCalendarSetup() {
        calendarConfiguration.value = calendarConfiguration.value.copy(
            step = CalendarSetupStep.EXPLAINING,
            calendars = emptyList(),
            selectionSeed = emptySet()
        )
    }

    private fun cancelCalendarSetup() {
        viewModelScope.launch {
            if (!calendarAwarenessRepository.settings.first().isEnabled) {
                calendarAwarenessRepository.disable()
                calendarAwarenessScheduler.disable()
            }
            calendarConfiguration.value = calendarConfiguration.value.copy(
                step = CalendarSetupStep.IDLE,
                calendars = emptyList(),
                selectionSeed = emptySet()
            )
        }
    }

    private fun handleCalendarPermission(granted: Boolean) {
        viewModelScope.launch {
            calendarConfiguration.value = calendarConfiguration.value.copy(permissionGranted = granted)
            if (!granted) {
                calendarAwarenessRepository.disable(CalendarAccessIssue.PERMISSION_DENIED)
                calendarAwarenessScheduler.disable()
                calendarConfiguration.value = calendarConfiguration.value.copy(step = CalendarSetupStep.IDLE)
                return@launch
            }
            loadCalendars(selectionSeed = emptySet())
        }
    }

    private fun openCalendarSelection() {
        viewModelScope.launch {
            if (!calendarAwarenessRepository.isReadPermissionGranted()) {
                calendarAwarenessRepository.disable(CalendarAccessIssue.PERMISSION_REVOKED)
                calendarAwarenessScheduler.disable()
                calendarConfiguration.value = CalendarConfigurationUiState(
                    step = CalendarSetupStep.EXPLAINING,
                    permissionGranted = false
                )
                return@launch
            }
            val current = calendarAwarenessRepository.settings.first()
            calendarConfiguration.value = calendarConfiguration.value.copy(permissionGranted = true)
            loadCalendars(selectionSeed = current.selectedCalendarIds)
        }
    }

    private suspend fun loadCalendars(selectionSeed: Set<Long>) {
        calendarConfiguration.value = calendarConfiguration.value.copy(step = CalendarSetupStep.LOADING_CALENDARS)
        val calendars = runCatching { calendarAwarenessRepository.getAccessibleCalendars() }.getOrDefault(emptyList())
        if (calendars.isEmpty()) {
            calendarAwarenessRepository.disable(CalendarAccessIssue.NO_ACCESSIBLE_CALENDARS)
            calendarAwarenessScheduler.disable()
            calendarConfiguration.value = calendarConfiguration.value.copy(
                step = CalendarSetupStep.IDLE,
                calendars = emptyList(),
                selectionSeed = emptySet()
            )
        } else {
            calendarConfiguration.value = calendarConfiguration.value.copy(
                step = CalendarSetupStep.SELECTING_CALENDARS,
                calendars = calendars,
                selectionSeed = selectionSeed intersect calendars.mapTo(mutableSetOf()) { it.id }
            )
        }
    }

    private fun confirmCalendarSelection(calendarIds: Set<Long>) {
        viewModelScope.launch {
            if (calendarIds.isEmpty()) {
                cancelCalendarSetup()
                return@launch
            }
            val current = calendarAwarenessRepository.settings.first()
            if (current.isEnabled) {
                calendarAwarenessRepository.updateSelection(calendarIds)
            } else {
                calendarAwarenessRepository.enable(calendarIds)
            }
            calendarConfiguration.value = calendarConfiguration.value.copy(
                step = CalendarSetupStep.IDLE,
                calendars = emptyList(),
                selectionSeed = emptySet(),
                permissionGranted = true
            )
            calendarAwarenessScheduler.enable()
            refreshCalendarSuggestions()
        }
    }

    private fun disableCalendarAwareness() {
        viewModelScope.launch {
            calendarAwarenessRepository.disable()
            calendarAwarenessScheduler.disable()
            calendarConfiguration.value = calendarConfiguration.value.copy(
                step = CalendarSetupStep.IDLE,
                calendars = emptyList(),
                selectionSeed = emptySet()
            )
        }
    }

    private suspend fun refreshCalendarSuggestions() {
        val permissionGranted = calendarAwarenessRepository.isReadPermissionGranted()
        calendarConfiguration.value = calendarConfiguration.value.copy(permissionGranted = permissionGranted)
        if (!permissionGranted) {
            calendarAwarenessRepository.disable(CalendarAccessIssue.PERMISSION_REVOKED)
            calendarAwarenessScheduler.disable()
            return
        }
        val statuses = getCycleStatusesUseCase().first()
        calendarAwarenessRepository.refresh(statuses, timeProvider.today())
        if (!calendarAwarenessRepository.settings.first().isEnabled) {
            calendarAwarenessScheduler.disable()
        }
    }
}

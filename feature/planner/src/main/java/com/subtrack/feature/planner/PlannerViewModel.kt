package com.subtrack.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.dispatcher.DefaultDispatcher
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class PlannerUiState(
    val cycleStatuses: List<CycleStatus> = emptyList(),
    val selectedTargetDate: LocalDate = LocalDate.now(),
    val activeOnSelectedDate: List<String> = emptyList(),
    val blackoutOnSelectedDate: List<String> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface PlannerAction {
    data class SetSelectedDate(val date: LocalDate) : PlannerAction
    data class StepDate(val days: Long) : PlannerAction
}

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val timeProvider: TimeProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _selectedTargetDate = MutableStateFlow(timeProvider.today())
    val selectedTargetDate: StateFlow<LocalDate> = _selectedTargetDate

    val uiState: StateFlow<PlannerUiState> = combine(
        getCycleStatusesUseCase(),
        _selectedTargetDate
    ) { statuses, targetDate ->
        // Projections for selected date
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

        PlannerUiState(
            cycleStatuses = statuses,
            selectedTargetDate = targetDate,
            activeOnSelectedDate = activeList,
            blackoutOnSelectedDate = blackoutList,
            isLoading = false
        )
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState(selectedTargetDate = timeProvider.today())
    )

    fun onAction(action: PlannerAction) {
        when (action) {
            is PlannerAction.SetSelectedDate -> _selectedTargetDate.value = action.date
            is PlannerAction.StepDate -> _selectedTargetDate.value = _selectedTargetDate.value.plusDays(action.days)
        }
    }
}

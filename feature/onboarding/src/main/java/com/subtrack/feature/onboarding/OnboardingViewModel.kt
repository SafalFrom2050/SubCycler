package com.subtrack.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.model.RegionalPriceCatalog
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.ServicePlanTier
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import com.subtrack.core.domain.model.SubscriptionPreset
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    SELECT_SERVICES,
    REVIEW_DETAILS,
    STARTER_PLAN
}

enum class OnboardingCategory(
    val displayName: String,
    val subscriptionCategory: SubscriptionCategory? = null
) {
    POPULAR("Popular"),
    VIDEO("Video", SubscriptionCategory.STREAMING),
    MUSIC("Music", SubscriptionCategory.MUSIC),
    SOFTWARE("Software", SubscriptionCategory.AI_TOOLS),
    GAMING("Gaming", SubscriptionCategory.GAMING),
    CLOUD("Cloud", SubscriptionCategory.CLOUD)
}

data class OnboardingServiceDraft(
    val preset: SubscriptionPreset,
    val initials: String,
    val isPopular: Boolean,
    val isCustom: Boolean = false,
    val isSelected: Boolean = false,
    val costInput: String = "",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val availableTiers: List<ServicePlanTier> = emptyList(),
    val selectedTierLabel: String? = null,
    val nextBillingDate: LocalDate? = null,
    val strategyOverride: RotationStrategy? = null
) {
    val parsedCost: Double?
        get() = costInput.toDoubleOrNull()?.takeIf { it > 0.0 }

    val monthlyEquivalentCost: Double
        get() {
            val raw = parsedCost ?: 0.0
            return if (billingPeriod == BillingPeriod.YEARLY) raw / 12.0 else raw
        }

    val effectiveStrategy: RotationStrategy
        get() = when {
            billingPeriod == BillingPeriod.YEARLY -> RotationStrategy.CONTINUOUS
            strategyOverride != null -> strategyOverride
            else -> preset.suggestedStrategy
        }
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val services: List<OnboardingServiceDraft> = OnboardingServiceCatalog.services.forCurrency("USD"),
    val searchQuery: String = "",
    val selectedCategory: OnboardingCategory = OnboardingCategory.POPULAR,
    val currencyCode: String = "USD",
    val remindersEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val shouldRequestNotificationPermission: Boolean = false
) {
    val selectedServices: List<OnboardingServiceDraft>
        get() = services.filter { it.isSelected }

    val visibleServices: List<OnboardingServiceDraft>
        get() {
            val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
            val filtered = services.filter { service ->
                val matchesCategory = if (normalizedQuery.isNotEmpty()) {
                    true
                } else {
                    when (selectedCategory) {
                        OnboardingCategory.POPULAR -> service.isPopular || service.isCustom
                        else -> service.preset.category == selectedCategory.subscriptionCategory
                    }
                }
                val matchesQuery = normalizedQuery.isEmpty() ||
                    service.preset.name.lowercase(Locale.getDefault()).contains(normalizedQuery)
                matchesCategory && matchesQuery
            }
            // Surface user-added custom services first so they are immediately visible when added.
            return if (normalizedQuery.isEmpty() && selectedCategory == OnboardingCategory.POPULAR) {
                filtered.sortedByDescending { it.isCustom }
            } else {
                filtered
            }
        }

    val monthlyCost: Double
        get() = selectedServices.sumOf { it.monthlyEquivalentCost }

    val potentialAnnualSavings: Double
        get() = selectedServices.sumOf { draft ->
            if (draft.effectiveStrategy == RotationStrategy.CONTINUOUS) {
                0.0
            } else {
                val preview = draft.toSubscription(
                    today = LocalDate.of(2026, 1, 1),
                    currency = currencyCode
                )
                RotationEngine.calculateCycleStatus(preview, preview.startDate).annualSavings
            }
        }
}

sealed interface OnboardingAction {
    data object BeginSelection : OnboardingAction
    data class SetSearchQuery(val query: String) : OnboardingAction
    data class SetCategory(val category: OnboardingCategory) : OnboardingAction
    data class SetCurrency(val currencyCode: String) : OnboardingAction
    data class ToggleService(val serviceName: String) : OnboardingAction
    data class AddCustomService(
        val serviceName: String,
        val category: SubscriptionCategory,
        val costInput: String,
        val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY
    ) : OnboardingAction
    data object ContinueToReview : OnboardingAction
    data object Back : OnboardingAction
    data class UpdateCost(val serviceName: String, val costInput: String) : OnboardingAction
    data class SelectPlanTier(val serviceName: String, val tier: ServicePlanTier) : OnboardingAction
    data class SetBillingPeriod(val serviceName: String, val billingPeriod: BillingPeriod) : OnboardingAction
    data class ToggleStrategy(val serviceName: String) : OnboardingAction
    data class SetNextBillingDate(
        val serviceName: String,
        val nextBillingDate: LocalDate?
    ) : OnboardingAction
    data class SetRemindersEnabled(val enabled: Boolean) : OnboardingAction
    data object ShowStarterPlan : OnboardingAction
    data object CompleteOnboarding : OnboardingAction
    data object SkipOnboarding : OnboardingAction
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private var hasExplicitCurrencySelection = false

    init {
        loadPreferredCurrency()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.BeginSelection -> _uiState.update {
                it.copy(step = OnboardingStep.SELECT_SERVICES, errorMessage = null)
            }
            is OnboardingAction.SetSearchQuery -> _uiState.update {
                it.copy(searchQuery = action.query)
            }
            is OnboardingAction.SetCategory -> _uiState.update {
                it.copy(selectedCategory = action.category, errorMessage = null)
            }
            is OnboardingAction.SetCurrency -> setCurrency(action.currencyCode)
            is OnboardingAction.ToggleService -> updateService(action.serviceName) { draft ->
                draft.copy(isSelected = !draft.isSelected)
            }
            is OnboardingAction.AddCustomService -> addCustomService(action)
            OnboardingAction.ContinueToReview -> continueToReview()
            OnboardingAction.Back -> goBack()
            is OnboardingAction.UpdateCost -> updateService(action.serviceName) { draft ->
                draft.copy(costInput = action.costInput, selectedTierLabel = null)
            }
            is OnboardingAction.SelectPlanTier -> updateService(action.serviceName) { draft ->
                val currency = _uiState.value.currencyCode
                draft.copy(
                    costInput = RegionalPriceCatalog.formatEditablePrice(action.tier.price, currency),
                    billingPeriod = action.tier.billingPeriod,
                    selectedTierLabel = action.tier.label
                )
            }
            is OnboardingAction.SetBillingPeriod -> updateService(action.serviceName) { draft ->
                draft.copy(billingPeriod = action.billingPeriod, selectedTierLabel = null)
            }
            is OnboardingAction.ToggleStrategy -> updateService(action.serviceName) { draft ->
                if (draft.billingPeriod == BillingPeriod.YEARLY) {
                    draft
                } else {
                    val nextStrategy = if (draft.effectiveStrategy == RotationStrategy.CONTINUOUS) {
                        RotationStrategy.ROTATION_45_DAYS
                    } else {
                        RotationStrategy.CONTINUOUS
                    }
                    draft.copy(strategyOverride = nextStrategy)
                }
            }
            is OnboardingAction.SetNextBillingDate -> updateService(action.serviceName) { draft ->
                draft.copy(nextBillingDate = action.nextBillingDate)
            }
            is OnboardingAction.SetRemindersEnabled -> _uiState.update {
                it.copy(remindersEnabled = action.enabled)
            }
            OnboardingAction.ShowStarterPlan -> showStarterPlan()
            OnboardingAction.CompleteOnboarding -> completeOnboarding()
            OnboardingAction.SkipOnboarding -> skipOnboarding()
        }
    }

    private fun loadPreferredCurrency() {
        viewModelScope.launch {
            val currencyCode = userPreferencesRepository.userPreferences.first().currency
            if (!hasExplicitCurrencySelection) {
                _uiState.update { state ->
                    state.copy(
                        currencyCode = currencyCode,
                        services = state.services.forCurrency(currencyCode)
                    )
                }
            }
        }
    }

    private fun setCurrency(currencyCode: String) {
        hasExplicitCurrencySelection = true
        val normalizedCode = currencyCode.uppercase(Locale.US)
        _uiState.update { state ->
            state.copy(
                currencyCode = normalizedCode,
                services = state.services.forCurrency(normalizedCode),
                errorMessage = if (state.selectedServices.isNotEmpty()) {
                    "Currency changed to $normalizedCode. Confirm each selected price."
                } else {
                    null
                }
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.setCurrency(normalizedCode)
        }
    }

    private fun addCustomService(action: OnboardingAction.AddCustomService) {
        val serviceName = action.serviceName.trim()
        val cost = action.costInput.toDoubleOrNull()?.takeIf { it > 0.0 }

        if (serviceName.isBlank() || cost == null) {
            _uiState.update {
                it.copy(errorMessage = "Enter a service name and a valid price.")
            }
            return
        }

        _uiState.update { state ->
            val existingService = state.services.firstOrNull {
                it.preset.name.equals(serviceName, ignoreCase = true)
            }
            val updatedServices = if (existingService != null) {
                state.services.map { draft ->
                    if (draft === existingService) {
                        draft.copy(
                            isSelected = true,
                            costInput = action.costInput,
                            billingPeriod = action.billingPeriod,
                            selectedTierLabel = null
                        )
                    } else {
                        draft
                    }
                }
            } else {
                val preset = SubscriptionPreset(
                    name = serviceName,
                    defaultCost = cost,
                    category = action.category,
                    colorHex = "#0C6B4E",
                    iconName = action.category.iconName,
                    suggestedStrategy = defaultCustomStrategy(action.category)
                )
                state.services + OnboardingServiceDraft(
                    preset = preset,
                    initials = initialsForName(serviceName),
                    isPopular = false,
                    isCustom = true,
                    isSelected = true,
                    costInput = action.costInput,
                    billingPeriod = action.billingPeriod
                )
            }

            state.copy(
                services = updatedServices,
                searchQuery = "",
                errorMessage = null
            )
        }
    }

    private fun updateService(
        serviceName: String,
        transform: (OnboardingServiceDraft) -> OnboardingServiceDraft
    ) {
        _uiState.update { state ->
            state.copy(
                services = state.services.map { draft ->
                    if (draft.preset.name == serviceName) transform(draft) else draft
                },
                errorMessage = null
            )
        }
    }

    private fun continueToReview() {
        _uiState.update { state ->
            if (state.selectedServices.isEmpty()) {
                state.copy(errorMessage = "Choose at least one service to continue.")
            } else {
                state.copy(
                    step = OnboardingStep.REVIEW_DETAILS,
                    searchQuery = "",
                    errorMessage = null
                )
            }
        }
    }

    private fun goBack() {
        _uiState.update { state ->
            state.copy(
                step = when (state.step) {
                    OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                    OnboardingStep.SELECT_SERVICES -> OnboardingStep.WELCOME
                    OnboardingStep.REVIEW_DETAILS -> OnboardingStep.SELECT_SERVICES
                    OnboardingStep.STARTER_PLAN -> OnboardingStep.REVIEW_DETAILS
                },
                errorMessage = null
            )
        }
    }

    private fun showStarterPlan() {
        _uiState.update { state ->
            val invalidService = state.selectedServices.firstOrNull { it.parsedCost == null }
            if (invalidService != null) {
                state.copy(errorMessage = "Enter a valid price for ${invalidService.preset.name}.")
            } else {
                state.copy(step = OnboardingStep.STARTER_PLAN, errorMessage = null)
            }
        }
    }

    private fun completeOnboarding() {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val state = _uiState.value
                val currency = state.currencyCode
                val today = timeProvider.today()
                val subscriptions = state.selectedServices.map { draft ->
                    draft.toSubscription(today = today, currency = currency)
                }

                userPreferencesRepository.setCurrency(currency)
                manageSubscriptionUseCase.addSubscriptionsIfMissing(subscriptions)
                userPreferencesRepository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCompleted = true,
                        shouldRequestNotificationPermission = subscriptions.isNotEmpty() && state.remindersEnabled
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "We couldn't save your starter stack. Please try again."
                    )
                }
            }
        }
    }

    private fun skipOnboarding() {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                userPreferencesRepository.setCurrency(_uiState.value.currencyCode)
                userPreferencesRepository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCompleted = true,
                        shouldRequestNotificationPermission = false
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "We couldn't finish setup. Please try again."
                    )
                }
            }
        }
    }
}

private fun OnboardingServiceDraft.toSubscription(
    today: LocalDate,
    currency: String
): Subscription {
    val cycleBackOffsetDays = if (billingPeriod == BillingPeriod.YEARLY) {
        365L
    } else {
        preset.suggestedActiveDays.toLong()
    }
    val startDate = nextBillingDate?.minusDays(cycleBackOffsetDays) ?: today
    val effective = effectiveStrategy
    return Subscription(
        name = preset.name,
        cost = parsedCost ?: preset.defaultCost,
        currency = currency,
        billingPeriod = billingPeriod,
        category = preset.category,
        startDate = startDate,
        strategy = effective,
        customActiveDays = if (effective == RotationStrategy.ROTATION_45_DAYS) 30 else preset.suggestedActiveDays,
        customBlackoutDays = if (effective == RotationStrategy.ROTATION_45_DAYS) 15 else preset.suggestedBlackoutDays,
        colorHex = preset.colorHex,
        iconName = preset.iconName,
        notes = "",
        isBillingDateConfirmed = nextBillingDate != null
    )
}

private object OnboardingServiceCatalog {
    private val popularNames = setOf(
        "Netflix",
        "Disney+",
        "Max (HBO)",
        "Amazon Prime Video",
        "YouTube Premium",
        "Spotify",
        "Apple Music",
        "ChatGPT Plus",
        "Claude Pro",
        "Google AI Pro",
        "Notion Plus",
        "Xbox Game Pass Ultimate",
        "PlayStation Plus"
    )

    private val initialsByName = mapOf(
        "Netflix" to "NF",
        "Disney+" to "D+",
        "Max (HBO)" to "MX",
        "YouTube Premium" to "YT",
        "Spotify" to "SP",
        "Apple Music" to "AM",
        "Apple TV+" to "TV",
        "Amazon Prime Video" to "PV",
        "ChatGPT Plus" to "AI",
        "Claude Pro" to "CL",
        "Google AI Plus" to "G+",
        "Google AI Pro" to "GP",
        "Google AI Ultra" to "GU",
        "Notion Plus" to "NO",
        "Xbox Game Pass Ultimate" to "XB",
        "PlayStation Plus" to "PS",
        "iCloud+" to "iC",
        "Google One" to "G1"
    )

    val services: List<OnboardingServiceDraft> = PresetLibrary.presets.map { preset ->
        OnboardingServiceDraft(
            preset = preset,
            initials = initialsByName[preset.name] ?: initialsForName(preset.name),
            isPopular = preset.name in popularNames
        )
    }
}

private fun initialsForName(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase(Locale.US)
        else -> words.take(2).joinToString("") { it.take(1) }.uppercase(Locale.US)
    }
}

private fun defaultCustomStrategy(category: SubscriptionCategory): RotationStrategy = when (category) {
    SubscriptionCategory.STREAMING,
    SubscriptionCategory.GAMING,
    SubscriptionCategory.AI_TOOLS -> RotationStrategy.ROTATION_45_DAYS
    SubscriptionCategory.MUSIC,
    SubscriptionCategory.CLOUD,
    SubscriptionCategory.FITNESS,
    SubscriptionCategory.NEWS,
    SubscriptionCategory.OTHER -> RotationStrategy.CONTINUOUS
}

private fun List<OnboardingServiceDraft>.forCurrency(currencyCode: String): List<OnboardingServiceDraft> =
    map { draft ->
        if (draft.isCustom) {
            draft.copy(costInput = "", availableTiers = emptyList(), selectedTierLabel = null)
        } else {
            val tiers = RegionalPriceCatalog.tiersFor(draft.preset.name, currencyCode)
            val defaultTier = RegionalPriceCatalog.defaultTierFor(draft.preset.name, currencyCode)
            if (defaultTier != null) {
                draft.copy(
                    costInput = RegionalPriceCatalog.formatEditablePrice(defaultTier.price, currencyCode),
                    billingPeriod = defaultTier.billingPeriod,
                    availableTiers = tiers,
                    selectedTierLabel = defaultTier.label
                )
            } else {
                draft.copy(
                    costInput = "",
                    billingPeriod = BillingPeriod.MONTHLY,
                    availableTiers = emptyList(),
                    selectedTierLabel = null
                )
            }
        }
    }

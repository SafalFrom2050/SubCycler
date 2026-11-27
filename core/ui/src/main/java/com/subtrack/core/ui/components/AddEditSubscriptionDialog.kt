package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class SubscriptionFormStep(val label: String) {
    BASICS("Basics"),
    CYCLE("Cycle"),
    NOTES("Notes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionDialog(
    initialSubscription: Subscription? = null,
    initialPreset: SubscriptionPreset? = null,
    initialStep: Int = 0,
    defaultCurrencyCode: String = "USD",
    onDismiss: () -> Unit,
    onAutoSave: suspend (Subscription) -> Long
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    // Block accidental swipe-down dismissal; users close deliberately via ×, Done, or Back.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    // Single app-wide currency: never expose per-subscription currency options in the form.
    val currencyCode = defaultCurrencyCode
    val isEditing = initialSubscription != null
    val hasLegacyCurrencyMismatch = initialSubscription != null &&
        initialSubscription.currency != defaultCurrencyCode

    var currentStep by remember {
        mutableIntStateOf(initialStep.coerceIn(0, SubscriptionFormStep.entries.lastIndex))
    }
    var visitedCycleStep by remember {
        mutableStateOf(initialStep == SubscriptionFormStep.CYCLE.ordinal)
    }
    LaunchedEffect(currentStep) {
        if (currentStep == SubscriptionFormStep.CYCLE.ordinal) {
            visitedCycleStep = true
        }
    }

    val initialNameSeed = remember(initialSubscription, initialPreset) {
        initialSubscription?.name ?: initialPreset?.name ?: ""
    }
    val initialCostSeed = remember(initialSubscription, initialPreset, currencyCode) {
        when {
            initialSubscription != null && initialSubscription.cost > 0.0 ->
                RegionalPriceCatalog.formatEditablePrice(initialSubscription.cost, currencyCode)
            initialPreset != null ->
                RegionalPriceCatalog.defaultTierFor(initialPreset.name, currencyCode)
                    ?.let { RegionalPriceCatalog.formatEditablePrice(it.price, currencyCode) }
                    ?: ""
            else -> ""
        }
    }
    var name by remember { mutableStateOf(initialNameSeed) }
    var costStr by remember { mutableStateOf(initialCostSeed) }
    var billingPeriod by remember {
        mutableStateOf(
            initialSubscription?.billingPeriod
                ?: initialPreset?.let {
                    RegionalPriceCatalog.defaultTierFor(it.name, currencyCode)?.billingPeriod
                }
                ?: BillingPeriod.MONTHLY
        )
    }
    var category by remember {
        mutableStateOf(initialSubscription?.category ?: initialPreset?.category ?: SubscriptionCategory.STREAMING)
    }
    var strategy by remember {
        mutableStateOf(initialSubscription?.strategy ?: initialPreset?.suggestedStrategy ?: RotationStrategy.ROTATION_45_DAYS)
    }
    var customActiveDays by remember {
        mutableIntStateOf(
            (initialSubscription?.customActiveDays ?: initialPreset?.suggestedActiveDays ?: 30).coerceIn(7, 90)
        )
    }
    var customBlackoutDays by remember {
        mutableIntStateOf(
            (initialSubscription?.customBlackoutDays ?: initialPreset?.suggestedBlackoutDays ?: 15).coerceAtLeast(0)
        )
    }
    var colorHex by remember { mutableStateOf(initialSubscription?.colorHex ?: initialPreset?.colorHex ?: "#0C6B4E") }
    var notes by remember { mutableStateOf(initialSubscription?.notes ?: "") }

    val initialCycleStatus = remember(initialSubscription) {
        initialSubscription?.let { RotationEngine.calculateCycleStatus(it, LocalDate.now()) }
    }
    val isEditingPaused = initialCycleStatus?.phase == CyclePhase.BLACKOUT
    val initialNextBillDate = initialCycleStatus?.phaseEndDate?.takeIf {
        initialSubscription?.isBillingDateConfirmed == true
    }
    var selectedBillingDate by remember { mutableStateOf<LocalDate?>(initialNextBillDate) }
    var didChangeBillingDate by remember { mutableStateOf(false) }
    var billingDateCleared by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var attemptedAdvance by remember { mutableStateOf(false) }

    val previewName = name.trim().ifEmpty { "Sub" }
    val zeroDecimal = MoneyFormatter.isZeroDecimal(currencyCode)
    val matchingTiers = remember(name, currencyCode) {
        RegionalPriceCatalog.tiersFor(name.trim(), currencyCode)
    }

    val currentActiveDays = when {
        billingPeriod == BillingPeriod.YEARLY -> 365
        strategy == RotationStrategy.CUSTOM -> customActiveDays.coerceAtLeast(1)
        else -> strategy.activeDays
    }
    val suggestedBillingDate = selectedBillingDate ?: LocalDate.now().plusDays(currentActiveDays.toLong())

    val trimmedCost = costStr.trim()
    val parsedCost = trimmedCost.toDoubleOrNull()?.takeIf { it > 0.0 }
    val hasInvalidCostFormat = trimmedCost.isNotEmpty() && parsedCost == null
    val canSave = name.trim().isNotEmpty() && !hasInvalidCostFormat

    val effectiveStrategyConfirmed = (initialSubscription?.isStrategyConfirmed == true) ||
        visitedCycleStep ||
        billingPeriod == BillingPeriod.YEARLY
    val effectiveDateConfirmed = when {
        billingDateCleared -> false
        didChangeBillingDate -> selectedBillingDate != null
        visitedCycleStep -> true
        else -> initialSubscription?.isBillingDateConfirmed ?: false
    }

    val previewMissingItems = remember(parsedCost, effectiveDateConfirmed, effectiveStrategyConfirmed, billingPeriod) {
        buildList {
            if (parsedCost == null) add(SetupMissingItem.PRICE)
            if (!effectiveDateConfirmed) add(SetupMissingItem.BILLING_DATE)
            if (billingPeriod == BillingPeriod.MONTHLY && !effectiveStrategyConfirmed) add(SetupMissingItem.CYCLE)
        }
    }

    // Race-free automatic persistence state (insert once on new draft, update in place thereafter!)
    val saveMutex = remember { Mutex() }
    var persistedId by remember { mutableLongStateOf(initialSubscription?.id ?: 0L) }
    var lastSavedSnapshot by remember { mutableStateOf(initialSubscription) }
    var hasAutoSaved by remember { mutableStateOf(initialSubscription != null) }

    val currentDraftSnapshot: Subscription? = remember(
        name,
        parsedCost,
        hasInvalidCostFormat,
        currencyCode,
        billingPeriod,
        category,
        strategy,
        customActiveDays,
        customBlackoutDays,
        colorHex,
        notes,
        selectedBillingDate,
        didChangeBillingDate,
        billingDateCleared,
        visitedCycleStep,
        effectiveDateConfirmed,
        effectiveStrategyConfirmed,
        persistedId
    ) {
        if (name.trim().isEmpty() || hasInvalidCostFormat) return@remember null
        val finalCost = parsedCost ?: 0.0
        val finalActive = customActiveDays.coerceAtLeast(1)
        val finalBlackout = customBlackoutDays.coerceAtLeast(0)
        val effectiveStrategy = if (billingPeriod == BillingPeriod.YEARLY) {
            RotationStrategy.CONTINUOUS
        } else {
            strategy
        }
        val effectiveActiveDays = if (effectiveStrategy == RotationStrategy.CUSTOM) {
            finalActive
        } else {
            effectiveStrategy.activeDays
        }
        val effectiveBlackoutDays = if (effectiveStrategy == RotationStrategy.CUSTOM) {
            finalBlackout
        } else {
            effectiveStrategy.blackoutDays
        }
        val cycleOffsetDays = when {
            billingPeriod == BillingPeriod.YEARLY -> 365L
            isEditingPaused && effectiveBlackoutDays > 0 ->
                (effectiveActiveDays + effectiveBlackoutDays).toLong()
            else -> effectiveActiveDays.toLong()
        }
        val computedStartDate = when {
            billingDateCleared -> LocalDate.now()
            didChangeBillingDate && selectedBillingDate != null ->
                selectedBillingDate!!.minusDays(cycleOffsetDays)
            visitedCycleStep && initialSubscription?.isBillingDateConfirmed != true ->
                suggestedBillingDate.minusDays(cycleOffsetDays)
            initialSubscription != null -> initialSubscription.startDate
            else -> LocalDate.now()
        }
        val scheduleChanged = didChangeBillingDate ||
            billingDateCleared ||
            (initialSubscription != null && (
                effectiveStrategy != initialSubscription.strategy ||
                    billingPeriod != initialSubscription.billingPeriod ||
                    finalActive != initialSubscription.customActiveDays ||
                    finalBlackout != initialSubscription.customBlackoutDays
                ))

        (initialSubscription ?: Subscription(
            id = persistedId,
            name = name.trim(),
            cost = finalCost,
            currency = currencyCode,
            billingPeriod = billingPeriod,
            startDate = computedStartDate
        )).copy(
            id = persistedId,
            name = name.trim(),
            cost = finalCost,
            currency = currencyCode,
            billingPeriod = billingPeriod,
            category = category,
            startDate = computedStartDate,
            strategy = effectiveStrategy,
            customActiveDays = finalActive,
            customBlackoutDays = finalBlackout,
            colorHex = colorHex,
            notes = notes.trim(),
            isBillingDateConfirmed = effectiveDateConfirmed,
            isStrategyConfirmed = effectiveStrategyConfirmed,
            isAutoRenewCancelled = if (scheduleChanged && !isEditingPaused) {
                false
            } else {
                initialSubscription?.isAutoRenewCancelled ?: false
            },
            cancelledCycleIndex = if (scheduleChanged && !isEditingPaused) {
                null
            } else {
                initialSubscription?.cancelledCycleIndex
            },
            bonusActiveDays = if (scheduleChanged) {
                0
            } else {
                initialSubscription?.bonusActiveDays ?: 0
            }
        )
    }

    suspend fun flushSave(snapshot: Subscription) {
        saveMutex.withLock {
            val toPersist = snapshot.copy(id = persistedId)
            if (toPersist == lastSavedSnapshot) return
            val assignedId = onAutoSave(toPersist)
            if (assignedId > 0L) {
                persistedId = assignedId
                lastSavedSnapshot = toPersist.copy(id = assignedId)
            } else {
                lastSavedSnapshot = toPersist
            }
            hasAutoSaved = true
        }
    }

    // Automatic debounced background save whenever the user updates any field or visits Cycle
    LaunchedEffect(currentDraftSnapshot) {
        val snapshot = currentDraftSnapshot ?: return@LaunchedEffect
        if (snapshot.copy(id = persistedId) == lastSavedSnapshot) return@LaunchedEffect
        delay(280)
        flushSave(snapshot)
    }

    fun closeAndFlush() {
        focusManager.clearFocus()
        scope.launch {
            val snapshot = currentDraftSnapshot
            if (snapshot != null) {
                flushSave(snapshot)
            }
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { closeAndFlush() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Clean Header Row: Organic Badge + Title & Subtle Auto-Save Status + Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveServiceBadge(
                    serviceName = previewName,
                    initials = if (name.isBlank()) "+" else ExpressivePalettes.initialsForService(name),
                    size = 42.dp,
                    fontSize = if (name.isBlank()) 17.sp else 13.5.sp,
                    isSelected = name.isNotBlank()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "Edit subscription" else "New subscription",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isSavedComplete = hasAutoSaved && canSave && previewMissingItems.isEmpty()
                        if (isSavedComplete) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = when {
                                !canSave && name.isBlank() -> "Saves automatically"
                                hasInvalidCostFormat -> "Check price format"
                                previewMissingItems.isNotEmpty() && hasAutoSaved -> "Saved · Incomplete"
                                isSavedComplete -> "Saved automatically"
                                else -> "Saving…"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            color = if (hasInvalidCostFormat) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = { closeAndFlush() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spring-expanding stadium progress dots (8dp -> 28dp) above whisper-quiet capsule switcher
            ExpressiveStepDots(
                totalSteps = SubscriptionFormStep.entries.size,
                currentStep = currentStep,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
            )

            // Whisper-Quiet Neutral Capsule Step Switcher (calm, tappable, zero loud bars)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = expressiveCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubscriptionFormStep.entries.forEachIndexed { index, step ->
                        val isCurrent = currentStep == index
                        val isVisited = index < currentStep || (index == 1 && visitedCycleStep)
                        val pillBg by animateColorAsState(
                            targetValue = if (isCurrent) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                Color.Transparent
                            },
                            animationSpec = tween(160),
                            label = "quietStepBg"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isVisited -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            },
                            animationSpec = tween(160),
                            label = "quietStepDot"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(pillBg, RoundedCornerShape(999.dp))
                                .clickable {
                                    if (index > 0 && name.trim().isEmpty()) {
                                        attemptedAdvance = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        return@clickable
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    focusManager.clearFocus()
                                    currentStep = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isCurrent || isVisited) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = step.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = if (isVisited) 0.9f else 0.72f
                                        )
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Compact Animated Step Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 14.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val direction = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { width -> direction * (width / 5) } + fadeIn(tween(180)))
                            .togetherWith(
                                slideOutHorizontally(tween(130)) { width -> -direction * (width / 6) } +
                                    fadeOut(tween(110))
                            )
                    },
                    label = "addEditStepTransition"
                ) { stepIndex ->
                    when (stepIndex) {
                        // STEP 1: BASICS (Name, Optional Price in App Currency, Billing Period, Category)
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            if (!isEditing) {
                                Column {
                                    ExpressiveEyebrow(
                                        text = "QUICK PRESETS",
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        items(PresetLibrary.presets, key = { it.name }) { preset ->
                                            val isPresetSelected = name == preset.name
                                            Surface(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    name = preset.name
                                                    val defaultTier = RegionalPriceCatalog.defaultTierFor(preset.name, currencyCode)
                                                    if (defaultTier != null) {
                                                        costStr = RegionalPriceCatalog.formatEditablePrice(defaultTier.price, currencyCode)
                                                        billingPeriod = defaultTier.billingPeriod
                                                    } else {
                                                        costStr = ""
                                                        billingPeriod = BillingPeriod.MONTHLY
                                                    }
                                                    category = preset.category
                                                    colorHex = preset.colorHex
                                                    strategy = if (billingPeriod == BillingPeriod.YEARLY) {
                                                        RotationStrategy.CONTINUOUS
                                                    } else {
                                                        preset.suggestedStrategy
                                                    }
                                                    customActiveDays = preset.suggestedActiveDays.coerceIn(7, 90)
                                                    customBlackoutDays = preset.suggestedBlackoutDays.coerceAtLeast(0)
                                                },
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (isPresetSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(start = 7.dp, end = 13.dp, top = 6.dp, bottom = 6.dp)
                                                ) {
                                                    ExpressiveServiceBadge(
                                                        serviceName = preset.name,
                                                        size = 22.dp,
                                                        fontSize = 8.5.sp,
                                                        isSelected = isPresetSelected
                                                    )
                                                    Spacer(modifier = Modifier.width(7.dp))
                                                    Text(
                                                        text = preset.name,
                                                        color = if (isPresetSelected) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        },
                                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Service Name + Optional Cost (uses single app currency — no per-item currency picker)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it.take(60) },
                                        label = { Text("Service name") },
                                        placeholder = { Text("e.g. Netflix") },
                                        isError = attemptedAdvance && name.trim().isEmpty(),
                                        modifier = Modifier.weight(1.25f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = dialogTextFieldColors(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                                    )

                                    OutlinedTextField(
                                        value = costStr,
                                        onValueChange = { costStr = it.take(12) },
                                        label = { Text("Cost ($currencyCode)") },
                                        prefix = { Text(MoneyFormatter.symbol(currencyCode)) },
                                        placeholder = { Text("Optional") },
                                        isError = hasInvalidCostFormat,
                                        modifier = Modifier.weight(0.95f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = dialogTextFieldColors(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                    )
                                }

                                if (hasLegacyCurrencyMismatch) {
                                    Text(
                                        text = "App currency is $currencyCode — confirm the price in $currencyCode.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }

                            // 1-Tap Localized Plan Tiers
                            if (matchingTiers.isNotEmpty()) {
                                Column {
                                    ExpressiveEyebrow(
                                        text = "ESTIMATED $currencyCode PLANS · TAP TO FILL",
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(7.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        items(matchingTiers, key = { "${it.label}_${it.billingPeriod}" }) { tier ->
                                            val formatted = RegionalPriceCatalog.formatEditablePrice(tier.price, currencyCode)
                                            val isTierActive = costStr == formatted && billingPeriod == tier.billingPeriod
                                            Surface(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    costStr = formatted
                                                    billingPeriod = tier.billingPeriod
                                                    if (tier.billingPeriod == BillingPeriod.YEARLY) {
                                                        strategy = RotationStrategy.CONTINUOUS
                                                    }
                                                },
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (isTierActive) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            ) {
                                                Text(
                                                    text = "${tier.label} · ${MoneyFormatter.format(tier.price, currencyCode, maximumFractionDigits = if (zeroDecimal) 0 else 2)}${tier.billingPeriod.shortSuffix}",
                                                    color = if (isTierActive) {
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontWeight = if (isTierActive) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.5.sp,
                                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Connected Material 3 Expressive Billing Period Segmented Group
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                ExpressiveEyebrow(text = "BILLING PERIOD")
                                Spacer(modifier = Modifier.height(8.dp))
                                val periodOptions = remember {
                                    BillingPeriod.entries.map { ExpressiveSegmentOption(it, it.displayName) }
                                }
                                ExpressiveSegmentedGroup(
                                    options = periodOptions,
                                    selectedValue = billingPeriod,
                                    onSelect = { period ->
                                        billingPeriod = period
                                        if (period == BillingPeriod.YEARLY) {
                                            strategy = RotationStrategy.CONTINUOUS
                                        }
                                    },
                                    height = 44.dp,
                                    fontSize = 12.5.sp
                                )
                            }

                            // Full-bleed Category Pill Row
                            Column {
                                ExpressiveEyebrow(
                                    text = "CATEGORY",
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    items(SubscriptionCategory.entries, key = { it.name }) { cat ->
                                        val isSelected = category == cat
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                category = cat
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ) {
                                            Text(
                                                text = cat.displayName,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp,
                                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // STEP 2: BILLING DATE + INTERACTIVE CYCLE STRATEGY
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            Surface(
                                onClick = { showDatePicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = expressiveCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (billingDateCleared) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEditingPaused) "PAUSE ENDS" else "NEXT BILLING DATE",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = if (billingDateCleared) {
                                                "Not set · tap to pick"
                                            } else {
                                                suggestedBillingDate.format(dateFormatter)
                                            },
                                            color = if (billingDateCleared) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!billingDateCleared) {
                                        TextButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                billingDateCleared = true
                                                selectedBillingDate = null
                                                didChangeBillingDate = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Not sure",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (billingPeriod == BillingPeriod.MONTHLY) {
                                InteractiveCycleStrategyPicker(
                                    strategy = strategy,
                                    activeDays = customActiveDays,
                                    blackoutDays = customBlackoutDays,
                                    monthlyCost = parsedCost,
                                    currencyCode = currencyCode,
                                    onStrategyChanged = { newStrategy, newActive, newBlackout ->
                                        strategy = newStrategy
                                        customActiveDays = newActive
                                        customBlackoutDays = newBlackout
                                    },
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = expressiveCardBorder()
                                ) {
                                    Text(
                                        text = "Yearly plans renew annually on a continuous 365-day cycle and are normalized to monthly spend in your overview.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        // STEP 3: NOTES & LIVE AUTO-SAVED STATUS SUMMARY
                        else -> Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it.take(180) },
                                label = { Text("Watchlist / Notes (optional)") },
                                placeholder = { Text("e.g. Severance S3 — shown when break ends") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(16.dp),
                                colors = dialogTextFieldColors(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )

                            val isReady = previewMissingItems.isEmpty()
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = expressiveCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Pending,
                                            contentDescription = null,
                                            tint = if (isReady) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.tertiary
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isReady) "Complete · saved automatically" else "Incomplete · saved automatically",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (isReady) {
                                        val priceText = MoneyFormatter.format(parsedCost ?: 0.0, currencyCode) + billingPeriod.shortSuffix
                                        val cadenceSummary = when {
                                            billingPeriod == BillingPeriod.YEARLY -> "Yearly"
                                            strategy == RotationStrategy.CONTINUOUS || customBlackoutDays <= 0 -> "Always on"
                                            strategy == RotationStrategy.CUSTOM -> "${customActiveDays}d on · ${customBlackoutDays}d off"
                                            else -> "${strategy.activeDays}d on · ${strategy.blackoutDays}d off"
                                        }
                                        Text(
                                            text = "${name.trim()} · $priceText · $cadenceSummary · Next bill ${suggestedBillingDate.format(dateFormatter)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        val missingText = previewMissingItems.joinToString(", ") { it.label }
                                        Text(
                                            text = "Missing: $missingText. You can close now and finish these details anytime.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (SetupMissingItem.PRICE in previewMissingItems) {
                                                SuggestionChip(
                                                    onClick = { currentStep = 0 },
                                                    label = { Text("Add price", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                            }
                                            if (SetupMissingItem.BILLING_DATE in previewMissingItems || SetupMissingItem.CYCLE in previewMissingItems) {
                                                SuggestionChip(
                                                    onClick = { currentStep = 1 },
                                                    label = {
                                                        Text(
                                                            if (SetupMissingItem.BILLING_DATE in previewMissingItems) "Set date" else "Pick cycle",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Clean Navigation Footer (No Save buttons — edits auto-save automatically!)
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (attemptedAdvance && !canSave) {
                        Text(
                            text = if (name.trim().isEmpty()) {
                                "Enter a service name to continue."
                            } else {
                                "Enter a valid price or leave it blank for now."
                            },
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentStep > 0) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    focusManager.clearFocus()
                                    currentStep = (currentStep - 1).coerceAtLeast(0)
                                },
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        if (currentStep < SubscriptionFormStep.entries.lastIndex) {
                            Button(
                                onClick = {
                                    attemptedAdvance = true
                                    if (!canSave) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        return@Button
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    focusManager.clearFocus()
                                    currentStep += 1
                                },
                                modifier = Modifier
                                    .weight(if (currentStep > 0) 0.62f else 1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canSave) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    contentColor = if (canSave) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            ) {
                                val nextLabel = when (currentStep) {
                                    0 -> if (billingPeriod == BillingPeriod.YEARLY) "Next: Billing date" else "Next: Cycle"
                                    else -> "Next: Notes"
                                }
                                Text(
                                    text = nextLabel,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    closeAndFlush()
                                },
                                modifier = Modifier
                                    .weight(0.62f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Done",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = suggestedBillingDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosenMillis = pickerState.selectedDateMillis ?: initialMillis
                        selectedBillingDate = Instant.ofEpochMilli(chosenMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        didChangeBillingDate = true
                        billingDateCleared = false
                        showDatePicker = false
                    }
                ) {
                    Text("Use date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            billingDateCleared = true
                            selectedBillingDate = null
                            didChangeBillingDate = false
                            showDatePicker = false
                        }
                    ) {
                        Text("Not sure")
                    }
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

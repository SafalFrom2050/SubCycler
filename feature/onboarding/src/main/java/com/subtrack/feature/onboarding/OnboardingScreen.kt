package com.subtrack.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.ServicePlanTier
import com.subtrack.core.domain.model.SubscriptionCategory
import com.subtrack.core.ui.components.CurrencyPickerDialog
import com.subtrack.core.ui.components.ExpressiveBrandWordmark
import com.subtrack.core.ui.components.ExpressiveClover4Shape
import com.subtrack.core.ui.components.ExpressiveCookie12Shape
import com.subtrack.core.ui.components.ExpressiveCookie8Shape
import com.subtrack.core.ui.components.ExpressiveEyebrow
import com.subtrack.core.ui.components.ExpressiveMaterialPolygons
import com.subtrack.core.ui.components.ExpressiveMoneyText
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.components.ExpressiveReactiveIconButton
import com.subtrack.core.ui.components.ExpressiveScreenBackdrop
import com.subtrack.core.ui.components.ExpressiveSectionRow
import com.subtrack.core.ui.components.ExpressiveSectionStack
import com.subtrack.core.ui.components.ExpressiveServiceBadge
import com.subtrack.core.ui.components.ExpressiveStepDots
import com.subtrack.core.ui.components.ExpressiveTonalPalette
import com.subtrack.core.ui.components.FannedBadgeStack
import com.subtrack.core.ui.components.ReactiveIcon
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.ShapeBloomBackdrop
import com.subtrack.core.ui.components.rememberMorphingPolygonShape
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.DisplayHeroFontFamily
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun OnboardingRoute(
    onFinished: (requestNotificationPermission: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onFinished(uiState.shouldRequestNotificationPermission)
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var serviceRequestingDate by remember { mutableStateOf<String?>(null) }
    var showCustomServiceDialog by rememberSaveable { mutableStateOf(false) }
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var customServiceSeed by rememberSaveable { mutableStateOf("") }
    var customServiceCategory by rememberSaveable { mutableStateOf(SubscriptionCategory.OTHER) }

    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME) {
        onAction(OnboardingAction.Back)
    }

    ExpressiveScreenBackdrop(
        modifier = modifier,
        emphasized = uiState.step == OnboardingStep.WELCOME || uiState.step == OnboardingStep.STARTER_PLAN
    ) {
        ShapeBloomBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            if (uiState.step != OnboardingStep.WELCOME) {
                OnboardingProgress(step = uiState.step)
                Spacer(modifier = Modifier.height(10.dp))
            }

            AnimatedContent(
                targetState = uiState.step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val direction = if (forward) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { fullWidth -> direction * (fullWidth / 4) } +
                        fadeIn(animationSpec = tween(220)) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(160)
                        ) { fullWidth -> -direction * (fullWidth / 5) } +
                            fadeOut(animationSpec = tween(140))
                    )
                },
                label = "onboardingStepTransition"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        currencyCode = uiState.currencyCode,
                        isSaving = uiState.isSaving,
                        onChooseCurrency = { showCurrencyDialog = true },
                        onGetStarted = { onAction(OnboardingAction.BeginSelection) },
                        onSkip = { onAction(OnboardingAction.SkipOnboarding) }
                    )
                    OnboardingStep.SELECT_SERVICES -> ServiceSelectionStep(
                        uiState = uiState,
                        onAction = onAction,
                        onChooseCurrency = { showCurrencyDialog = true },
                        onAddCustomService = { seed, category ->
                            customServiceSeed = seed
                            customServiceCategory = category
                            showCustomServiceDialog = true
                        }
                    )
                    OnboardingStep.REVIEW_DETAILS -> ServiceReviewStep(
                        uiState = uiState,
                        onAction = onAction,
                        onChooseCurrency = { showCurrencyDialog = true },
                        onChooseDate = { serviceRequestingDate = it }
                    )
                    OnboardingStep.STARTER_PLAN -> StarterPlanStep(
                        uiState = uiState,
                        onAction = onAction
                    )
                }
            }
        }
    }

    serviceRequestingDate?.let { serviceName ->
        val draft = uiState.services.firstOrNull { it.preset.name == serviceName }
        if (draft != null) {
            key(serviceName) {
                BillingDatePicker(
                    serviceName = serviceName,
                    initialDate = draft.nextBillingDate,
                    onDismiss = { serviceRequestingDate = null },
                    onDateSelected = { date ->
                        onAction(OnboardingAction.SetNextBillingDate(serviceName, date))
                        serviceRequestingDate = null
                    }
                )
            }
        }
    }

    if (showCustomServiceDialog) {
        CustomServiceDialog(
            initialName = customServiceSeed,
            initialCategory = customServiceCategory,
            currencyCode = uiState.currencyCode,
            onDismiss = { showCustomServiceDialog = false },
            onAdd = { serviceName, category, costInput, billingPeriod ->
                onAction(
                    OnboardingAction.AddCustomService(
                        serviceName = serviceName,
                        category = category,
                        costInput = costInput,
                        billingPeriod = billingPeriod
                    )
                )
                showCustomServiceDialog = false
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            selectedCurrencyCode = uiState.currencyCode,
            onDismiss = { showCurrencyDialog = false },
            onSelect = { currencyCode ->
                onAction(OnboardingAction.SetCurrency(currencyCode))
                showCurrencyDialog = false
            }
        )
    }
}

@Composable
private fun OnboardingProgress(step: OnboardingStep) {
    val activeStep = (step.ordinal - 1).coerceAtLeast(0)
    val totalSteps = OnboardingStep.entries.size - 1
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        ExpressiveStepDots(
            totalSteps = totalSteps,
            currentStep = activeStep
        )
    }
}

@Composable
private fun WelcomeStep(
    currencyCode: String,
    isSaving: Boolean,
    onChooseCurrency: () -> Unit,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val boldPalette = ExpressivePalettes.boldPrimary()
    val oceanPalette = ExpressivePalettes.softOcean()
    val slatePalette = ExpressivePalettes.softLavender()
    val lightCardBorder = if (isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                onClick = onSkip,
                enabled = !isSaving,
                shape = RoundedCornerShape(999.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                border = lightCardBorder
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Interactive "Vault Mosaic" Wall (BunPod ChannelWall inspiration!):
            // Starts as a gapless 4x2 grid of monogram squares, holds 280ms, then blooms into
            // 8 distinct M3 Expressive silhouettes — tap any tile to morph it into a fresh polygon!
            BotanicalVaultMosaic(
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            ExpressiveBrandWordmark(
                fontSize = 42.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontFamily = DisplayFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        )
                    ) {
                        append("Pause. ")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = DisplayHeroFontFamily,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        )
                    ) {
                        append("Binge. ")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = DisplayHeroFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp
                        )
                    ) {
                        append("Save.")
                    }
                },
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            ExpressiveSectionStack {
                WelcomeFeatureRow(
                    index = 0,
                    count = 3,
                    shape = ExpressiveCookie8Shape,
                    palette = ExpressivePalettes.softSky(),
                    icon = Icons.Default.Autorenew,
                    title = "30 days on, 15 days off · save ~32% yearly"
                )
                WelcomeFeatureRow(
                    index = 1,
                    count = 3,
                    shape = CircleShape,
                    palette = oceanPalette,
                    icon = Icons.Default.Shield,
                    title = "100% offline · zero internet permission"
                )
                WelcomeFeatureRow(
                    index = 2,
                    count = 3,
                    shape = ExpressiveClover4Shape,
                    palette = slatePalette,
                    icon = Icons.Default.NotificationsNone,
                    title = "Quiet local reminders before renewal dates"
                )
            }
        }

        // First-launch single app-wide currency selector
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChooseCurrency()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = lightCardBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = MoneyFormatter.symbol(currencyCode),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App currency · $currencyCode",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${MoneyFormatter.displayName(currencyCode)} · used across all services",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Change currency",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        PrimaryOnboardingButton(
            text = "Let's build your stack →",
            enabled = !isSaving,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onGetStarted()
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun BotanicalVaultMosaic(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val tiles = remember {
        listOf(
            "NF" to 0,
            "D+" to 1,
            "SP" to 2,
            "AI" to 3,
            "YT" to 4,
            "CL" to 5,
            "PV" to 6,
            "XB" to 7
        )
    }
    val palettes = listOf(
        ExpressivePalettes.boldPrimary(),
        ExpressivePalettes.softStone(),
        ExpressivePalettes.softSky(),
        ExpressivePalettes.softOcean(),
        ExpressivePalettes.softLavender(),
        ExpressivePalettes.softStone(),
        ExpressivePalettes.softOcean(),
        ExpressivePalettes.softSky()
    )
    var bloomed by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            bloomed = true
        } else {
            delay(280L)
            bloomed = true
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(4).forEachIndexed { rowIdx, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { colIdx, (monogram, seedIdx) ->
                    val palette = palettes[(rowIdx * 4 + colIdx) % palettes.size]
                    var shapeIdx by remember { mutableIntStateOf(seedIdx % ExpressiveMaterialPolygons.mosaicPalette.size) }
                    val targetPolygon = if (!bloomed) {
                        ExpressiveMaterialPolygons.Square
                    } else {
                        ExpressiveMaterialPolygons.mosaicPalette[shapeIdx]
                    }
                    val shape = rememberMorphingPolygonShape(targetPolygon = targetPolygon)

                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(shape)
                            .background(palette.container, shape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                shapeIdx = (shapeIdx + 3) % ExpressiveMaterialPolygons.mosaicPalette.size
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monogram,
                            fontFamily = DisplayHeroFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp,
                            color = palette.content
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    index: Int,
    count: Int,
    shape: Shape,
    palette: ExpressiveTonalPalette,
    icon: ImageVector,
    title: String
) {
    ExpressiveSectionRow(
        index = index,
        count = count,
        outerRadius = 24.dp,
        innerRadius = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(shape)
                    .background(palette.container, shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.content,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CurrencyPillButton(
    currencyCode: String,
    onClick: () -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = if (isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${MoneyFormatter.symbol(currencyCode)} $currencyCode",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Choose currency",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ServiceSelectionStep(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onChooseCurrency: () -> Unit,
    onAddCustomService: (String, SubscriptionCategory) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val searchSeed = uiState.searchQuery.trim()
    val hasExactMatch = uiState.services.any {
        it.preset.name.equals(searchSeed, ignoreCase = true)
    }
    val slatePalette = ExpressivePalettes.softLavender()
    val bgFadeColor = MaterialTheme.colorScheme.background

    Column(modifier = Modifier.fillMaxSize()) {
        // Compact Top Utility Row with >= 44-48dp ergonomic targets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveReactiveIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { onAction(OnboardingAction.Back) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                iconTint = MaterialTheme.colorScheme.onSurface,
                buttonSize = 44.dp,
                iconSize = 19.dp,
                motion = ReactiveMotionType.NUDGE_LEFT
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrencyPillButton(
                    currencyCode = uiState.currencyCode,
                    onClick = onChooseCurrency
                )

                TextButton(
                    onClick = { onAction(OnboardingAction.SkipOnboarding) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What's in your rotation?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.7).sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pill Search Bar with clear 'X' + Soft Tonal '+ Custom' Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onAction(OnboardingAction.SetSearchQuery(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search services…", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onAction(OnboardingAction.SetSearchQuery("")) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = onboardingTextFieldColors()
            )

            var customTapCount by remember { mutableIntStateOf(0) }
            Surface(
                onClick = {
                    customTapCount++
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddCustomService(
                        searchSeed.takeIf { it.isNotEmpty() && !hasExactMatch }.orEmpty(),
                        uiState.selectedCategory.subscriptionCategory ?: SubscriptionCategory.OTHER
                    )
                },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(999.dp),
                color = slatePalette.container
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add a custom service",
                        tint = slatePalette.content,
                        size = 16.dp,
                        motion = ReactiveMotionType.PLUS_TWIST,
                        triggerKey = customTapCount
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Custom",
                        color = slatePalette.content,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Borderless Full-Pill Category Filter Chips with smooth trailing fade
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(end = 26.dp)
            ) {
                items(OnboardingCategory.entries, key = { it.name }) { category ->
                    val selected = category == uiState.selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAction(OnboardingAction.SetCategory(category))
                        },
                        label = {
                            Text(
                                text = category.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .height(40.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Shape-Morphing 3-Column Service Grid with soft bottom fade
        if (uiState.visibleServices.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching preset",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (searchSeed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                onAddCustomService(
                                    searchSeed,
                                    uiState.selectedCategory.subscriptionCategory ?: SubscriptionCategory.OTHER
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add \"$searchSeed\" as custom", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 26.dp)
                ) {
                    items(
                        items = uiState.visibleServices,
                        key = { it.preset.name }
                    ) { service ->
                        ServiceTile(
                            service = service,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(OnboardingAction.ToggleService(service.preset.name))
                            }
                        )
                    }
                }
                // Soft gradient fade so bottom row never slices harshly above CTA
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, bgFadeColor.copy(alpha = 0.85f))
                            )
                        )
                )
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val selectedCount = uiState.selectedServices.size
        if (selectedCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FannedBadgeStack(
                    serviceNames = uiState.selectedServices.map { it.preset.name },
                    maxVisible = 5,
                    badgeSize = 32.dp,
                    step = 21.dp,
                    ringWidth = 2.dp
                )
            }
        }
        PrimaryOnboardingButton(
            text = if (selectedCount == 0) {
                "Pick at least 1 service"
            } else {
                "Continue · $selectedCount picked"
            },
            enabled = selectedCount > 0,
            onClick = { onAction(OnboardingAction.ContinueToReview) }
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun CustomServiceDialog(
    initialName: String,
    initialCategory: SubscriptionCategory,
    currencyCode: String,
    onDismiss: () -> Unit,
    onAdd: (String, SubscriptionCategory, String, BillingPeriod) -> Unit
) {
    var serviceName by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var costInput by rememberSaveable(initialName) { mutableStateOf("") }
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    var billingPeriod by rememberSaveable(initialName) { mutableStateOf(BillingPeriod.MONTHLY) }
    val isValid = serviceName.trim().isNotEmpty() &&
        costInput.toDoubleOrNull()?.let { it > 0.0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                text = "Add custom service",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Service name") },
                    placeholder = { Text("e.g. My VPN") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = onboardingTextFieldColors()
                )
                OutlinedTextField(
                    value = costInput,
                    onValueChange = { costInput = it.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${billingPeriod.displayName} price ($currencyCode)") },
                    prefix = { Text(MoneyFormatter.symbol(currencyCode)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = costInput.isNotEmpty() && costInput.toDoubleOrNull()?.let { it > 0.0 } != true,
                    shape = RoundedCornerShape(16.dp),
                    colors = onboardingTextFieldColors()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillingPeriod.entries.forEach { period ->
                        val selected = billingPeriod == period
                        Surface(
                            onClick = { billingPeriod = period },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = period.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                ExpressiveEyebrow(text = "CATEGORY")
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SubscriptionCategory.entries.chunked(4).forEach { categoryRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            categoryRow.forEach { option ->
                                FilterChip(
                                    selected = option == category,
                                    onClick = { category = option },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(999.dp),
                                    label = {
                                        Text(
                                            text = customCategoryLabel(option),
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 11.sp,
                                            fontWeight = if (option == category) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(serviceName.trim(), category, costInput, billingPeriod) },
                enabled = isValid,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Add", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun customCategoryLabel(category: SubscriptionCategory): String = when (category) {
    SubscriptionCategory.STREAMING -> "Video"
    SubscriptionCategory.MUSIC -> "Music"
    SubscriptionCategory.GAMING -> "Gaming"
    SubscriptionCategory.AI_TOOLS -> "AI/Apps"
    SubscriptionCategory.CLOUD -> "Cloud"
    SubscriptionCategory.FITNESS -> "Fitness"
    SubscriptionCategory.NEWS -> "News"
    SubscriptionCategory.OTHER -> "Other"
}

@Composable
private fun ServiceTile(
    service: OnboardingServiceDraft,
    onClick: () -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            service.isSelected -> 1.01f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "serviceTileScale"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (service.isSelected) 28.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "serviceTileCorner"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (service.isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(180),
        label = "serviceTileBg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = if (!isDark && !service.isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        } else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            if (service.isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 12.dp,
                        motion = ReactiveMotionType.POP_BOUNCE,
                        triggerKey = service.isSelected,
                        animateOnFirstComposition = true
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpressiveServiceBadge(
                    serviceName = service.preset.name,
                    initials = service.initials,
                    size = 46.dp,
                    fontSize = 14.sp,
                    isSelected = service.isSelected,
                    isPressed = isPressed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.preset.name,
                    color = if (service.isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    fontWeight = if (service.isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ServiceReviewStep(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onChooseCurrency: () -> Unit,
    onChooseDate: (String) -> Unit
) {
    val hasRegionalEstimates = uiState.selectedServices.any { it.availableTiers.isNotEmpty() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveReactiveIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { onAction(OnboardingAction.Back) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                iconTint = MaterialTheme.colorScheme.onSurface,
                buttonSize = 44.dp,
                iconSize = 19.dp,
                motion = ReactiveMotionType.NUDGE_LEFT
            )
            CurrencyPillButton(
                currencyCode = uiState.currencyCode,
                onClick = onChooseCurrency
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Confirm prices",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.7).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasRegionalEstimates) {
                "Estimated ${uiState.currencyCode} plans pre-filled — tap a tier, cycle pill, or edit anytime."
            } else {
                "Enter what you pay in ${uiState.currencyCode}. Dates are optional."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(uiState.selectedServices, key = { it.preset.name }) { service ->
                ReviewServiceCard(
                    service = service,
                    currencyCode = uiState.currencyCode,
                    showMissingPriceError = uiState.errorMessage != null,
                    onCostChanged = {
                        onAction(OnboardingAction.UpdateCost(service.preset.name, it))
                    },
                    onSelectTier = { tier ->
                        onAction(OnboardingAction.SelectPlanTier(service.preset.name, tier))
                    },
                    onBillingPeriodChanged = { period ->
                        onAction(OnboardingAction.SetBillingPeriod(service.preset.name, period))
                    },
                    onToggleStrategy = {
                        onAction(OnboardingAction.ToggleStrategy(service.preset.name))
                    },
                    onChooseDate = { onChooseDate(service.preset.name) }
                )
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryOnboardingButton(
            text = "Continue",
            onClick = { onAction(OnboardingAction.ShowStarterPlan) }
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ReviewServiceCard(
    service: OnboardingServiceDraft,
    currencyCode: String,
    showMissingPriceError: Boolean,
    onCostChanged: (String) -> Unit,
    onSelectTier: (ServicePlanTier) -> Unit,
    onBillingPeriodChanged: (BillingPeriod) -> Unit,
    onToggleStrategy: () -> Unit,
    onChooseDate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val hasPriceError = (service.costInput.isNotEmpty() && service.parsedCost == null) ||
        (showMissingPriceError && service.parsedCost == null)
    val zeroDecimal = MoneyFormatter.isZeroDecimal(currencyCode)
    val isMonthly = service.billingPeriod == BillingPeriod.MONTHLY

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ExpressiveServiceBadge(
                        serviceName = service.preset.name,
                        initials = service.initials,
                        size = 42.dp,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = service.preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Tappable Strategy Toggle Pill on Monthly plans
                        if (isMonthly) {
                            val isRotating = service.effectiveStrategy != RotationStrategy.CONTINUOUS
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleStrategy()
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isRotating) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ) {
                                Text(
                                    text = if (isRotating) "45-day cycle · tap to switch" else "Continuous · tap to rotate",
                                    color = if (isRotating) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Annual renewal plan",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Ergonomic Monthly / Yearly Pill Switcher
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        BillingPeriod.entries.forEach { period ->
                            val selected = service.billingPeriod == period
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBillingPeriodChanged(period)
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (period == BillingPeriod.MONTHLY) "Mo" else "Yr",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Full-bleed 1-Tap Localized Plan Tier Chips (no hard right-edge chopping)
            if (service.availableTiers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(service.availableTiers, key = { "${it.label}_${it.billingPeriod}" }) { tier ->
                        val isTierSelected = service.selectedTierLabel == tier.label &&
                            service.billingPeriod == tier.billingPeriod
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTier(tier)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isTierSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Text(
                                text = "${tier.label} · ${MoneyFormatter.format(tier.price, currencyCode, maximumFractionDigits = if (zeroDecimal) 0 else 2)}${tier.billingPeriod.shortSuffix}",
                                color = if (isTierSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 11.5.sp,
                                fontWeight = if (isTierSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = service.costInput,
                    onValueChange = onCostChanged,
                    modifier = Modifier.weight(0.95f),
                    label = { Text("${service.billingPeriod.displayName} ($currencyCode)") },
                    placeholder = {
                        Text(
                            text = if (zeroDecimal) "e.g. 1590" else "0.00",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    prefix = { Text(MoneyFormatter.symbol(currencyCode)) },
                    singleLine = true,
                    isError = hasPriceError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Surface(
                    onClick = onChooseDate,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NEXT BILL",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = service.nextBillingDate?.format(dateFormatter) ?: "Optional",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterPlanStep(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val rotatableCount = uiState.selectedServices.count {
        it.effectiveStrategy != RotationStrategy.CONTINUOUS
    }
    val sagePalette = ExpressivePalettes.softSage()
    val boldPalette = ExpressivePalettes.boldPrimary()
    val lightBorder = if (isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

    Column(modifier = Modifier.fillMaxSize()) {
        BackTitleRow(onBack = { onAction(OnboardingAction.Back) })

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(ExpressiveCookie12Shape)
                    .background(boldPalette.container, ExpressiveCookie12Shape),
                contentAlignment = Alignment.Center
            ) {
                ReactiveIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = boldPalette.content,
                    size = 34.dp,
                    motion = ReactiveMotionType.POP_BOUNCE,
                    triggerKey = uiState.selectedServices.size,
                    animateOnFirstComposition = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Your stack is ready",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.9).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (rotatableCount == 0) {
                    "All selected services start as continuous plans."
                } else {
                    "$rotatableCount service${if (rotatableCount == 1) "" else "s"} set to smart 45-day cycles. Editable anytime."
                },
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            SelectedServiceAvatars(services = uiState.selectedServices)

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PayoffCard(
                    label = "CURRENT MONTHLY",
                    value = MoneyFormatter.format(uiState.monthlyCost, uiState.currencyCode),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    border = lightBorder,
                    modifier = Modifier.weight(1f)
                )
                PayoffCard(
                    label = "EST. YEARLY SAVINGS",
                    value = MoneyFormatter.format(
                        uiState.potentialAnnualSavings,
                        uiState.currencyCode,
                        maximumFractionDigits = 0
                    ),
                    containerColor = sagePalette.container,
                    labelColor = sagePalette.content.copy(alpha = 0.78f),
                    valueColor = sagePalette.content,
                    border = null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transparent Notification Pre-Priming Card (explains local reminders before OS prompt)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = lightBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(ExpressiveClover4Shape)
                            .background(MaterialTheme.colorScheme.primaryContainer, ExpressiveClover4Shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quiet renewal reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "1 local alert 3 days before a billing date · 100% on-device",
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.remindersEnabled,
                        onCheckedChange = { onAction(OnboardingAction.SetRemindersEnabled(it)) }
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% offline · zero internet permission",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        PrimaryOnboardingButton(
            text = if (uiState.isSaving) "Saving…" else "Open dashboard",
            enabled = !uiState.isSaving,
            onClick = { onAction(OnboardingAction.CompleteOnboarding) },
            showProgress = uiState.isSaving
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun SelectedServiceAvatars(services: List<OnboardingServiceDraft>) {
    FannedBadgeStack(
        serviceNames = services.map { it.preset.name },
        maxVisible = 5,
        badgeSize = 46.dp,
        step = 32.dp,
        ringWidth = 3.dp
    )
}

@Composable
private fun PayoffCard(
    label: String,
    value: String,
    containerColor: Color,
    labelColor: Color,
    valueColor: Color,
    border: BorderStroke? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = border
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpressiveEyebrow(
                text = label,
                fontSize = 11.sp,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            ExpressiveMoneyText(
                formattedAmount = value,
                heroFontSize = 26.sp,
                color = valueColor,
                elevateCurrencySymbol = false
            )
        }
    }
}

@Composable
private fun BackTitleRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveReactiveIconButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconTint = MaterialTheme.colorScheme.onSurface,
            buttonSize = 44.dp,
            iconSize = 19.dp,
            motion = ReactiveMotionType.NUDGE_LEFT
        )
    }
}

@Composable
private fun PrimaryOnboardingButton(
    text: String,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingDatePicker(
    serviceName: String,
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate?) -> Unit
) {
    val fallbackDate = remember { LocalDate.now().plusDays(30) }
    val initialMillis = (initialDate ?: fallbackDate)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = pickerState.selectedDateMillis ?: initialMillis
                    val selectedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    onDateSelected(selectedDate)
                }
            ) {
                Text("Use date")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDateSelected(null) }) {
                    Text("Not sure")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    ) {
        Column {
            Text(
                text = "$serviceName next billing date",
                modifier = Modifier.padding(start = 24.dp, top = 18.dp),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun onboardingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay

/**
 * Actionable quick check-in card dealt inside [CycleCheckInDeck] (inspired by BunPod's `OpmlSuggestionDeck`).
 */
sealed interface CheckInDeckItem {
    val key: String
    val serviceName: String
    val headline: String
    val subtitle: String
    val primaryLabel: String
    val secondaryLabel: String
    val lifecycleState: ServiceBadgeLifecycleState

    data class BankCompletedBreak(
        val cycleStatus: CycleStatus,
        val formattedSavings: String
    ) : CheckInDeckItem {
        override val key: String = "bank_${cycleStatus.subscription.id}"
        override val serviceName: String = cycleStatus.subscription.name
        override val headline: String = "${cycleStatus.subscription.name} break complete"
        override val subtitle: String = "Bank ~$formattedSavings saved & restart your active cycle."
        override val primaryLabel: String = "Bank $formattedSavings"
        override val secondaryLabel: String = "Later"
        override val lifecycleState: ServiceBadgeLifecycleState = ServiceBadgeLifecycleState.UNBANKED_READY
    }

    data class FinishIncompleteSetup(
        val cycleStatus: CycleStatus
    ) : CheckInDeckItem {
        private val missingText: String = cycleStatus.subscription.missingSetupItems
            .joinToString(", ") { it.label }
        override val key: String = "incomplete_${cycleStatus.subscription.id}"
        override val serviceName: String = cycleStatus.subscription.name
        override val headline: String = "Finish ${cycleStatus.subscription.name} setup"
        override val subtitle: String = "Missing: $missingText — unlock countdowns & savings."
        override val primaryLabel: String = "Finish setup"
        override val secondaryLabel: String = "Skip"
        override val lifecycleState: ServiceBadgeLifecycleState = ServiceBadgeLifecycleState.INCOMPLETE
    }
}

/**
 * Builds the prioritized list of check-in cards whenever 2+ items await quick confirmation
 * (finished breaks to bank or incomplete drafts missing details).
 */
fun buildCycleCheckInItems(statuses: List<CycleStatus>): List<CheckInDeckItem> {
    val breaksToBank = statuses
        .filter { !it.subscription.isIncomplete && it.subscription.isRotating && it.hasUnbankedCompletedBreak }
        .map { status ->
            val saved = MoneyFormatter.format(
                status.perBreakSavings,
                status.subscription.currency,
                maximumFractionDigits = 0
            )
            CheckInDeckItem.BankCompletedBreak(status, saved)
        }

    val incompleteDrafts = statuses
        .filter { it.subscription.isIncomplete }
        .map { CheckInDeckItem.FinishIncompleteSetup(it) }

    return breaksToBank + incompleteDrafts
}

/**
 * Interactive top-of-feed "Cycle Check-In" Card Deck (inspired by BunPod's `OpmlSuggestionDeck`):
 * - Header pairs `QUICK CHECK-IN` eyebrow, tabular `1 of N` counter, and spring-stretching
 *   [ExpressiveStepDots] (`8dp → 28dp` stadium pills).
 * - Fast scale-fade exits (`140ms`) + spring slide-in entrances (`ExpressiveSpatialDefault`).
 * - Transforms into a `Cookie9Sided` Botanical Emerald `"All caught up"` celebration card
 *   after the final decision before collapsing smoothly out of the way.
 */
@Composable
fun CycleCheckInDeck(
    items: List<CheckInDeckItem>,
    onPrimaryAction: (CheckInDeckItem) -> Unit,
    onDismissDeck: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val skippedKeys = remember { mutableStateListOf<String>() }
    var resolvedCount by remember { mutableIntStateOf(0) }
    var showAllCaughtUp by remember { mutableStateOf(false) }

    val remaining = items.filterNot { it.key in skippedKeys }
    val totalInSession = (remaining.size + resolvedCount + skippedKeys.size).coerceAtLeast(1)
    val activeItem = remaining.firstOrNull()
    val currentStepIndex = (resolvedCount + skippedKeys.size).coerceIn(0, (totalInSession - 1).coerceAtLeast(0))

    LaunchedEffect(remaining.isEmpty(), resolvedCount) {
        if (remaining.isEmpty() && resolvedCount > 0) {
            showAllCaughtUp = true
            delay(1800L)
            showAllCaughtUp = false
            onDismissDeck()
        }
    }

    if (activeItem == null && !showAllCaughtUp) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveEyebrow(
                text = "QUICK CHECK-IN",
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (showAllCaughtUp) "Done" else "${currentStepIndex + 1} of $totalInSession",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (totalInSession in 2..10 && !showAllCaughtUp) {
            ExpressiveStepDots(
                totalSteps = totalInSession,
                currentStep = currentStepIndex,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        AnimatedContent(
            targetState = if (showAllCaughtUp) null else activeItem,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                } else {
                    (slideInHorizontally(
                        animationSpec = ExpressiveMotion.expressiveSpatialDefaultIntOffset()
                    ) { width -> (width * 0.22f).toInt() } +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
                        ) +
                        fadeIn(tween(180))).togetherWith(
                        scaleOut(targetScale = 0.90f, animationSpec = tween(140)) +
                            fadeOut(tween(130))
                    )
                }
            },
            label = "checkInDeckCardTransition"
        ) { cardItem ->
            if (cardItem == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .expressivePolygonSurface(
                                    shape = ExpressiveCookie9Shape,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All caught up",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$resolvedCount check-in${if (resolvedCount == 1) "" else "s"} updated cleanly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = expressiveCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpressiveServiceBadge(
                                serviceName = cardItem.serviceName,
                                size = 44.dp,
                                fontSize = 14.sp,
                                lifecycleState = cardItem.lifecycleState
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cardItem.headline,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cardItem.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    skippedKeys.add(cardItem.key)
                                },
                                modifier = Modifier.height(42.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = cardItem.secondaryLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                )
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resolvedCount++
                                    onPrimaryAction(cardItem)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = cardItem.primaryLabel,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
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
}

package com.subtrack.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled

/**
 * Urgent Next-Action Banner featuring BunPod's synchronized Dual-Pass Curtain-Contrast Vessel:
 * Renders the elapsed countdown fraction in high-contrast accent fill (`palette.accent`) with
 * pixel-aligned inverted foreground ink (`onAccent`) clipped to the exact same seam so WCAG
 * legibility stays razor-sharp even mid-character!
 */
@Composable
fun NextActionBanner(
    urgentStatuses: List<CycleStatus>,
    calendarSuggestion: CalendarAwareSuggestion? = null,
    onUrgentActionClick: (CycleStatus) -> Unit = {},
    onCalendarPrimaryAction: () -> Unit = {},
    onCalendarDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (urgentStatuses.isEmpty()) return

    val reducedMotion = rememberReducedMotionEnabled()
    val topUrgent = urgentStatuses.first()
    val action = topUrgent.nextAction
    val isDue = action.urgency == ActionUrgency.ACTION_DUE
    val palette = if (isDue) ExpressivePalettes.softRose() else ExpressivePalettes.softAmber()
    val fillColor = if (isDue) MaterialTheme.colorScheme.error else palette.accent
    val onFillColor = if (isDue) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.surface

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bannerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "urgentBannerScale"
    )
    val bannerCorner by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 24.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "urgentBannerCorner"
    )

    // Vessel urgency fill progresses from 22% (warning) up to 92% (action due today)
    val targetVesselFraction = when {
        isDue && topUrgent.daysRemainingInPhase <= 0 -> 0.94f
        isDue -> (0.72f + (1f - (topUrgent.daysRemainingInPhase.coerceIn(0, 3) / 3f)) * 0.22f)
        else -> topUrgent.progressFraction.coerceIn(0.28f, 0.72f)
    }
    val vesselFraction by animateFloatAsState(
        targetValue = targetVesselFraction,
        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
        label = "urgentVesselFraction"
    )

    Surface(
        onClick = { onUrgentActionClick(topUrgent) },
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = bannerScale
                scaleY = bannerScale
            },
        shape = RoundedCornerShape(bannerCorner),
        color = palette.container
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Dual-Layer Synchronized Curtain-Contrast Vessel Row
            Box(modifier = Modifier.fillMaxWidth()) {
                // Pass 1: Base tonal surface content
                UrgentBannerHeaderContent(
                    title = "${topUrgent.subscription.name}: ${action.title}",
                    description = action.description,
                    badgeBg = palette.selectedContainer,
                    contentColor = palette.content,
                    triggerKey = topUrgent.subscription.id,
                    isDue = isDue,
                    isPressed = isPressed
                )

                // Pass 2: Clipped high-contrast filled vessel with inverted foreground ink!
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawWithContent {
                            clipRect(right = size.width * vesselFraction.coerceIn(0f, 1f)) {
                                drawRect(color = fillColor)
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    UrgentBannerHeaderContent(
                        title = "${topUrgent.subscription.name}: ${action.title}",
                        description = action.description,
                        badgeBg = onFillColor.copy(alpha = 0.20f),
                        contentColor = onFillColor,
                        triggerKey = topUrgent.subscription.id,
                        isDue = isDue,
                        isPressed = isPressed
                    )
                }
            }

            calendarSuggestion?.let { suggestion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                ) {
                    HorizontalDivider(color = palette.content.copy(alpha = 0.14f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = suggestion.message,
                        fontSize = 12.sp,
                        color = palette.content.copy(alpha = 0.85f)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCalendarDismiss) {
                            Text("Dismiss", color = palette.content.copy(alpha = 0.75f))
                        }
                        TextButton(onClick = onCalendarPrimaryAction) {
                            Text(suggestion.primaryActionLabel, color = palette.content, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgentBannerHeaderContent(
    title: String,
    description: String,
    badgeBg: Color,
    contentColor: Color,
    triggerKey: Any,
    isDue: Boolean,
    isPressed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .expressivePolygonSurface(
                    shape = ExpressiveCookie8Shape,
                    color = badgeBg
                ),
            contentAlignment = Alignment.Center
        ) {
            ReactiveIcon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = contentColor,
                size = 18.dp,
                motion = ReactiveMotionType.BELL_RING,
                isPressed = isPressed,
                triggerKey = triggerKey,
                animateOnFirstComposition = isDue
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        ReactiveIcon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open details",
            tint = contentColor.copy(alpha = 0.80f),
            size = 20.dp,
            motion = ReactiveMotionType.NUDGE_RIGHT,
            isPressed = isPressed
        )
    }
}

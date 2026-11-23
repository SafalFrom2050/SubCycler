package com.subtrack.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CycleStatus

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

    val topUrgent = urgentStatuses.first()
    val action = topUrgent.nextAction
    val isDue = action.urgency == ActionUrgency.ACTION_DUE
    val palette = if (isDue) ExpressivePalettes.softRose() else ExpressivePalettes.softAmber()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bannerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "urgentBannerScale"
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
        shape = RoundedCornerShape(24.dp),
        color = palette.container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(ExpressiveCookie8Shape)
                        .background(palette.selectedContainer, ExpressiveCookie8Shape),
                    contentAlignment = Alignment.Center
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = palette.content,
                        size = 18.dp,
                        motion = ReactiveMotionType.BELL_RING,
                        isPressed = isPressed,
                        triggerKey = topUrgent.subscription.id,
                        animateOnFirstComposition = isDue
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${topUrgent.subscription.name}: ${action.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = palette.content
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = action.description,
                        fontSize = 12.sp,
                        color = palette.content.copy(alpha = 0.78f)
                    )
                }
                ReactiveIcon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open details",
                    tint = palette.content.copy(alpha = 0.75f),
                    size = 20.dp,
                    motion = ReactiveMotionType.NUDGE_RIGHT,
                    isPressed = isPressed
                )
            }
            calendarSuggestion?.let { suggestion ->
                Spacer(Modifier.height(10.dp))
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

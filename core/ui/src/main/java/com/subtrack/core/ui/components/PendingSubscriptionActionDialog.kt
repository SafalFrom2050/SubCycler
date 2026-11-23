package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.SubscriptionActionProgressType

@Composable
fun PendingSubscriptionActionDialog(
    pendingAction: PendingSubscriptionAction,
    onCompleted: () -> Unit,
    onCancel: () -> Unit
) {
    val isUnsubscribe = pendingAction.actionType == SubscriptionActionProgressType.UNSUBSCRIBE
    val title = if (isUnsubscribe) {
        "Unsubscribing from ${pendingAction.subscriptionName}"
    } else {
        "Subscribing to ${pendingAction.subscriptionName}"
    }
    val description = if (isUnsubscribe) {
        "Did you finish unsubscribing from ${pendingAction.subscriptionName}? Confirming marks auto-renew as cancelled for this cycle."
    } else {
        "Did you finish subscribing to ${pendingAction.subscriptionName}? Confirming starts a new active cycle today."
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val badgeInteraction = remember { MutableInteractionSource() }
                val badgePressed by badgeInteraction.collectIsPressedAsState()
                var badgePopCount by remember { mutableIntStateOf(0) }
                val badgeEntryScale = remember { Animatable(0.72f) }
                LaunchedEffect(badgePopCount) {
                    badgeEntryScale.snapTo(0.76f)
                    badgeEntryScale.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }

                ExpressiveServiceBadge(
                    serviceName = pendingAction.subscriptionName,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = badgeEntryScale.value
                            scaleY = badgeEntryScale.value
                        }
                        .clickable(
                            interactionSource = badgeInteraction,
                            indication = null
                        ) { badgePopCount++ },
                    size = 56.dp,
                    fontSize = 17.sp,
                    isSelected = true,
                    isPressed = badgePressed
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onCompleted,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Completed", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

package com.subtrack.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.ui.theme.ErrorRose
import com.subtrack.core.ui.theme.WarningAmber

@Composable
fun NextActionBanner(
    urgentStatuses: List<CycleStatus>,
    modifier: Modifier = Modifier
) {
    if (urgentStatuses.isEmpty()) return

    val topUrgent = urgentStatuses.first()
    val action = topUrgent.nextAction
    val isDue = action.urgency == ActionUrgency.ACTION_DUE

    val containerColor = if (isDue) ErrorRose.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)
    val contentColor = if (isDue) ErrorRose else WarningAmber

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(contentColor.copy(alpha = 0.4f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${topUrgent.subscription.name}: ${action.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

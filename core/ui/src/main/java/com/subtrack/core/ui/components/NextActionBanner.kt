package com.subtrack.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val containerColor = if (isDue) Color(0xFF160A0A) else Color(0xFF14120A)
    val contentColor = if (isDue) ErrorRose else WarningAmber
    val cleanTitle = action.title.replace("⚡ ", "").replace("⚠️ ", "").replace("🔔 ", "")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = contentColor.copy(alpha = 0.35f)
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
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${topUrgent.subscription.name}: $cleanTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

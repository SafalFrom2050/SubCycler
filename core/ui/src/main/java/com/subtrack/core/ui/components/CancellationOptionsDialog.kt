package com.subtrack.core.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.CancellationDestination
import com.subtrack.core.domain.model.CancellationLinks
import com.subtrack.core.domain.model.SubscriptionActionProgressType

/** Returns only whether Android accepted the navigation request, never provider completion. */
fun openCancellationDestination(context: Context, destination: CancellationDestination): Boolean {
    val uri = Uri.parse(destination.url)
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return false
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
fun CancellationOptionsDialog(
    subscriptionName: String,
    onDismiss: () -> Unit,
    onOpenLink: (CancellationDestination) -> Boolean,
    actionType: SubscriptionActionProgressType = SubscriptionActionProgressType.UNSUBSCRIBE
) {
    val clipboard = LocalClipboardManager.current
    var failedUrl by rememberSaveable(subscriptionName) { mutableStateOf<String?>(null) }
    var copied by rememberSaveable(subscriptionName) { mutableStateOf(false) }
    val isUnsubscribe = actionType == SubscriptionActionProgressType.UNSUBSCRIBE
    val destinations = CancellationLinks.destinationsFor(subscriptionName)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExpressiveServiceBadge(
                    serviceName = subscriptionName,
                    size = 40.dp,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isUnsubscribe) "Cancel subscription" else "Subscribe to service",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isUnsubscribe) {
                        "Where do you pay for $subscriptionName?"
                    } else {
                        "Where do you want to subscribe to $subscriptionName?"
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isUnsubscribe) {
                        "Choose the provider shown on your receipt. Links open outside SubCycler. Opening a page does not cancel anything or update your tracker."
                    } else {
                        "Choose your billing provider to open its page outside SubCycler. Use the active notification or return here to confirm once completed."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (CancellationLinks.forService(subscriptionName) == null) {
                    Text(
                        "No service link is available for this name. If you pay directly, open the service's official website and find Billing or Subscriptions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Positional Connected Section Stack for billing destinations (28dp outer / 4dp inner seams)
                ExpressiveSectionStack {
                    destinations.forEachIndexed { index, destination ->
                        ExpressiveSectionRow(
                            index = index,
                            count = destinations.size,
                            onClick = {
                                copied = false
                                failedUrl = if (onOpenLink(destination)) null else destination.url
                            },
                            containerColor = if (index == 0) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            outerRadius = 22.dp,
                            innerRadius = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = destination.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (index == 0) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = destination.guidance,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = if (index == 0) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = if (index == 0) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    "Billed by a mobile carrier or another partner? Use that provider's account or support. Finish the cancellation there and keep its confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                failedUrl?.let { url ->
                    Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(
                            "Couldn't open the link. Copy it into a browser or use another device.",
                            color = MaterialTheme.colorScheme.error
                        )
                        SelectionContainer { Text(url, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(url))
                            copied = true
                        }) { Text(if (copied) "Link copied" else "Copy link", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

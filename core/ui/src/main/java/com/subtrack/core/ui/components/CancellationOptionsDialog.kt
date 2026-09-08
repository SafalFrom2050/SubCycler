package com.subtrack.core.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.subtrack.core.domain.model.CancellationDestination
import com.subtrack.core.domain.model.CancellationLinks

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
    onOpenLink: (CancellationDestination) -> Boolean
) {
    val clipboard = LocalClipboardManager.current
    var failedUrl by rememberSaveable(subscriptionName) { mutableStateOf<String?>(null) }
    var copied by rememberSaveable(subscriptionName) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel subscription") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Where do you pay for $subscriptionName?")
                Text(
                    "Choose the provider shown on your receipt. Links open outside SubCycler. Opening a page does not cancel anything or update your tracker.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (CancellationLinks.forService(subscriptionName) == null) {
                    Text(
                        "No service link is available for this name. If you pay directly, open the service's official website and find Billing or Subscriptions.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                CancellationLinks.destinationsFor(subscriptionName).forEach { destination ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                copied = false
                                failedUrl = if (onOpenLink(destination)) null else destination.url
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(destination.label) }
                        Text(destination.guidance, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "Billed by a mobile carrier or another partner? Use that provider's account or support. Finish the cancellation there and keep its confirmation.",
                    style = MaterialTheme.typography.bodySmall
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
                        }) { Text(if (copied) "Link copied" else "Copy link") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

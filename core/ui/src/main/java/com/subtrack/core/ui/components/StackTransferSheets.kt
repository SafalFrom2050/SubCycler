package com.subtrack.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.backup.SubscriptionBackupCodec
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.theme.DisplayFontFamily
import java.util.Locale

data class ImportPreviewItem(
    val subscription: Subscription,
    val alreadyInStack: Boolean
)

/**
 * Classifies incoming transfer items against the user's existing portfolio using the same
 * case-insensitive service-name key as `ManageSubscriptionUseCase.addSubscriptionsIfMissing`.
 */
fun buildImportPreviewItems(
    incoming: List<Subscription>,
    existing: List<Subscription>
): List<ImportPreviewItem> {
    val existingNames = existing
        .map { it.name.trim().lowercase(Locale.ROOT) }
        .toSet()
    val seenIncoming = mutableSetOf<String>()
    return incoming.map { sub ->
        val normalized = sub.name.trim().lowercase(Locale.ROOT)
        val duplicate = normalized in existingNames || !seenIncoming.add(normalized)
        ImportPreviewItem(
            subscription = sub,
            alreadyInStack = duplicate
        )
    }
}

/**
 * Material 3 Expressive v4.0 App-to-App Share Sheet:
 * Lets users share their stack via messaging & social apps (offline `subcycler://import?d=...`
 * transfer link), clipboard copy, `.subcycler` file share, or local device save — with a 1-tap
 * privacy toggle for personal notes & earned savings history and zero technical jargon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStackSheet(
    subscriptions: List<Subscription>,
    onShareTransferLink: (includePersonalHistory: Boolean) -> Unit,
    onCopyTransferLink: (includePersonalHistory: Boolean) -> Unit,
    onShareBackupFile: (includePersonalHistory: Boolean) -> Unit,
    onSaveBackupFile: (includePersonalHistory: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var includePersonalHistory by rememberSaveable { mutableStateOf(true) }
    val boldPalette = ExpressivePalettes.boldPrimary()
    val skyPalette = ExpressivePalettes.softSky()
    val oceanPalette = ExpressivePalettes.softOcean()
    val slatePalette = ExpressivePalettes.softLavender()
    val count = subscriptions.size
    val hasSubscriptions = count > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                ExpressiveEyebrow(
                    text = "APP-TO-APP TRANSFER",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share your stack",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Move your subscriptions to another device or share your rotation setup through messaging and social apps — 100% offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!hasSubscriptions) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = expressiveCardBorder()
                ) {
                    Text(
                        text = "Add at least one subscription first to create a transfer link or backup file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                // Fanned portfolio summary + Personal history privacy toggle
                ExpressiveSectionStack {
                    ExpressiveSectionRow(index = 0, count = 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FannedBadgeStack(
                                serviceNames = subscriptions.map { it.name },
                                badgeSize = 36.dp,
                                maxVisible = 4,
                                ringColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$count subscription${if (count == 1) "" else "s"} ready",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subscriptions.take(3).joinToString(" · ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    ExpressiveSectionRow(index = 1, count = 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include notes & savings history",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (includePersonalHistory) {
                                        "Full backup for moving to your own device."
                                    } else {
                                        "Clean template for friends — personal notes & earned savings are omitted."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = includePersonalHistory,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    includePersonalHistory = it
                                }
                            )
                        }
                    }
                }

                // Connected transfer options (zero JSON / CSV jargon)
                ExpressiveSectionStack(eyebrow = "CHOOSE HOW TO SEND") {
                    TransferActionRow(
                        index = 0,
                        count = 4,
                        title = "Share via messaging & social",
                        subtitle = "Send a SubCycler link in WhatsApp, Telegram, Messages, or social apps",
                        icon = Icons.Default.Share,
                        badgeShape = ExpressiveCookie8Shape,
                        palette = boldPalette,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShareTransferLink(includePersonalHistory)
                        }
                    )
                    TransferActionRow(
                        index = 1,
                        count = 4,
                        title = "Copy transfer link",
                        subtitle = "Copy to clipboard to paste in any chat or on another device",
                        icon = Icons.Default.ContentCopy,
                        badgeShape = ExpressiveArchShape,
                        palette = skyPalette,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopyTransferLink(includePersonalHistory)
                        }
                    )
                    TransferActionRow(
                        index = 2,
                        count = 4,
                        title = "Share backup file",
                        subtitle = "Send a .subcycler file via Quick Share, Bluetooth, or chat",
                        icon = Icons.Default.Description,
                        badgeShape = ExpressiveClover4Shape,
                        palette = oceanPalette,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShareBackupFile(includePersonalHistory)
                        }
                    )
                    TransferActionRow(
                        index = 3,
                        count = 4,
                        title = "Save to device",
                        subtitle = "Store a .subcycler backup file in your device folders",
                        icon = Icons.Default.SaveAlt,
                        badgeShape = ExpressivePebbleShape,
                        palette = slatePalette,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSaveBackupFile(includePersonalHistory)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive v4.0 Import Sheet:
 * Accepts a pasted `subcycler://import?d=...` link / shared chat message (1-tap Paste)
 * or opens a `.subcycler` backup file from device storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStackSheet(
    onInspectTransferText: (String) -> Unit,
    onPickBackupFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pastedText by rememberSaveable { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }
    val oceanPalette = ExpressivePalettes.softOcean()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                ExpressiveEyebrow(
                    text = "RECEIVE STACK",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Import into SubCycler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paste a SubCycler link or shared message, or open a .subcycler file. You'll review every item before anything is added.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = expressiveCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transfer link or message",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val clip = clipboard.getText()?.text.orEmpty()
                                if (clip.isBlank()) {
                                    inlineError = "Your clipboard is empty."
                                } else {
                                    pastedText = clip
                                    inlineError = null
                                    if (SubscriptionBackupCodec.containsTransferLink(clip)) {
                                        onInspectTransferText(clip)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Paste",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = {
                            pastedText = it
                            inlineError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Paste a subcycler:// link or chat message…",
                                fontSize = 13.sp
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    if (!inlineError.isNullOrBlank()) {
                        Text(
                            text = inlineError!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onInspectTransferText(pastedText)
                        },
                        enabled = pastedText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Preview import", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            ExpressiveSectionStack(eyebrow = "OR FROM DEVICE STORAGE") {
                TransferActionRow(
                    index = 0,
                    count = 1,
                    title = "Open a .subcycler file",
                    subtitle = "Choose a backup file saved on your phone",
                    icon = Icons.Default.FolderOpen,
                    badgeShape = ExpressiveArchShape,
                    palette = oceanPalette,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPickBackupFile()
                    }
                )
            }
        }
    }
}

/**
 * Safe confirmation preview sheet shown whenever a `subcycler://import?d=...` deep link,
 * shared chat message, pasted link, or `.subcycler` file is received.
 * Displays fanned badges, every incoming service, and duplicate status before adding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheet(
    items: List<ImportPreviewItem>,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val newCount = items.count { !it.alreadyInStack }
    val existingCount = items.size - newCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ExpressiveEyebrow(
                        text = "INCOMING STACK",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (newCount > 0) "Ready to import" else "Already in your stack",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            newCount == 0 -> "All ${items.size} services are already tracked."
                            existingCount == 0 -> "$newCount new subscription${if (newCount == 1) "" else "s"} will be added."
                            else -> "$newCount new · $existingCount already tracked (skipped automatically)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    FannedBadgeStack(
                        serviceNames = items.map { it.subscription.name },
                        badgeSize = 34.dp,
                        maxVisible = 3,
                        ringColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { idx, item -> "${item.subscription.name}_$idx" }
                ) { index, item ->
                    val sub = item.subscription
                    val cycleSummary = when {
                        sub.billingPeriod == BillingPeriod.YEARLY -> "Yearly"
                        sub.strategy == RotationStrategy.CONTINUOUS -> "Always on"
                        else -> "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                    }
                    val priceLabel = if (sub.cost > 0.0) {
                        "${MoneyFormatter.format(sub.cost, sub.currency)}${sub.billingPeriod.shortSuffix}"
                    } else {
                        "Price not set"
                    }

                    ExpressiveSectionRow(
                        index = index,
                        count = items.size,
                        containerColor = if (item.alreadyInStack) {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExpressiveServiceBadge(
                                serviceName = sub.name,
                                size = 38.dp,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.name,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$priceLabel · $cycleSummary",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (item.alreadyInStack) {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = if (item.alreadyInStack) "Already added" else "New",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.alreadyInStack) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (newCount > 0) "Cancel" else "Close", fontWeight = FontWeight.SemiBold)
                }
                if (newCount > 0) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirmImport()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Add $newCount service${if (newCount == 1) "" else "s"}",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferActionRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeShape: Shape,
    palette: ExpressiveTonalPalette,
    onClick: () -> Unit
) {
    ExpressiveSectionRow(
        index = index,
        count = count,
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .expressivePolygonSurface(
                        shape = badgeShape,
                        color = palette.container
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.content,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

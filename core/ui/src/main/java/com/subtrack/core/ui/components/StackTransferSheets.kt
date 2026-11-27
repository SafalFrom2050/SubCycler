package com.subtrack.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.backup.SubscriptionBackupCodec
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.R
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
 * Simplified Material 3 Expressive Share Sheet:
 * - **Main Mode (Option 2)**: Live scannable QR Stack Card right on screen (for instant
 *   phone-to-phone camera transfer) + 1 focal **Share Stack Card** button (branded PNG with
 *   embedded QR for WhatsApp, Instagram, Telegram, LINE, Discord, X).
 * - **Secondary Options (Option 1 & 3)**: Compact **Short transfer link** (`subcycler://s/...`
 *   with inline Copy) and **Backup file (`.subcycler`)** paired with a clean human caption + Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStackSheet(
    subscriptions: List<Subscription>,
    onShareStackCard: (includePersonalHistory: Boolean) -> Unit,
    onShareTransferLink: (includePersonalHistory: Boolean) -> Unit,
    onCopyTransferLink: (includePersonalHistory: Boolean) -> Unit,
    onShareBackupFile: (includePersonalHistory: Boolean) -> Unit,
    onSaveBackupFile: (includePersonalHistory: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var includePersonalHistory by rememberSaveable { mutableStateOf(true) }
    val skyPalette = ExpressivePalettes.softSky()
    val oceanPalette = ExpressivePalettes.softOcean()
    val count = subscriptions.size
    val hasSubscriptions = count > 0

    val liveShortLink = remember(subscriptions, includePersonalHistory) {
        if (hasSubscriptions) {
            SubscriptionBackupCodec.toTransferLink(subscriptions, includePersonalHistory)
        } else {
            ""
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                ExpressiveEyebrow(
                    text = "SHARE & TRANSFER",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Share your stack",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
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
                        text = "Add at least one subscription first to generate a Stack Card or backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                // Hero Interactive Stack Card with live scannable QR code (Option 2 Main Mode)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFF1F5243))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF02151B),
                                        Color(0xFF0A2B22),
                                        Color(0xFF0F3D30)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_subcycler_brand_mark),
                                        contentDescription = "SubCycler logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Text(
                                        text = "SubCycler",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFBAFCE0)
                                    )
                                }

                                FannedBadgeStack(
                                    serviceNames = subscriptions.map { it.name },
                                    badgeSize = 32.dp,
                                    maxVisible = 4,
                                    ringColor = Color(0xFF0A2B22)
                                )

                                Column {
                                    Text(
                                        text = "$count subscription${if (count == 1) "" else "s"}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Scan with any phone camera, or share as a card.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFFA7D3C5)
                                    )
                                }
                            }

                            ExpressiveQrCodeImage(
                                content = liveShortLink,
                                size = 126.dp,
                                containerCorner = 18.dp
                            )
                        }
                    }
                }

                // Compact Privacy Toggle
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = expressiveCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include notes & savings history",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (includePersonalHistory) {
                                    "Full backup for moving to your own device"
                                } else {
                                    "Clean template for friends (notes & savings omitted)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = includePersonalHistory,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                includePersonalHistory = it
                            }
                        )
                    }
                }

                // Focal Main CTA: Share Visual Stack Card PNG (Option 2)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShareStackCard(includePersonalHistory)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Stack Card",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                // Quiet Secondary Options (Option 1 Short Link + Option 3 Clean Backup File)
                ExpressiveSectionStack(eyebrow = "OR SEND ANOTHER WAY") {
                    TransferActionRowWithPill(
                        index = 0,
                        count = 2,
                        title = "Short transfer link",
                        subtitle = "Compact link for chats & messages (${liveShortLink.length} chars)",
                        icon = Icons.Default.Link,
                        badgeShape = ExpressiveCookie8Shape,
                        palette = skyPalette,
                        trailingPillLabel = "Copy",
                        onRowClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShareTransferLink(includePersonalHistory)
                        },
                        onTrailingPillClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopyTransferLink(includePersonalHistory)
                        }
                    )
                    TransferActionRowWithPill(
                        index = 1,
                        count = 2,
                        title = "Backup file (.subcycler)",
                        subtitle = "Clean chat summary with attached file",
                        icon = Icons.Default.Description,
                        badgeShape = ExpressiveArchShape,
                        palette = oceanPalette,
                        trailingPillLabel = "Save",
                        onRowClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShareBackupFile(includePersonalHistory)
                        },
                        onTrailingPillClick = {
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
 * Simplified Material 3 Expressive Import Sheet:
 * - **Main Mode (Option 2)**: Pick a saved Stack Card image/screenshot to decode its QR offline.
 * - **Secondary Options (Option 1 & 3)**: 1-tap Paste short link or open a `.subcycler` file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStackSheet(
    onPickCardImage: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                ExpressiveEyebrow(
                    text = "RECEIVE STACK",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Import into SubCycler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Pick a Stack Card image, paste a short link, or open a .subcycler file. You'll preview everything before adding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Focal Main CTA: Import from Stack Card Image (Option 2)
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPickCardImage()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import from Stack Card image",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp
                )
            }

            // Option 1: Paste Short Link
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = expressiveCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste a short link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
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
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
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
                                text = "subcycler://s/…",
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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

                    if (pastedText.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onInspectTransferText(pastedText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Preview import", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Option 3: Open .subcycler file
            ExpressiveSectionStack {
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
 * Safe confirmation preview sheet shown whenever a `subcycler://s/...` deep link,
 * shared Stack Card image, shared chat message, pasted link, or `.subcycler` file is received.
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
private fun TransferActionRowWithPill(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeShape: Shape,
    palette: ExpressiveTonalPalette,
    trailingPillLabel: String,
    onRowClick: () -> Unit,
    onTrailingPillClick: () -> Unit
) {
    ExpressiveSectionRow(
        index = index,
        count = count,
        onClick = onRowClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
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
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onTrailingPillClick,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = trailingPillLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
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
                    .size(38.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
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

package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.SwirlLogoColors
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled

private enum class SettingsPage(val title: String) {
    ROOT("Settings"),
    APPEARANCE("Appearance & Icon"),
    NOTIFICATIONS("Notifications & Calendar"),
    TRANSFER("Transfer & Backup")
}

/**
 * Multi-Level Nested Offline Settings Sheet using Material 3 Expressive Connected Card Stacks
 * (`28dp` outer radii, `4dp` inner seams, `2dp` gaps, press-blooming `14dp` inner corners).
 * Keeps the root screen compact and zero-scroll while drilling cleanly into Appearance & Icon,
 * Notifications & Calendar, and Transfer & Backup sub-pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsSheet(
    enabled: Boolean,
    selectedCalendarCount: Int,
    accessIssue: CalendarAccessIssue?,
    notificationsAvailable: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenCalendars: () -> Unit,
    onDismiss: () -> Unit,
    currencyCode: String = "USD",
    hasExistingSubscriptions: Boolean = false,
    notificationLeadDays: Int = 3,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    useStackHeroMood: Boolean = true,
    selectedLauncherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
    recommendedLauncherIcon: AppLauncherIcon? = null,
    onCurrencySelected: ((String) -> Unit)? = null,
    onNotificationLeadDaysSelected: ((Int) -> Unit)? = null,
    onThemeModeSelected: ((AppThemeMode) -> Unit)? = null,
    onUseStackHeroMoodChanged: ((Boolean) -> Unit)? = null,
    onLauncherIconSelected: ((AppLauncherIcon) -> Unit)? = null,
    onOpenNotificationSettings: (() -> Unit)? = null,
    onPreviewNotifications: (() -> Unit)? = null,
    onShareStack: (() -> Unit)? = null,
    onImportStack: (() -> Unit)? = null,
    backupMessage: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reducedMotion = rememberReducedMotionEnabled()
    val skyPalette = ExpressivePalettes.softSky()
    val oceanPalette = ExpressivePalettes.softOcean()
    val stonePalette = ExpressivePalettes.softStone()
    val rosePalette = ExpressivePalettes.softRose()

    var currentPage by remember { mutableStateOf(SettingsPage.ROOT) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var pendingCurrencyChange by remember { mutableStateOf<String?>(null) }

    val hasAppearanceSection = onThemeModeSelected != null ||
        onUseStackHeroMoodChanged != null ||
        onLauncherIconSelected != null
    val hasTransferSection = onShareStack != null || onImportStack != null

    val pageSlideSpec = remember(reducedMotion) {
        ExpressiveMotion.standardSpatialFast<IntOffset>().orSnapIfReduced(reducedMotion)
    }
    val pageFadeSpec = remember(reducedMotion) {
        ExpressiveMotion.effectsFast<Float>().orSnapIfReduced(reducedMotion)
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
        ) {
            // Top Bar: Root "Settings" title OR Sub-page back arrow + sub-page title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage != SettingsPage.ROOT) {
                    Surface(
                        onClick = { currentPage = SettingsPage.ROOT },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = currentPage.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    val forward = targetState != SettingsPage.ROOT
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(pageSlideSpec) { dir * (it / 5) } + fadeIn(pageFadeSpec))
                        .togetherWith(
                            slideOutHorizontally(pageSlideSpec) { -dir * (it / 6) } + fadeOut(pageFadeSpec)
                        )
                },
                label = "settingsNestedPage"
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (page) {
                        SettingsPage.ROOT -> {
                            // 1. Personalize Stack (Appearance & Icon · App Currency)
                            val personalizeRows = buildList {
                                if (hasAppearanceSection) add("appearance")
                                if (onCurrencySelected != null) add("currency")
                            }
                            if (personalizeRows.isNotEmpty()) {
                                ExpressiveSectionStack(eyebrow = "PERSONALIZE") {
                                    personalizeRows.forEachIndexed { idx, key ->
                                        when (key) {
                                            "appearance" -> {
                                                val moodText = if (useStackHeroMood) "Living mood on" else "Static accent"
                                                val summary = "${themeMode.displayName} · ${selectedLauncherIcon.displayName} · $moodText"
                                                SettingsVaultActionRow(
                                                    index = idx,
                                                    count = personalizeRows.size,
                                                    title = "Appearance & Icon",
                                                    subtitle = summary,
                                                    icon = Icons.Default.Palette,
                                                    badgeShape = ExpressiveCookie8Shape,
                                                    palette = skyPalette,
                                                    onClick = { currentPage = SettingsPage.APPEARANCE }
                                                )
                                            }
                                            "currency" -> ExpressiveSectionRow(
                                                index = idx,
                                                count = personalizeRows.size,
                                                onClick = { showCurrencyPicker = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(38.dp)
                                                                .expressivePolygonSurface(
                                                                    shape = ExpressiveClover4Shape,
                                                                    color = oceanPalette.container
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = MoneyFormatter.symbol(currencyCode),
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 14.sp,
                                                                color = oceanPalette.content
                                                            )
                                                        }
                                                        Spacer(Modifier.width(12.dp))
                                                        Column {
                                                            Text(
                                                                text = "App currency",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "${MoneyFormatter.symbol(currencyCode)} $currencyCode · ${MoneyFormatter.displayName(currencyCode)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = "Change currency",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Alerts & Data Stack (Notifications & Calendar · Transfer & Backup)
                            val utilityRows = buildList {
                                add("notifications")
                                if (hasTransferSection) add("transfer")
                            }
                            ExpressiveSectionStack(eyebrow = "ALERTS & DATA") {
                                utilityRows.forEachIndexed { idx, key ->
                                    when (key) {
                                        "notifications" -> {
                                            val calendarPart = if (enabled) {
                                                "Calendar on ($selectedCalendarCount)"
                                            } else {
                                                "Calendar off"
                                            }
                                            val notifSummary = if (!notificationsAvailable) {
                                                "Notifications off · $calendarPart"
                                            } else if (onNotificationLeadDaysSelected != null) {
                                                "${notificationLeadDays}d before renewal · $calendarPart"
                                            } else {
                                                calendarPart
                                            }
                                            SettingsVaultActionRow(
                                                index = idx,
                                                count = utilityRows.size,
                                                title = "Notifications & Calendar",
                                                subtitle = notifSummary,
                                                icon = if (!notificationsAvailable) {
                                                    Icons.Default.NotificationsOff
                                                } else {
                                                    Icons.Default.NotificationsActive
                                                },
                                                badgeShape = ExpressiveArchShape,
                                                palette = if (!notificationsAvailable) rosePalette else skyPalette,
                                                onClick = { currentPage = SettingsPage.NOTIFICATIONS }
                                            )
                                        }
                                        "transfer" -> SettingsVaultActionRow(
                                            index = idx,
                                            count = utilityRows.size,
                                            title = "Transfer & Backup",
                                            subtitle = "QR Stack Card, short link, or backup file",
                                            icon = Icons.Default.Share,
                                            badgeShape = ExpressiveCookie8Shape,
                                            palette = stonePalette,
                                            onClick = { currentPage = SettingsPage.TRANSFER }
                                        )
                                    }
                                }
                            }

                            if (!backupMessage.isNullOrBlank()) {
                                Text(
                                    text = backupMessage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        SettingsPage.APPEARANCE -> {
                            val appearanceRows = buildList {
                                if (onThemeModeSelected != null) add("theme")
                                if (onLauncherIconSelected != null) add("app_icon")
                                if (onUseStackHeroMoodChanged != null) add("hero_mood")
                            }
                            if (appearanceRows.isNotEmpty()) {
                                ExpressiveSectionStack(eyebrow = "THEME & ACCENT") {
                                    appearanceRows.forEachIndexed { index, key ->
                                        when (key) {
                                            "theme" -> ExpressiveSectionRow(index = index, count = appearanceRows.size) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(
                                                        text = "Appearance",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    val themeOptions = remember {
                                                        listOf(
                                                            ExpressiveSegmentOption(AppThemeMode.SYSTEM, AppThemeMode.SYSTEM.displayName, Icons.Default.BrightnessAuto),
                                                            ExpressiveSegmentOption(AppThemeMode.LIGHT, AppThemeMode.LIGHT.displayName, Icons.Default.LightMode),
                                                            ExpressiveSegmentOption(AppThemeMode.DARK, AppThemeMode.DARK.displayName, Icons.Default.DarkMode)
                                                        )
                                                    }
                                                    ExpressiveSegmentedGroup(
                                                        options = themeOptions,
                                                        selectedValue = themeMode,
                                                        onSelect = onThemeModeSelected!!,
                                                        height = 42.dp,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }

                                            "app_icon" -> ExpressiveSectionRow(index = index, count = appearanceRows.size) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 15.dp),
                                                    verticalArrangement = Arrangement.spacedBy(11.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "App icon & accent",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Sets your home screen icon and app accent color.",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (recommendedLauncherIcon != null && recommendedLauncherIcon != selectedLauncherIcon) {
                                                            Spacer(Modifier.width(8.dp))
                                                            Surface(
                                                                onClick = { onLauncherIconSelected!!(recommendedLauncherIcon) },
                                                                shape = RoundedCornerShape(999.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer
                                                            ) {
                                                                Text(
                                                                    text = "Match ${recommendedLauncherIcon.displayName}",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    LazyRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        items(AppLauncherIcon.entries, key = { it.name }) { iconOption ->
                                                            val isSelected = iconOption == selectedLauncherIcon
                                                            val isStackMatch = iconOption == recommendedLauncherIcon
                                                            val swirlPreviewColors = remember(iconOption) {
                                                                BrandColorHarmonizer.swirlColorsForLauncherIcon(iconOption)
                                                            }
                                                            Surface(
                                                                onClick = { onLauncherIconSelected!!(iconOption) },
                                                                shape = RoundedCornerShape(20.dp),
                                                                color = if (isSelected) {
                                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                                                } else {
                                                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                                                },
                                                                border = BorderStroke(
                                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                                    color = if (isSelected) {
                                                                        MaterialTheme.colorScheme.primary
                                                                    } else {
                                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                                    }
                                                                )
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .width(84.dp)
                                                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.BottomEnd) {
                                                                        SubCyclerSwirlLogo(
                                                                            colors = swirlPreviewColors,
                                                                            size = 44.dp,
                                                                            cornerRadius = 13.dp,
                                                                            contentDescriptionText = "${iconOption.displayName} app icon"
                                                                        )
                                                                        if (isSelected) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size(18.dp)
                                                                                    .clip(CircleShape)
                                                                                    .background(MaterialTheme.colorScheme.primary),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Check,
                                                                                    contentDescription = "Selected",
                                                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                                                    modifier = Modifier.size(11.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    Text(
                                                                        text = iconOption.displayName,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                        color = if (isSelected) {
                                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                                        } else {
                                                                            MaterialTheme.colorScheme.onSurface
                                                                        },
                                                                        maxLines = 1
                                                                    )
                                                                    Box(
                                                                        modifier = Modifier.height(18.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        if (isStackMatch) {
                                                                            Text(
                                                                                text = "Stack fit",
                                                                                fontSize = 9.5.sp,
                                                                                lineHeight = 12.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = MaterialTheme.colorScheme.primary,
                                                                                maxLines = 1
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            "hero_mood" -> ExpressiveSectionRow(index = index, count = appearanceRows.size) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Living stack mood",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Softly tint the hero card, swirl logo & active dock pill from your active subscriptions.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Switch(
                                                        checked = useStackHeroMood,
                                                        onCheckedChange = onUseStackHeroMoodChanged!!
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsPage.NOTIFICATIONS -> {
                            val reminderRows = buildList {
                                if (!notificationsAvailable) add("notif_off")
                                if (onNotificationLeadDaysSelected != null) add("lead_days")
                                if (onPreviewNotifications != null) add("preview_notif")
                            }
                            if (reminderRows.isNotEmpty()) {
                                ExpressiveSectionStack(eyebrow = "RENEWAL REMINDERS") {
                                    reminderRows.forEachIndexed { idx, key ->
                                        when (key) {
                                            "notif_off" -> ExpressiveSectionRow(
                                                index = idx,
                                                count = reminderRows.size,
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.NotificationsOff,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(Modifier.width(10.dp))
                                                        Text(
                                                            text = "Notifications are off for SubCycler.",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                    }
                                                    if (onOpenNotificationSettings != null) {
                                                        TextButton(onClick = onOpenNotificationSettings) {
                                                            Text(
                                                                "Enable",
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onErrorContainer
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            "lead_days" -> ExpressiveSectionRow(index = idx, count = reminderRows.size) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(
                                                        text = "Remind me before renewal",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    val leadOptions = remember {
                                                        listOf(1, 3, 5, 7).map { ExpressiveSegmentOption(it, "${it}d") }
                                                    }
                                                    ExpressiveSegmentedGroup(
                                                        options = leadOptions,
                                                        selectedValue = notificationLeadDays,
                                                        onSelect = onNotificationLeadDaysSelected!!,
                                                        height = 42.dp,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }

                                            "preview_notif" -> SettingsVaultActionRow(
                                                index = idx,
                                                count = reminderRows.size,
                                                title = "Preview expressive alerts",
                                                subtitle = "Send a sample Cycle Rhythm card & 2-step progress alert",
                                                icon = Icons.Default.NotificationsActive,
                                                badgeShape = ExpressiveCookie8Shape,
                                                palette = skyPalette,
                                                onClick = onPreviewNotifications!!
                                            )
                                        }
                                    }
                                }
                            }

                            val calendarRowCount = if (enabled) 2 else 1
                            ExpressiveSectionStack(eyebrow = "CALENDAR AWARENESS") {
                                ExpressiveSectionRow(index = 0, count = calendarRowCount) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .expressivePolygonSurface(
                                                    shape = ExpressiveCookie8Shape,
                                                    color = skyPalette.container
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = skyPalette.content,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Calendar-aware suggestions",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Use selected device calendars for timely subscription suggestions.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = onEnabledChange
                                        )
                                    }
                                }

                                if (enabled) {
                                    ExpressiveSectionRow(
                                        index = 1,
                                        count = calendarRowCount,
                                        onClick = onOpenCalendars
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    "Calendars",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "$selectedCalendarCount selected",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Change calendars",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            accessIssue?.let {
                                Text(
                                    text = when (it) {
                                        CalendarAccessIssue.PERMISSION_DENIED -> "Calendar permission was not granted. The feature remains off."
                                        CalendarAccessIssue.PERMISSION_REVOKED -> "Calendar access was removed. The feature was turned off."
                                        CalendarAccessIssue.NO_ACCESSIBLE_CALENDARS -> "No selected device calendars are currently accessible."
                                        CalendarAccessIssue.PROVIDER_UNAVAILABLE -> "Calendar data is currently unavailable. The feature was turned off."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        SettingsPage.TRANSFER -> {
                            val transferActions = buildList {
                                if (onShareStack != null) {
                                    add(Triple("Share stack", "QR Stack Card, short link, or backup file", onShareStack))
                                }
                                if (onImportStack != null) {
                                    add(Triple("Import stack", "From a Stack Card image, short link, or file", onImportStack))
                                }
                            }
                            if (transferActions.isNotEmpty()) {
                                ExpressiveSectionStack(eyebrow = "OFFLINE TRANSFER") {
                                    transferActions.forEachIndexed { index, (title, subtitle, onClick) ->
                                        val (icon, shape, palette) = if (title == "Share stack") {
                                            Triple(Icons.Default.Share, ExpressiveCookie8Shape, skyPalette)
                                        } else {
                                            Triple(Icons.Default.Download, ExpressiveArchShape, oceanPalette)
                                        }
                                        SettingsVaultActionRow(
                                            index = index,
                                            count = transferActions.size,
                                            title = title,
                                            subtitle = subtitle,
                                            icon = icon,
                                            badgeShape = shape,
                                            palette = palette,
                                            onClick = onClick
                                        )
                                    }
                                }
                            }
                            if (!backupMessage.isNullOrBlank()) {
                                Text(
                                    text = backupMessage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                            Text(
                                text = "100% offline — SubCycler never uses internet permission or cloud accounts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyPicker && onCurrencySelected != null) {
        CurrencyPickerDialog(
            selectedCurrencyCode = currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { code ->
                showCurrencyPicker = false
                if (code != currencyCode) {
                    pendingCurrencyChange = code
                }
            }
        )
    }

    pendingCurrencyChange?.let { targetCode ->
        if (onCurrencySelected != null) {
            AlertDialog(
                onDismissRequest = { pendingCurrencyChange = null },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                title = {
                    Text(
                        text = "Switch app currency to $targetCode?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    val symbol = MoneyFormatter.symbol(targetCode)
                    val targetDisplay = if (symbol.equals(targetCode, ignoreCase = true)) {
                        targetCode
                    } else {
                        "$targetCode ($symbol)"
                    }
                    Text(
                        text = if (hasExistingSubscriptions) {
                            "SubCycler works 100% offline and does not convert currencies automatically. All existing subscriptions will switch to $targetDisplay with their current numbers unchanged — please update each service's price manually."
                        } else {
                            "SubCycler works 100% offline and does not convert currencies automatically. Prices are not converted when switching currencies and should be entered or updated manually."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                dismissButton = {
                    TextButton(onClick = { pendingCurrencyChange = null }) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val chosen = targetCode
                            pendingCurrencyChange = null
                            onCurrencySelected(chosen)
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Switch to $targetCode", fontWeight = FontWeight.ExtraBold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsVaultActionRow(
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
        onClick = onClick
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
            Spacer(Modifier.width(12.dp))
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
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSelectionSheet(
    calendars: List<DeviceCalendar>,
    initiallySelectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember(calendars, initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Choose calendars",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Only events from calendars you select here are checked. Android's permission itself covers device calendars broadly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(calendars, key = { _, cal -> cal.id }) { index, calendar ->
                    val selected = calendar.id in selectedIds
                    ExpressiveSectionRow(
                        index = index,
                        count = calendars.size,
                        onClick = {
                            selectedIds = if (selected) selectedIds - calendar.id else selectedIds + calendar.id
                        },
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + calendar.id else selectedIds - calendar.id
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    calendar.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (calendar.accountContext.isNotBlank()) {
                                    Text(
                                        calendar.accountContext,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
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
                    onClick = { onConfirm(selectedIds) },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Use selected", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun CalendarSuggestionCard(
    suggestion: CalendarAwareSuggestion,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lavenderPalette = ExpressivePalettes.softLavender()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = lavenderPalette.container
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .expressivePolygonSurface(
                            shape = ExpressiveClover4Shape,
                            color = lavenderPalette.selectedContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = lavenderPalette.content,
                        size = 17.dp,
                        motion = ReactiveMotionType.POP_BOUNCE,
                        triggerKey = suggestion.identity,
                        animateOnFirstComposition = true
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Calendar suggestion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = lavenderPalette.content
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                suggestion.message,
                style = MaterialTheme.typography.bodySmall,
                color = lavenderPalette.content.copy(alpha = 0.85f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = lavenderPalette.content.copy(alpha = 0.72f))
                }
                TextButton(onClick = onPrimaryAction) {
                    Text(suggestion.primaryActionLabel, color = lavenderPalette.content, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

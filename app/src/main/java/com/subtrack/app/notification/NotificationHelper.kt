package com.subtrack.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.subtrack.app.MainActivity
import com.subtrack.app.R
import com.subtrack.app.receiver.SubscriptionActionReceiver
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.ui.theme.launcherAccentTokensFor
import java.time.LocalDate

object NotificationHelper {
    const val CHANNEL_ID = "subtrack_alerts_channel"
    private const val CHANNEL_NAME = "SubCycler Rotation Alerts"
    private const val CHANNEL_DESC = "Local reminders for subscription cancellation deadlines and re-subscription dates"

    const val PROGRESS_CHANNEL_ID = "subtrack_action_progress_channel_v2"
    private const val LEGACY_PROGRESS_CHANNEL_ID = "subtrack_action_progress_channel"
    private const val PROGRESS_CHANNEL_NAME = "Active Subscribe & Unsubscribe Progress"
    private const val PROGRESS_CHANNEL_DESC = "Silent active notification while you complete subscribing or unsubscribing"

    const val CALENDAR_NOTIFICATION_ID = 2_000_001
    const val ACTION_PROGRESS_NOTIFICATION_ID = 3_000_001
    const val PREVIEW_REMINDER_NOTIFICATION_ID = 4_000_001
    const val PREVIEW_PROGRESS_NOTIFICATION_ID = 4_000_002
    const val EXTRA_OPEN_SUBSCRIPTION_ID = "com.subtrack.app.extra.OPEN_SUBSCRIPTION_ID"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(alertChannel)

            notificationManager.deleteNotificationChannel(LEGACY_PROGRESS_CHANNEL_ID)
            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                PROGRESS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = PROGRESS_CHANNEL_DESC
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(progressChannel)
        }
    }

    /**
     * Rich Material 3 Expressive renewal / break notification featuring:
     * - Organic harmonized service monogram LargeIcon + selected App Icon accent color
     * - Expanded Cycle Rhythm Card (`BigPictureStyle`) with wavy active + hatched break track & scalloped savings seal
     * - 1-tap inline shade actions (`Already cancelled`, `Keep (+30d)`, or `Bank ¥X & Resume`)
     */
    fun showCycleReminderNotification(
        context: Context,
        cycleStatus: CycleStatus,
        notificationId: Int,
        title: String,
        message: String,
        launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
        themeMode: AppThemeMode = AppThemeMode.SYSTEM,
        isPreview: Boolean = false
    ) {
        val sub = cycleStatus.subscription
        val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)
        val accent = launcherAccentTokensFor(launcherIcon, isDark)
        val accentArgb = ExpressiveNotificationArt.toArgb(accent.primary)
        val isBlackout = cycleStatus.phase == CyclePhase.BLACKOUT

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!isPreview && sub.id > 0L) {
                putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, sub.id)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = runCatching {
            ExpressiveNotificationArt.renderServiceBadgeBitmap(
                context = context,
                serviceName = sub.name,
                colorHex = sub.colorHex,
                shape = if (isBlackout) NotificationBadgeShape.SUNNY_12 else NotificationBadgeShape.COOKIE_9,
                isDark = isDark,
                launcherIcon = launcherIcon
            )
        }.getOrNull()

        val bannerBitmap = runCatching {
            ExpressiveNotificationArt.renderCycleRhythmBannerBitmap(
                context = context,
                cycleStatus = cycleStatus,
                launcherIcon = launcherIcon,
                isDark = isDark
            )
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setColor(accentArgb)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }
        if (bannerBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(message)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }

        // Attach 1-tap inline shade actions
        if (isBlackout) {
            val bankLabel = if (cycleStatus.perBreakSavings > 0.5) {
                val formatted = MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency, maximumFractionDigits = 0)
                "Bank $formatted & Resume"
            } else {
                "Resume today"
            }
            builder.addAction(
                0,
                bankLabel,
                buildReceiverActionIntent(
                    context = context,
                    requestCode = notificationId + 11,
                    actionName = SubscriptionActionReceiver.ACTION_BANK_AND_RESUME,
                    subscriptionId = sub.id,
                    notificationId = notificationId,
                    isPreview = isPreview
                )
            )
            builder.addAction(0, "Open", contentPendingIntent)
        } else {
            builder.addAction(
                0,
                "Already cancelled",
                buildReceiverActionIntent(
                    context = context,
                    requestCode = notificationId + 21,
                    actionName = SubscriptionActionReceiver.ACTION_MARK_CANCELLED,
                    subscriptionId = sub.id,
                    notificationId = notificationId,
                    isPreview = isPreview
                )
            )
            builder.addAction(
                0,
                "Keep (+${sub.activeDays}d)",
                buildReceiverActionIntent(
                    context = context,
                    requestCode = notificationId + 22,
                    actionName = SubscriptionActionReceiver.ACTION_SKIP_BREAK,
                    subscriptionId = sub.id,
                    notificationId = notificationId,
                    isPreview = isPreview
                )
            )
            builder.addAction(0, "Open", contentPendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted on Android 13+.
        }
    }

    /**
     * Fallback text notification (preserving compatibility for generic alerts without a live Subscription row).
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        subscriptionId: Long? = null,
        launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
        themeMode: AppThemeMode = AppThemeMode.SYSTEM
    ) {
        val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)
        val accentArgb = ExpressiveNotificationArt.toArgb(launcherAccentTokensFor(launcherIcon, isDark).primary)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (subscriptionId != null && subscriptionId > 0L) {
                putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, subscriptionId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setColor(accentArgb)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted on Android 13+.
        }
    }

    fun showActionProgressNotification(
        context: Context,
        action: PendingSubscriptionAction,
        serviceColorHex: String? = null,
        launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
        themeMode: AppThemeMode = AppThemeMode.SYSTEM,
        notificationId: Int = ACTION_PROGRESS_NOTIFICATION_ID,
        isPreview: Boolean = false
    ) {
        val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)
        val accentArgb = ExpressiveNotificationArt.toArgb(launcherAccentTokensFor(launcherIcon, isDark).primary)
        val isUnsubscribe = action.actionType == SubscriptionActionProgressType.UNSUBSCRIBE
        val title = if (isUnsubscribe) {
            "Unsubscribing from ${action.subscriptionName}"
        } else {
            "Subscribing to ${action.subscriptionName}"
        }
        val message = if (isUnsubscribe) {
            "Step 1 of 2 · Finish on provider, then tap Completed."
        } else {
            "Step 1 of 2 · Complete checkout, then tap Completed."
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!isPreview && action.subscriptionId > 0L) {
                putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, action.subscriptionId)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            this.action = SubscriptionActionReceiver.ACTION_COMPLETE_PROGRESS
            putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, action.subscriptionId)
            putExtra(SubscriptionActionReceiver.EXTRA_ACTION_TYPE, action.actionType.name)
            putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SubscriptionActionReceiver.EXTRA_IS_PREVIEW, isPreview)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            this.action = SubscriptionActionReceiver.ACTION_CANCEL_PROGRESS
            putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, action.subscriptionId)
            putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SubscriptionActionReceiver.EXTRA_IS_PREVIEW, isPreview)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = runCatching {
            ExpressiveNotificationArt.renderServiceBadgeBitmap(
                context = context,
                serviceName = action.subscriptionName,
                colorHex = serviceColorHex,
                shape = NotificationBadgeShape.SQUIRCLE,
                isDark = isDark,
                launcherIcon = launcherIcon
            )
        }.getOrNull()

        val progressBanner = runCatching {
            ExpressiveNotificationArt.renderActionProgressBannerBitmap(
                context = context,
                action = action,
                serviceColorHex = serviceColorHex,
                launcherIcon = launcherIcon,
                isDark = isDark
            )
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setColor(accentArgb)
            .setContentTitle(title)
            .setContentText(message)
            .setProgress(2, 1, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(!isPreview)
            .setAutoCancel(isPreview)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Completed", completePendingIntent)
            .addAction(0, "Cancel", cancelPendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }
        if (progressBanner != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(progressBanner)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(message)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // DataStore persistence handles recovery on next launch.
        }
    }

    fun dismissActionProgressNotification(
        context: Context,
        notificationId: Int = ACTION_PROGRESS_NOTIFICATION_ID
    ) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Sends safe, non-mutating sample expressive notifications (a Cycle Rhythm Reminder + a 2-Step
     * Action Progress notification) so users can preview their shade design directly from Settings.
     */
    fun showExpressiveNotificationPreview(
        context: Context,
        sampleSubscription: Subscription?,
        currencyCode: String,
        launcherIcon: AppLauncherIcon,
        themeMode: AppThemeMode
    ) {
        createNotificationChannel(context)
        val today = LocalDate.now()
        val demoSub = (sampleSubscription ?: Subscription(
            id = 0L,
            name = "Netflix",
            cost = if (currencyCode == "JPY") 1590.0 else 15.49,
            currency = currencyCode,
            startDate = today.minusDays(28),
            strategy = RotationStrategy.ROTATION_45_DAYS,
            colorHex = "#E50914"
        )).let { base ->
            // Position preview at 2 days left in an active rotating cycle so the rhythm card shines
            base.copy(
                startDate = today.minusDays((base.activeDays - 2).coerceAtLeast(1).toLong()),
                strategy = if (base.isRotating) base.strategy else RotationStrategy.ROTATION_45_DAYS,
                isBillingDateConfirmed = true,
                isStrategyConfirmed = true,
                isAutoRenewCancelled = false
            )
        }
        val status = RotationEngine.calculateCycleStatus(demoSub, today)
        val annualSave = MoneyFormatter.format(status.annualSavings, demoSub.currency, maximumFractionDigits = 0)
        val endDate = RotationEngine.formatReadableDate(status.phaseEndDate)

        showCycleReminderNotification(
            context = context,
            cycleStatus = status,
            notificationId = PREVIEW_REMINDER_NOTIFICATION_ID,
            title = "Cancel auto-renew: ${demoSub.name}",
            message = "Ends in 2 days ($endDate) · Turn off auto-renew to save ~$annualSave/yr.",
            launcherIcon = launcherIcon,
            themeMode = themeMode,
            isPreview = true
        )

        showActionProgressNotification(
            context = context,
            action = PendingSubscriptionAction(
                subscriptionId = demoSub.id,
                subscriptionName = demoSub.name,
                actionType = SubscriptionActionProgressType.UNSUBSCRIBE
            ),
            serviceColorHex = demoSub.colorHex,
            launcherIcon = launcherIcon,
            themeMode = themeMode,
            notificationId = PREVIEW_PROGRESS_NOTIFICATION_ID,
            isPreview = true
        )
    }

    fun showCalendarSuggestion(
        context: Context,
        subscriptionId: Long,
        subscriptionName: String? = null,
        colorHex: String? = null,
        launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
        themeMode: AppThemeMode = AppThemeMode.SYSTEM
    ) {
        val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)
        val accentArgb = ExpressiveNotificationArt.toArgb(launcherAccentTokensFor(launcherIcon, isDark).primary)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SUBSCRIPTION_ID, subscriptionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            CALENDAR_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (!subscriptionName.isNullOrBlank()) {
            "Review $subscriptionName before travel"
        } else {
            "Subscription review"
        }
        val message = "A calendar event overlaps an upcoming subscription decision. Open SubCycler to review."
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_subcycler)
            .setColor(accentArgb)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (!subscriptionName.isNullOrBlank()) {
            runCatching {
                ExpressiveNotificationArt.renderServiceBadgeBitmap(
                    context = context,
                    serviceName = subscriptionName,
                    colorHex = colorHex,
                    shape = NotificationBadgeShape.COOKIE_9,
                    isDark = isDark,
                    launcherIcon = launcherIcon
                )
            }.getOrNull()?.let { builder.setLargeIcon(it) }
        }

        try {
            NotificationManagerCompat.from(context).notify(CALENDAR_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Calendar opt-in never substitutes for notification permission.
        }
    }

    private fun buildReceiverActionIntent(
        context: Context,
        requestCode: Int,
        actionName: String,
        subscriptionId: Long,
        notificationId: Int,
        isPreview: Boolean
    ): PendingIntent {
        val intent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            action = actionName
            putExtra(SubscriptionActionReceiver.EXTRA_SUBSCRIPTION_ID, subscriptionId)
            putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SubscriptionActionReceiver.EXTRA_IS_PREVIEW, isPreview)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

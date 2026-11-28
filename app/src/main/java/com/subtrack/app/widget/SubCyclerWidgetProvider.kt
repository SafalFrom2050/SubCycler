package com.subtrack.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.subtrack.app.MainActivity
import com.subtrack.app.R
import com.subtrack.app.notification.ExpressiveNotificationArt
import com.subtrack.app.notification.NotificationBadgeShape
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.data.local.AppDatabase
import com.subtrack.core.data.local.mapper.toDomain
import com.subtrack.core.data.preferences.UserPreferencesRepositoryImpl
import com.subtrack.core.data.preferences.userDataStore
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 100% offline **Native `RemoteViews` Living Stack Pulse** Home-Screen Widget:
 * Uses real native Android `TextView`s, Android 12+ `@android:id/background` dynamic system corner
 * radii, native touch ripples, and shape-only expressive accents across `2×2`, `4×2`, and `4×3`.
 */
class SubCyclerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val subs = AppDatabase.getDatabase(context).subscriptionDao().getAllSubscriptionsList().toDomain()
                val prefs = runCatching {
                    UserPreferencesRepositoryImpl(
                        dataStore = context.userDataStore,
                        defaultCurrencyCode = MoneyFormatter.currencyCodeForLocale()
                    ).userPreferences.first()
                }.getOrNull()

                val preferredCurrency = prefs?.currency ?: MoneyFormatter.currencyCodeForLocale()
                val launcherIcon = prefs?.launcherIcon ?: AppLauncherIcon.BOTANICAL
                val useStackMood = prefs?.useStackHeroMood ?: true
                val themeMode = prefs?.themeMode ?: AppThemeMode.SYSTEM
                val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)

                val today = LocalDate.now()
                val statuses = subs.map { RotationEngine.calculateCycleStatus(it, today) }
                val currencies = statuses.map { it.subscription.currency }.distinct()
                val primaryCurrency = when {
                    currencies.isEmpty() -> preferredCurrency
                    currencies.size == 1 -> currencies.single()
                    preferredCurrency in currencies -> preferredCurrency
                    else -> currencies.first()
                }
                val summaryStatuses = statuses.filter { it.subscription.currency == primaryCurrency }
                val summary = RotationEngine.calculatePortfolioSavings(summaryStatuses)

                val activeForMood = statuses
                    .filter { !it.subscription.isIncomplete && it.phase == CyclePhase.ACTIVE }
                    .map { it.subscription }

                val upcoming = statuses
                    .filterNot { it.subscription.isIncomplete }
                    .sortedWith(
                        compareBy<CycleStatus> {
                            when (it.nextAction.urgency) {
                                ActionUrgency.ACTION_DUE -> 0
                                ActionUrgency.WARNING -> 1
                                ActionUrgency.INFO -> 2
                            }
                        }.thenBy { it.daysRemainingInPhase }
                    )

                appWidgetIds.forEach { widgetId ->
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                    val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                    val tier = ExpressiveWidgetArt.classifySizeTier(minW, minH)

                    val views = buildNativeLivingViews(
                        context = context,
                        widgetId = widgetId,
                        tier = tier,
                        summary = summary,
                        upcoming = upcoming,
                        activeForMood = activeForMood,
                        currencyCode = primaryCurrency,
                        launcherIcon = launcherIcon,
                        useStackMood = useStackMood,
                        isDark = isDark
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal fun buildNativeLivingViews(
        context: Context,
        widgetId: Int,
        tier: LivingWidgetSizeTier,
        summary: PortfolioSummary,
        upcoming: List<CycleStatus>,
        activeForMood: List<Subscription>,
        currencyCode: String,
        launcherIcon: AppLauncherIcon,
        useStackMood: Boolean,
        isDark: Boolean
    ): RemoteViews {
        val accent = BrandColorHarmonizer.resolveEffectiveAccentTokens(
            activeSubscriptions = activeForMood,
            useStackMood = useStackMood,
            isDark = isDark,
            fallbackIcon = launcherIcon
        )
        val heroMood = BrandColorHarmonizer.resolveHeroMoodPalette(
            activeSubscriptions = activeForMood,
            useStackMood = useStackMood,
            isDark = isDark,
            fallbackIcon = launcherIcon
        )

        val inkPrimary = ExpressiveNotificationArt.toArgb(heroMood.contentColor)
        val inkSecondary = ExpressiveNotificationArt.toArgb(heroMood.contentColor, 208)
        val accentPrimary = ExpressiveNotificationArt.toArgb(accent.primary)
        val sealPrimary = ExpressiveNotificationArt.toArgb(heroMood.sealColor)
        val sealOnPrimary = ExpressiveNotificationArt.toArgb(heroMood.onSealColor)

        val hasSavings = summary.totalAnnualSavings > 0.5
        val savingsAmount = MoneyFormatter.format(summary.totalAnnualSavings, currencyCode, maximumFractionDigits = 0)
        val monthlyFormatted = if (summary.totalSubscriptions == 0) {
            "Add stack"
        } else {
            MoneyFormatter.format(summary.totalMonthlyOptimizedCost, currencyCode)
        }
        val soonest = upcoming.firstOrNull()

        val heroBackdrop = ExpressiveWidgetArt.renderHeroMoodBackdropBitmap(
            context = context,
            activeSubscriptionsForMood = activeForMood,
            useStackMood = useStackMood,
            isDark = isDark,
            launcherIcon = launcherIcon
        )
        val swirlBmp = ExpressiveWidgetArt.renderSwirlBadgeBitmap(
            activeSubscriptionsForMood = activeForMood,
            useStackMood = useStackMood,
            launcherIcon = launcherIcon
        )

        val openDashboardIntent = buildOpenIntent(
            context = context,
            requestCode = WIDGET_REQUEST_CODE + widgetId * 10,
            subscriptionId = null
        )

        return when (tier) {
            LivingWidgetSizeTier.COMPACT_2X2 -> RemoteViews(context.packageName, R.layout.widget_living_compact).apply {
                setImageViewBitmap(R.id.widget_hero_backdrop, heroBackdrop)
                setImageViewBitmap(R.id.widget_swirl, swirlBmp)
                setOnClickPendingIntent(android.R.id.background, openDashboardIntent)

                val pillText = when {
                    summary.totalSubscriptions == 0 -> "Offline"
                    summary.blackoutCount > 0 -> "${summary.activeCount} on · ${summary.blackoutCount} off"
                    else -> "${summary.activeCount} active"
                }
                val pillRgb = if (isDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                setInt(R.id.widget_status_pill_bg, "setColorFilter", pillRgb)
                setInt(R.id.widget_status_pill_bg, "setImageAlpha", if (isDark) 76 else 58)
                setTextViewText(R.id.widget_status_pill_text, pillText)
                setTextColor(R.id.widget_status_pill_text, inkPrimary)

                setTextViewText(R.id.widget_eyebrow, if (hasSavings) "EFFECTIVE MONTHLY" else "MONTHLY SPEND")
                setTextColor(R.id.widget_eyebrow, inkSecondary)
                setTextViewText(R.id.widget_monthly_amount, monthlyFormatted)
                setTextColor(R.id.widget_monthly_amount, inkPrimary)
                setTextViewTextSize(
                    R.id.widget_monthly_amount,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (monthlyFormatted.length > 7) 21f else 24f
                )

                val nextCaption = when {
                    soonest == null -> "100% offline"
                    soonest.phase == CyclePhase.BLACKOUT -> "${soonest.subscription.name} · ${soonest.daysRemainingInPhase}d off"
                    else -> "${soonest.subscription.name} · ${soonest.daysRemainingInPhase}d"
                }
                setTextViewText(R.id.widget_next_caption, nextCaption)
                setTextColor(R.id.widget_next_caption, inkPrimary)

                if (soonest != null) {
                    val strip = ExpressiveWidgetArt.renderMiniRhythmStripBitmap(
                        status = soonest,
                        accentArgb = sealPrimary,
                        isDark = true
                    )
                    setViewVisibility(R.id.widget_rhythm_strip, View.VISIBLE)
                    setImageViewBitmap(R.id.widget_rhythm_strip, strip)
                } else {
                    setViewVisibility(R.id.widget_rhythm_strip, View.GONE)
                }

                bindSavingsSeal(
                    views = this,
                    hasSavings = hasSavings,
                    savingsAmount = savingsAmount,
                    accentPrimary = sealPrimary,
                    accentOnPrimary = sealOnPrimary,
                    compact = true
                )
            }

            LivingWidgetSizeTier.WIDE_4X2 -> RemoteViews(context.packageName, R.layout.widget_living_wide).apply {
                setImageViewBitmap(R.id.widget_hero_backdrop, heroBackdrop)
                setImageViewBitmap(R.id.widget_swirl, swirlBmp)
                setOnClickPendingIntent(R.id.widget_hero_section, openDashboardIntent)

                bindHeroSummaryText(
                    views = this,
                    summary = summary,
                    monthlyFormatted = monthlyFormatted,
                    currencyCode = currencyCode,
                    hasSavings = hasSavings,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    accentPrimary = sealPrimary
                )
                bindSavingsSeal(
                    views = this,
                    hasSavings = hasSavings,
                    savingsAmount = savingsAmount,
                    accentPrimary = sealPrimary,
                    accentOnPrimary = sealOnPrimary,
                    compact = false
                )

                val cardBgRgb = if (isDark) 0xFF14181A.toInt() else 0xFFFFFFFF.toInt()
                setInt(R.id.widget_next_card_bg, "setColorFilter", cardBgRgb)
                setInt(R.id.widget_next_card_bg, "setImageAlpha", if (isDark) 196 else 228)

                val rowPrimary = if (isDark) 0xFFECEFEE.toInt() else 0xFF151918.toInt()
                val rowSecondary = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()

                if (soonest != null) {
                    val sub = soonest.subscription
                    val badge = ExpressiveNotificationArt.renderServiceBadgeBitmap(
                        context = context,
                        serviceName = sub.name,
                        colorHex = sub.colorHex,
                        shape = if (soonest.phase == CyclePhase.BLACKOUT) NotificationBadgeShape.SUNNY_12 else NotificationBadgeShape.COOKIE_9,
                        isDark = isDark,
                        launcherIcon = launcherIcon,
                        accentOverride = accent,
                        sizePx = 96
                    )
                    setViewVisibility(R.id.widget_next_badge, View.VISIBLE)
                    setImageViewBitmap(R.id.widget_next_badge, badge)

                    val nextLabel = when {
                        soonest.phase == CyclePhase.ACTIVE && sub.isRotating && !soonest.isAutoRenewCancelledForCurrentCycle ->
                            "Cancel ${sub.name} in ${soonest.daysRemainingInPhase}d"
                        soonest.phase == CyclePhase.BLACKOUT ->
                            "Resume ${sub.name} in ${soonest.daysRemainingInPhase}d"
                        else -> "${sub.name} · ${soonest.daysRemainingInPhase}d left"
                    }
                    val subCaption = if (sub.isRotating) {
                        "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                    } else {
                        "Always on · ${sub.billingPeriod.name.lowercase()}"
                    }
                    setTextViewText(R.id.widget_next_title, nextLabel)
                    setTextColor(R.id.widget_next_title, rowPrimary)
                    setViewVisibility(R.id.widget_next_sub, View.VISIBLE)
                    setTextViewText(R.id.widget_next_sub, subCaption)
                    setTextColor(R.id.widget_next_sub, rowSecondary)

                    val strip = ExpressiveWidgetArt.renderMiniRhythmStripBitmap(
                        status = soonest,
                        accentArgb = accentPrimary,
                        isDark = isDark
                    )
                    setViewVisibility(R.id.widget_rhythm_strip, View.VISIBLE)
                    setImageViewBitmap(R.id.widget_rhythm_strip, strip)

                    setOnClickPendingIntent(
                        R.id.widget_next_card,
                        buildOpenIntent(
                            context = context,
                            requestCode = WIDGET_REQUEST_CODE + widgetId * 10 + 1,
                            subscriptionId = sub.id
                        )
                    )
                } else {
                    setViewVisibility(R.id.widget_next_badge, View.GONE)
                    setViewVisibility(R.id.widget_next_sub, View.GONE)
                    setViewVisibility(R.id.widget_rhythm_strip, View.GONE)
                    setTextViewText(R.id.widget_next_title, "Tap to add your first subscription offline")
                    setTextColor(R.id.widget_next_title, rowSecondary)
                    setOnClickPendingIntent(R.id.widget_next_card, openDashboardIntent)
                }
            }

            LivingWidgetSizeTier.TALL_4X3 -> RemoteViews(context.packageName, R.layout.widget_living_tall).apply {
                val outerSurface = if (isDark) 0xFF121618.toInt() else 0xFFF3F5F4.toInt()
                setInt(R.id.widget_surface_bg, "setColorFilter", outerSurface)
                setInt(R.id.widget_surface_bg, "setImageAlpha", 255)

                setImageViewBitmap(R.id.widget_hero_backdrop, heroBackdrop)
                setImageViewBitmap(R.id.widget_swirl, swirlBmp)
                setOnClickPendingIntent(R.id.widget_hero_section, openDashboardIntent)

                bindHeroSummaryText(
                    views = this,
                    summary = summary,
                    monthlyFormatted = monthlyFormatted,
                    currencyCode = currencyCode,
                    hasSavings = hasSavings,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    accentPrimary = sealPrimary
                )
                bindSavingsSeal(
                    views = this,
                    hasSavings = hasSavings,
                    savingsAmount = savingsAmount,
                    accentPrimary = sealPrimary,
                    accentOnPrimary = sealOnPrimary,
                    compact = false
                )

                val visible = upcoming.take(3)
                val rowSpecs = listOf(
                    TallRowIds(
                        root = R.id.widget_row_0,
                        bg = R.id.widget_row_0_bg,
                        badge = R.id.widget_row_0_badge,
                        name = R.id.widget_row_0_name,
                        sub = R.id.widget_row_0_sub,
                        pillBg = R.id.widget_row_0_pill_bg,
                        pillText = R.id.widget_row_0_pill_text
                    ),
                    TallRowIds(
                        root = R.id.widget_row_1,
                        bg = R.id.widget_row_1_bg,
                        badge = R.id.widget_row_1_badge,
                        name = R.id.widget_row_1_name,
                        sub = R.id.widget_row_1_sub,
                        pillBg = R.id.widget_row_1_pill_bg,
                        pillText = R.id.widget_row_1_pill_text
                    ),
                    TallRowIds(
                        root = R.id.widget_row_2,
                        bg = R.id.widget_row_2_bg,
                        badge = R.id.widget_row_2_badge,
                        name = R.id.widget_row_2_name,
                        sub = R.id.widget_row_2_sub,
                        pillBg = R.id.widget_row_2_pill_bg,
                        pillText = R.id.widget_row_2_pill_text
                    )
                )

                val rowCardRgb = if (isDark) 0xFF1B2023.toInt() else 0xFFFFFFFF.toInt()
                val rowNameColor = if (isDark) 0xFFECEFEE.toInt() else 0xFF151918.toInt()
                val rowSubColor = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()

                if (visible.isEmpty()) {
                    setViewVisibility(R.id.widget_empty_rows_text, View.VISIBLE)
                    setTextColor(R.id.widget_empty_rows_text, rowSubColor)
                    setOnClickPendingIntent(R.id.widget_empty_rows_text, openDashboardIntent)
                    rowSpecs.forEach { setViewVisibility(it.root, View.GONE) }
                } else {
                    setViewVisibility(R.id.widget_empty_rows_text, View.GONE)
                    rowSpecs.forEachIndexed { idx, ids ->
                        val status = visible.getOrNull(idx)
                        if (status == null) {
                            setViewVisibility(ids.root, View.GONE)
                        } else {
                            setViewVisibility(ids.root, View.VISIBLE)
                            val maskDrawable = when {
                                visible.size == 1 -> R.drawable.bg_widget_row_single
                                idx == 0 -> R.drawable.bg_widget_row_top
                                idx == visible.lastIndex -> R.drawable.bg_widget_row_bottom
                                else -> R.drawable.bg_widget_row_mid
                            }
                            setImageViewResource(ids.bg, maskDrawable)
                            setInt(ids.bg, "setColorFilter", rowCardRgb)
                            setInt(ids.bg, "setImageAlpha", 255)

                            val sub = status.subscription
                            val palette = BrandColorHarmonizer.harmonizedTonalPalette(sub.name, sub.colorHex, isDark)
                            val badge = ExpressiveNotificationArt.renderServiceBadgeBitmap(
                                context = context,
                                serviceName = sub.name,
                                colorHex = sub.colorHex,
                                shape = if (status.phase == CyclePhase.BLACKOUT) NotificationBadgeShape.SUNNY_12 else NotificationBadgeShape.COOKIE_9,
                                isDark = isDark,
                                launcherIcon = launcherIcon,
                                accentOverride = accent,
                                sizePx = 96
                            )
                            setImageViewBitmap(ids.badge, badge)

                            setTextViewText(ids.name, sub.name)
                            setTextColor(ids.name, rowNameColor)

                            val priceStr = MoneyFormatter.format(sub.cost, sub.currency)
                            val cadence = if (sub.isRotating) "${sub.totalCycleDays}d cycle" else "Always"
                            setTextViewText(ids.sub, "$priceStr · $cadence")
                            setTextColor(ids.sub, rowSubColor)

                            val pillText = when {
                                status.phase == CyclePhase.BLACKOUT -> "Paused · ${status.daysRemainingInPhase}d"
                                status.daysRemainingInPhase <= 0 -> "Due today"
                                else -> "${status.daysRemainingInPhase}d left"
                            }
                            setInt(ids.pillBg, "setColorFilter", ExpressiveNotificationArt.toArgb(palette.container))
                            setInt(ids.pillBg, "setImageAlpha", if (isDark) 225 else 242)
                            setTextViewText(ids.pillText, pillText)
                            setTextColor(ids.pillText, ExpressiveNotificationArt.toArgb(palette.content))

                            setOnClickPendingIntent(
                                ids.root,
                                buildOpenIntent(
                                    context = context,
                                    requestCode = WIDGET_REQUEST_CODE + widgetId * 10 + idx + 1,
                                    subscriptionId = sub.id
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindHeroSummaryText(
        views: RemoteViews,
        summary: PortfolioSummary,
        monthlyFormatted: String,
        currencyCode: String,
        hasSavings: Boolean,
        inkPrimary: Int,
        inkSecondary: Int,
        accentPrimary: Int
    ) {
        views.setTextViewText(R.id.widget_eyebrow, if (hasSavings) "EFFECTIVE MONTHLY" else "MONTHLY SPEND")
        views.setTextColor(R.id.widget_eyebrow, inkSecondary)

        views.setTextViewText(R.id.widget_monthly_amount, monthlyFormatted)
        views.setTextColor(R.id.widget_monthly_amount, inkPrimary)

        val pctOff = summary.overallSavingsPercentage.roundToInt()
        val subLine = when {
            summary.totalSubscriptions == 0 -> "100% offline rotation"
            hasSavings && pctOff > 0 -> {
                val std = MoneyFormatter.format(summary.totalMonthlyStandardCost, currencyCode)
                "Down from $std/mo · -$pctOff%"
            }
            else -> "${summary.activeCount} active service${if (summary.activeCount == 1) "" else "s"}"
        }
        views.setTextViewText(R.id.widget_savings_subtitle, subLine)
        views.setTextColor(R.id.widget_savings_subtitle, inkSecondary)

        if (hasSavings && summary.totalMonthlyStandardCost > summary.totalMonthlyOptimizedCost) {
            val spendFraction = (summary.totalMonthlyOptimizedCost / summary.totalMonthlyStandardCost).toFloat()
            val waveStrip = ExpressiveWidgetArt.renderWavyEfficiencyStripBitmap(
                spendFraction = spendFraction,
                waveArgb = (inkPrimary and 0x00FFFFFF) or (0xD6 shl 24),
                savedArgb = accentPrimary
            )
            views.setViewVisibility(R.id.widget_efficiency_wave, View.VISIBLE)
            views.setImageViewBitmap(R.id.widget_efficiency_wave, waveStrip)
        } else {
            views.setViewVisibility(R.id.widget_efficiency_wave, View.GONE)
        }
    }

    private fun bindSavingsSeal(
        views: RemoteViews,
        hasSavings: Boolean,
        savingsAmount: String,
        accentPrimary: Int,
        accentOnPrimary: Int,
        compact: Boolean
    ) {
        if (!hasSavings) {
            views.setViewVisibility(R.id.widget_seal_box, View.GONE)
            return
        }
        views.setViewVisibility(R.id.widget_seal_box, View.VISIBLE)
        views.setInt(R.id.widget_seal_bg, "setColorFilter", accentPrimary)
        views.setInt(R.id.widget_seal_bg, "setImageAlpha", 255)
        views.setTextColor(R.id.widget_seal_top, (accentOnPrimary and 0x00FFFFFF) or (0xC8 shl 24))
        views.setTextViewText(R.id.widget_seal_amount, savingsAmount)
        views.setTextColor(R.id.widget_seal_amount, accentOnPrimary)
        val amountSizeSp = when {
            compact && savingsAmount.length >= 6 -> 9.5f
            compact -> 11f
            savingsAmount.length >= 7 -> 11f
            else -> 13f
        }
        views.setTextViewTextSize(R.id.widget_seal_amount, TypedValue.COMPLEX_UNIT_SP, amountSizeSp)
        views.setTextColor(R.id.widget_seal_sub, (accentOnPrimary and 0x00FFFFFF) or (0xC8 shl 24))
    }

    private fun buildOpenIntent(
        context: Context,
        requestCode: Int,
        subscriptionId: Long?
    ): PendingIntent {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (subscriptionId != null && subscriptionId > 0L) {
                putExtra(NotificationHelper.EXTRA_OPEN_SUBSCRIPTION_ID, subscriptionId)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class TallRowIds(
        val root: Int,
        val bg: Int,
        val badge: Int,
        val name: Int,
        val sub: Int,
        val pillBg: Int,
        val pillText: Int
    )

    companion object {
        private const val WIDGET_REQUEST_CODE = 42_001

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SubCyclerWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, SubCyclerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

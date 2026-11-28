package com.subtrack.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import com.subtrack.app.MainActivity
import com.subtrack.app.R
import com.subtrack.app.notification.ExpressiveNotificationArt
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
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 100% offline **Native Next Up Cookie** (`2×2` sculptural countdown widget):
 * Pairs a transparent-corner scalloped cookie / sunny / squircle progress ring shell with
 * crisp native Android `TextView`s on top so countdown typography stays razor-sharp at every size.
 */
class NextUpCookieWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
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

                val launcherIcon = prefs?.launcherIcon ?: AppLauncherIcon.BOTANICAL
                val useStackMood = prefs?.useStackHeroMood ?: true
                val themeMode = prefs?.themeMode ?: AppThemeMode.SYSTEM
                val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)

                val today = LocalDate.now()
                val allStatuses = subs.map { RotationEngine.calculateCycleStatus(it, today) }
                val activeForMood = allStatuses
                    .filter { !it.subscription.isIncomplete && it.phase == CyclePhase.ACTIVE }
                    .map { it.subscription }
                val soonest = allStatuses
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
                    .firstOrNull()

                appWidgetIds.forEach { widgetId ->
                    val views = buildNativeCookieViews(
                        context = context,
                        soonest = soonest,
                        launcherIcon = launcherIcon,
                        useStackMood = useStackMood,
                        activeForMood = activeForMood,
                        isDark = isDark
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal fun buildNativeCookieViews(
        context: Context,
        soonest: CycleStatus?,
        launcherIcon: AppLauncherIcon,
        useStackMood: Boolean = true,
        activeForMood: List<Subscription> = emptyList(),
        isDark: Boolean
    ): RemoteViews {
        val shellBitmap = ExpressiveWidgetArt.renderNextUpCookieShellBitmap(
            soonest = soonest,
            launcherIcon = launcherIcon,
            isDark = isDark,
            useStackMood = useStackMood,
            activeSubscriptionsForMood = activeForMood
        )

        val moodPool = activeForMood.ifEmpty { listOfNotNull(soonest?.subscription) }
        val accent = BrandColorHarmonizer.resolveEffectiveAccentTokens(
            activeSubscriptions = moodPool,
            useStackMood = useStackMood,
            isDark = isDark,
            fallbackIcon = launcherIcon
        )
        val inkArgb = when {
            soonest == null -> ExpressiveNotificationArt.toArgb(accent.onPrimaryContainer)
            soonest.phase == CyclePhase.BLACKOUT && soonest.daysRemainingInPhase <= 1 ->
                ExpressiveNotificationArt.toArgb(accent.onHeroPop)
            !useStackMood -> ExpressiveNotificationArt.toArgb(accent.heroContent)
            else -> {
                val sub = soonest.subscription
                val palette = BrandColorHarmonizer.harmonizedTonalPalette(sub.name, sub.colorHex, isDark)
                ExpressiveNotificationArt.toArgb(palette.content)
            }
        }
        val secondaryInkArgb = (inkArgb and 0x00FFFFFF) or (0xCC shl 24)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            soonest?.subscription?.id?.let { id ->
                putExtra(NotificationHelper.EXTRA_OPEN_SUBSCRIPTION_ID, id)
            }
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            COOKIE_WIDGET_REQUEST_CODE,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return RemoteViews(context.packageName, R.layout.widget_next_up_cookie).apply {
            setImageViewBitmap(R.id.cookie_shell, shellBitmap)
            setOnClickPendingIntent(R.id.cookie_widget_root, tapPendingIntent)

            if (soonest != null) {
                val sub = soonest.subscription
                val countdown = if (soonest.daysRemainingInPhase <= 0) "Today" else "${soonest.daysRemainingInPhase}d"
                val caption = when {
                    soonest.phase == CyclePhase.BLACKOUT && soonest.perBreakSavings > 0.5 -> {
                        val saved = MoneyFormatter.format(soonest.perBreakSavings, sub.currency, maximumFractionDigits = 0)
                        "Bank $saved"
                    }
                    soonest.phase == CyclePhase.BLACKOUT -> "of break"
                    sub.isRotating && !soonest.isAutoRenewCancelledForCurrentCycle -> "until cancel"
                    else -> "remaining"
                }
                setTextViewText(R.id.cookie_service_name, sub.name)
                setTextColor(R.id.cookie_service_name, secondaryInkArgb)

                setTextViewText(R.id.cookie_countdown, countdown)
                setTextColor(R.id.cookie_countdown, inkArgb)
                setTextViewTextSize(
                    R.id.cookie_countdown,
                    TypedValue.COMPLEX_UNIT_SP,
                    when {
                        countdown == "Today" -> 22f
                        countdown.length >= 4 -> 26f
                        else -> 34f
                    }
                )

                setTextViewText(R.id.cookie_caption, caption)
                setTextColor(R.id.cookie_caption, secondaryInkArgb)
            } else {
                setTextViewText(R.id.cookie_service_name, "SUBCYCLER")
                setTextColor(R.id.cookie_service_name, secondaryInkArgb)
                setTextViewText(R.id.cookie_countdown, "+")
                setTextColor(R.id.cookie_countdown, inkArgb)
                setTextViewTextSize(R.id.cookie_countdown, TypedValue.COMPLEX_UNIT_SP, 34f)
                setTextViewText(R.id.cookie_caption, "Tap to add")
                setTextColor(R.id.cookie_caption, secondaryInkArgb)
            }
        }
    }

    companion object {
        private const val COOKIE_WIDGET_REQUEST_CODE = 42_501

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NextUpCookieWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, NextUpCookieWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}

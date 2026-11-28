package com.subtrack.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 100% offline Material 3 Expressive v4.0 **Living Stack Pulse** Home-Screen Widget:
 * Responsive across 2x2 (Compact Seal), 4x2 (Wide Hero + Rhythm Bar), and 4x3 (Hero + clickable
 * top-3 chromatic Cycle Vessel rows), synced live to App Icon accent and Living Stack Mood.
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

                    val artBitmap = ExpressiveWidgetArt.renderLivingStackPulseBitmap(
                        context = context,
                        summary = summary,
                        upcomingStatuses = upcoming,
                        activeSubscriptionsForMood = activeForMood,
                        currencyCode = primaryCurrency,
                        launcherIcon = launcherIcon,
                        useStackMood = useStackMood,
                        isDark = isDark,
                        tier = tier
                    )

                    val openHomePendingIntent = buildOpenIntent(
                        context = context,
                        requestCode = WIDGET_REQUEST_CODE + widgetId * 10,
                        subscriptionId = if (tier != LivingWidgetSizeTier.TALL_4X3) upcoming.firstOrNull()?.subscription?.id else null
                    )

                    val views = if (tier == LivingWidgetSizeTier.TALL_4X3) {
                        RemoteViews(context.packageName, R.layout.widget_subcycler_tall).apply {
                            setImageViewBitmap(R.id.widget_art, artBitmap)
                            setOnClickPendingIntent(R.id.widget_hero_hit, openHomePendingIntent)
                            val rowIds = listOf(R.id.widget_row_hit_0, R.id.widget_row_hit_1, R.id.widget_row_hit_2)
                            rowIds.forEachIndexed { idx, viewId ->
                                val subId = upcoming.getOrNull(idx)?.subscription?.id
                                if (subId != null) {
                                    setViewVisibility(viewId, View.VISIBLE)
                                    setOnClickPendingIntent(
                                        viewId,
                                        buildOpenIntent(
                                            context = context,
                                            requestCode = WIDGET_REQUEST_CODE + widgetId * 10 + idx + 1,
                                            subscriptionId = subId
                                        )
                                    )
                                } else {
                                    setViewVisibility(viewId, View.INVISIBLE)
                                    setOnClickPendingIntent(viewId, openHomePendingIntent)
                                }
                            }
                        }
                    } else {
                        RemoteViews(context.packageName, R.layout.widget_subcycler).apply {
                            setImageViewBitmap(R.id.widget_art, artBitmap)
                            setOnClickPendingIntent(R.id.widget_root, openHomePendingIntent)
                        }
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
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

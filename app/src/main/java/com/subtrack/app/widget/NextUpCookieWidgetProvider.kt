package com.subtrack.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.subtrack.core.domain.model.CycleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 100% offline **Next Up Cookie** (2x2 sculptural non-rectangular countdown widget):
 * Floats an organic scalloped cookie / squircle / sunny burst directly over the user's wallpaper,
 * tinted in the soonest-due subscription's harmonized brand pastel with a perimeter cycle ring.
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
                val themeMode = prefs?.themeMode ?: AppThemeMode.SYSTEM
                val isDark = ExpressiveNotificationArt.resolveDarkTheme(context, themeMode)

                val today = LocalDate.now()
                val soonest = subs
                    .map { RotationEngine.calculateCycleStatus(it, today) }
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

                val cookieBitmap = ExpressiveWidgetArt.renderNextUpCookieBitmap(
                    context = context,
                    soonest = soonest,
                    launcherIcon = launcherIcon,
                    isDark = isDark
                )

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

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.widget_next_up_cookie).apply {
                        setImageViewBitmap(R.id.cookie_art, cookieBitmap)
                        setOnClickPendingIntent(R.id.cookie_widget_root, tapPendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
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

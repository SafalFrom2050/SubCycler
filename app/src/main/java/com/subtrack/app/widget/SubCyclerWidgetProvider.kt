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
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.data.local.AppDatabase
import com.subtrack.core.data.local.mapper.toDomain
import com.subtrack.core.data.preferences.UserPreferencesRepositoryImpl
import com.subtrack.core.data.preferences.userDataStore
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CyclePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 100% offline Home-Screen Widget showing Effective Monthly spend, Active/Paused status,
 * and the soonest upcoming rotation action.
 */
class SubCyclerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val subs = AppDatabase.getDatabase(context).subscriptionDao().getAllSubscriptionsList().toDomain()
                val preferredCurrency = runCatching {
                    UserPreferencesRepositoryImpl(
                        dataStore = context.userDataStore,
                        defaultCurrencyCode = MoneyFormatter.currencyCodeForLocale()
                    ).userPreferences.first().currency
                }.getOrElse { MoneyFormatter.currencyCodeForLocale() }

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

                val soonest = statuses
                    .sortedWith(
                        compareBy<com.subtrack.core.domain.model.CycleStatus> {
                            when (it.nextAction.urgency) {
                                ActionUrgency.ACTION_DUE -> 0
                                ActionUrgency.WARNING -> 1
                                ActionUrgency.INFO -> 2
                            }
                        }.thenBy { it.daysRemainingInPhase }
                    )
                    .firstOrNull()

                val statusPill = when {
                    summary.totalSubscriptions == 0 -> "Empty"
                    summary.blackoutCount > 0 -> "${summary.activeCount} on · ${summary.blackoutCount} paused"
                    else -> "${summary.activeCount} active"
                }

                val headline = if (summary.totalSubscriptions == 0) {
                    "Add your stack"
                } else {
                    "${MoneyFormatter.format(summary.totalMonthlyOptimizedCost, primaryCurrency)}/mo"
                }

                val nextLine = when {
                    soonest == null -> "Tap to track subscriptions offline"
                    soonest.phase == CyclePhase.ACTIVE && soonest.subscription.isRotating && !soonest.isAutoRenewCancelledForCurrentCycle ->
                        "Next: Cancel ${soonest.subscription.name} in ${soonest.daysRemainingInPhase}d"
                    soonest.phase == CyclePhase.BLACKOUT ->
                        "Next: Resume ${soonest.subscription.name} in ${soonest.daysRemainingInPhase}d"
                    else ->
                        "${soonest.subscription.name} · ${soonest.daysRemainingInPhase}d left"
                }

                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    soonest?.subscription?.id?.let { id ->
                        putExtra(NotificationHelper.EXTRA_OPEN_SUBSCRIPTION_ID, id)
                    }
                }
                val tapPendingIntent = PendingIntent.getActivity(
                    context,
                    WIDGET_REQUEST_CODE,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.widget_subcycler).apply {
                        setTextViewText(R.id.widget_status_pill, statusPill)
                        setTextViewText(R.id.widget_headline, headline)
                        setTextViewText(R.id.widget_next_action, nextLine)
                        setOnClickPendingIntent(R.id.widget_root, tapPendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
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

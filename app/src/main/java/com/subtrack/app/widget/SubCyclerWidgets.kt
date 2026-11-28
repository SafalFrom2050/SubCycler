package com.subtrack.app.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.subtrack.app.worker.WorkSchedulerManager

/**
 * Single coordinator that refreshes every installed SubCycler home-screen widget
 * (**Living Stack Pulse** + **Next Up Cookie**) whenever subscriptions, currency,
 * launcher icon accent, theme mode, system dark/light configuration, or Living Stack Mood change.
 */
object SubCyclerWidgets {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun requestUpdateAll(context: Context) {
        val appContext = context.applicationContext
        SubCyclerWidgetProvider.requestUpdate(appContext)
        NextUpCookieWidgetProvider.requestUpdate(appContext)
    }

    /**
     * Re-pushes widget RemoteViews right after `<activity-alias>` toggles in `MainActivity.onStop()`.
     * Android's launcher processes `ACTION_PACKAGE_CHANGED` asynchronously (and may briefly reload
     * `initialLayout` or restart the process), so we trigger immediate + staggered in-process retries
     * backed by a lightweight 1.5s WorkManager refresh that survives process kills.
     */
    fun schedulePostAliasRefresh(context: Context) {
        val appContext = context.applicationContext
        requestUpdateAll(appContext)
        mainHandler.postDelayed({ requestUpdateAll(appContext) }, 400L)
        mainHandler.postDelayed({ requestUpdateAll(appContext) }, 1_100L)
        runCatching {
            WorkSchedulerManager.scheduleDelayedWidgetRefresh(appContext, delayMs = 1_500L)
        }
    }
}

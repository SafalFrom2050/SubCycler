package com.subtrack.app.widget

import android.content.Context

/**
 * Single coordinator that refreshes every installed SubCycler home-screen widget
 * (**Living Stack Pulse** + **Next Up Cookie**) whenever subscriptions, currency,
 * launcher icon accent, theme mode, or Living Stack Mood change.
 */
object SubCyclerWidgets {
    fun requestUpdateAll(context: Context) {
        SubCyclerWidgetProvider.requestUpdate(context)
        NextUpCookieWidgetProvider.requestUpdate(context)
    }
}

package com.subtrack.app.worker

import android.app.NotificationManager
import android.content.Context
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.core.domain.repository.CalendarAwarenessScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidCalendarAwarenessScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarAwarenessScheduler {
    override fun enable() {
        WorkSchedulerManager.scheduleCalendarReconciliation(context)
        WorkSchedulerManager.enqueueImmediateCalendarReconciliation(context)
    }

    override fun disable() {
        WorkSchedulerManager.cancelCalendarReconciliation(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationHelper.CALENDAR_NOTIFICATION_ID)
    }
}

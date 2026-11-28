package com.subtrack.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkSchedulerManager {

    fun scheduleDailyCycleEvaluation(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<DailyCycleEvaluationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyCycleEvaluationWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun enqueueImmediateEvaluation(context: Context) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DailyCycleEvaluationWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${DailyCycleEvaluationWorker.UNIQUE_PERIODIC_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }

    fun scheduleCalendarReconciliation(context: Context) {
        val request = PeriodicWorkRequestBuilder<CalendarReconciliationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CalendarReconciliationWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueImmediateCalendarReconciliation(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            CalendarReconciliationWorker.UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CalendarReconciliationWorker>().build()
        )
    }

    fun cancelCalendarReconciliation(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CalendarReconciliationWorker.UNIQUE_PERIODIC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(CalendarReconciliationWorker.UNIQUE_IMMEDIATE_WORK_NAME)
    }

    fun scheduleDelayedWidgetRefresh(context: Context, delayMs: Long = 1_500L) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(250L), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WidgetRefreshWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

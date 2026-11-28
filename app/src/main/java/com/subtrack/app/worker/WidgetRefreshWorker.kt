package com.subtrack.app.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.subtrack.app.widget.SubCyclerWidgets

/**
 * Lightweight, zero-side-effect worker scheduled after launcher icon alias switches so home-screen
 * widgets re-apply fresh RemoteViews even if Android kills the process during package component changes.
 */
class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        SubCyclerWidgets.requestUpdateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "subcycler_post_alias_widget_refresh"
    }
}

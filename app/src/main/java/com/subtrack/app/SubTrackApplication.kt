package com.subtrack.app

import android.app.Application
import android.content.res.Configuration as AndroidConfiguration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.app.widget.SubCyclerWidgets
import com.subtrack.app.worker.WorkSchedulerManager
import com.subtrack.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SubTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var repository: SubscriptionRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private var lastNightModeMask: Int = AndroidConfiguration.UI_MODE_NIGHT_UNDEFINED

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        lastNightModeMask = resources.configuration.uiMode and AndroidConfiguration.UI_MODE_NIGHT_MASK
        NotificationHelper.createNotificationChannel(this)
        WorkSchedulerManager.scheduleDailyCycleEvaluation(this)
    }

    override fun onConfigurationChanged(newConfig: AndroidConfiguration) {
        super.onConfigurationChanged(newConfig)
        val newNightMask = newConfig.uiMode and AndroidConfiguration.UI_MODE_NIGHT_MASK
        if (newNightMask != lastNightModeMask) {
            lastNightModeMask = newNightMask
            SubCyclerWidgets.requestUpdateAll(this)
        }
    }
}

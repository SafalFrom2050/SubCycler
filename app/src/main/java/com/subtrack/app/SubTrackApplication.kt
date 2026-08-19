package com.subtrack.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.app.notification.NotificationHelper
import com.subtrack.app.worker.WorkSchedulerManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SubTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var repository: SubscriptionRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        WorkSchedulerManager.scheduleDailyCycleEvaluation(this)
    }
}

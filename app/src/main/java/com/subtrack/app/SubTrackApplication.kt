package com.subtrack.app

import android.app.Application
import com.subtrack.app.data.local.AppDatabase
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.data.repository.SubscriptionRepositoryImpl
import com.subtrack.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class SubTrackApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository: SubscriptionRepository by lazy { SubscriptionRepositoryImpl(database.subscriptionDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}

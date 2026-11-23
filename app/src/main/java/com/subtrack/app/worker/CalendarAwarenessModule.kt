package com.subtrack.app.worker

import com.subtrack.app.notification.AndroidSubscriptionActionNotifier
import com.subtrack.app.notification.AndroidSubscriptionReminderScheduler
import com.subtrack.core.domain.repository.CalendarAwarenessScheduler
import com.subtrack.core.domain.repository.SubscriptionActionNotifier
import com.subtrack.core.domain.repository.SubscriptionReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarAwarenessModule {
    @Binds
    @Singleton
    abstract fun bindCalendarAwarenessScheduler(
        implementation: AndroidCalendarAwarenessScheduler
    ): CalendarAwarenessScheduler

    @Binds
    @Singleton
    abstract fun bindSubscriptionActionNotifier(
        implementation: AndroidSubscriptionActionNotifier
    ): SubscriptionActionNotifier

    @Binds
    @Singleton
    abstract fun bindSubscriptionReminderScheduler(
        implementation: AndroidSubscriptionReminderScheduler
    ): SubscriptionReminderScheduler
}

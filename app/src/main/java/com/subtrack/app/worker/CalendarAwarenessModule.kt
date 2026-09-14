package com.subtrack.app.worker

import com.subtrack.core.domain.repository.CalendarAwarenessScheduler
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
}

package com.subtrack.core.data.di

import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.repository.SubscriptionReminderScheduler
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.AlignPairedSubscriptionsUseCase
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetSubscriptionCycleStatusesUseCase(
        repository: SubscriptionRepository,
        timeProvider: TimeProvider
    ): GetSubscriptionCycleStatusesUseCase {
        return GetSubscriptionCycleStatusesUseCase(repository, timeProvider)
    }

    @Provides
    @Singleton
    fun provideGetPortfolioSummaryUseCase(): GetPortfolioSummaryUseCase {
        return GetPortfolioSummaryUseCase()
    }

    @Provides
    @Singleton
    fun provideAlignPairedSubscriptionsUseCase(
        repository: SubscriptionRepository,
        timeProvider: TimeProvider,
        reminderScheduler: SubscriptionReminderScheduler,
        userPreferencesRepository: UserPreferencesRepository
    ): AlignPairedSubscriptionsUseCase {
        return AlignPairedSubscriptionsUseCase(
            repository = repository,
            timeProvider = timeProvider,
            reminderScheduler = reminderScheduler,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    @Provides
    @Singleton
    fun provideManageSubscriptionUseCase(
        repository: SubscriptionRepository,
        reminderScheduler: SubscriptionReminderScheduler,
        userPreferencesRepository: UserPreferencesRepository
    ): ManageSubscriptionUseCase {
        return ManageSubscriptionUseCase(
            repository = repository,
            reminderScheduler = reminderScheduler,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}

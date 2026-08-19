package com.subtrack.app.di

import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.domain.time.TimeProvider
import com.subtrack.app.domain.usecase.AlignPairedSubscriptionsUseCase
import com.subtrack.app.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.app.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.app.domain.usecase.ManageSubscriptionUseCase
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
        timeProvider: TimeProvider
    ): AlignPairedSubscriptionsUseCase {
        return AlignPairedSubscriptionsUseCase(repository, timeProvider)
    }

    @Provides
    @Singleton
    fun provideManageSubscriptionUseCase(
        repository: SubscriptionRepository
    ): ManageSubscriptionUseCase {
        return ManageSubscriptionUseCase(repository)
    }
}

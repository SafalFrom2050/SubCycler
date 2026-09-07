package com.subtrack.core.data.di

import android.content.Context
import com.subtrack.core.common.time.DefaultTimeProvider
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.data.local.AppDatabase
import com.subtrack.core.data.local.SubscriptionDao
import com.subtrack.core.data.preferences.UserPreferencesRepositoryImpl
import com.subtrack.core.data.preferences.userDataStore
import com.subtrack.core.data.repository.SubscriptionRepositoryImpl
import com.subtrack.core.domain.repository.SubscriptionRepository
import com.subtrack.core.domain.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    @Singleton
    fun provideSubscriptionRepository(dao: SubscriptionDao): SubscriptionRepository {
        return SubscriptionRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        val locale = context.resources.configuration.locales[0]
        return UserPreferencesRepositoryImpl(
            dataStore = context.userDataStore,
            defaultCurrencyCode = MoneyFormatter.currencyCodeForLocale(locale)
        )
    }

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return DefaultTimeProvider()
    }
}

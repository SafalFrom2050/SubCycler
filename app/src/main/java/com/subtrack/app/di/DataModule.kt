package com.subtrack.app.di

import android.content.Context
import com.subtrack.app.data.local.AppDatabase
import com.subtrack.app.data.local.SubscriptionDao
import com.subtrack.app.data.preferences.UserPreferencesRepository
import com.subtrack.app.data.preferences.UserPreferencesRepositoryImpl
import com.subtrack.app.data.preferences.userDataStore
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.data.repository.SubscriptionRepositoryImpl
import com.subtrack.app.domain.time.DefaultTimeProvider
import com.subtrack.app.domain.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val applicationScope = CoroutineScope(SupervisorJob())
        return AppDatabase.getDatabase(context, applicationScope)
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
        return UserPreferencesRepositoryImpl(context.userDataStore)
    }

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return DefaultTimeProvider()
    }
}

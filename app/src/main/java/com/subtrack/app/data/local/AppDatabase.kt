package com.subtrack.app.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subtrack.app.data.local.entity.SubscriptionEntity
import com.subtrack.app.data.local.mapper.toEntity
import com.subtrack.app.data.model.RotationStrategy
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.model.SubscriptionCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(entities = [SubscriptionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subtrack_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.subscriptionDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: SubscriptionDao) {
            val today = LocalDate.now()
            val initialSubs = listOf(
                Subscription(
                    name = "Netflix",
                    cost = 15.49,
                    currency = "USD",
                    category = SubscriptionCategory.STREAMING,
                    startDate = today.minusDays(18), // Day 19 of 45 (Active, 12 days left to cancel)
                    strategy = RotationStrategy.ROTATION_45_DAYS,
                    customActiveDays = 30,
                    customBlackoutDays = 15,
                    colorHex = "#E50914",
                    iconName = "Tv",
                    notes = "Rotate every 45 days. Watch latest season drops during 30d window."
                ),
                Subscription(
                    name = "Disney+",
                    cost = 13.99,
                    currency = "USD",
                    category = SubscriptionCategory.STREAMING,
                    startDate = today.minusDays(33), // Day 34 of 45 (In Blackout, resubscribe in 11 days)
                    strategy = RotationStrategy.ROTATION_45_DAYS,
                    customActiveDays = 30,
                    customBlackoutDays = 15,
                    colorHex = "#113CCF",
                    iconName = "Tv",
                    notes = "In blackout period. Resubscribes when Marvel/Star Wars backlog accumulates."
                ),
                Subscription(
                    name = "Spotify Premium",
                    cost = 11.99,
                    currency = "USD",
                    category = SubscriptionCategory.MUSIC,
                    startDate = today.minusDays(10),
                    strategy = RotationStrategy.CONTINUOUS,
                    customActiveDays = 30,
                    customBlackoutDays = 0,
                    colorHex = "#1DB954",
                    iconName = "Music",
                    notes = "Continuous active subscription for daily music."
                ),
                Subscription(
                    name = "ChatGPT Plus",
                    cost = 20.00,
                    currency = "USD",
                    category = SubscriptionCategory.AI_TOOLS,
                    startDate = today.minusDays(27), // Day 28 of 45 (Active, 3 days left - Action warning!)
                    strategy = RotationStrategy.ROTATION_45_DAYS,
                    customActiveDays = 30,
                    customBlackoutDays = 15,
                    colorHex = "#10A37F",
                    iconName = "Cpu",
                    notes = "Rotate with Claude Pro for alternating AI workflow."
                )
            )
            dao.insertAll(initialSubs.toEntity())
        }
    }
}

package com.subtrack.core.data.local

import android.content.Context
import androidx.room.*
import com.subtrack.core.data.local.entity.SubscriptionEntity

@Database(entities = [SubscriptionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subtrack_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

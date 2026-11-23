package com.subtrack.core.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subtrack.core.data.local.entity.SubscriptionEntity

@Database(entities = [SubscriptionEntity::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        private const val LEGACY_UNCONFIRMED_NOTE = "Added during onboarding. Next billing date not confirmed."

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN billingPeriod TEXT NOT NULL DEFAULT 'MONTHLY'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN isBillingDateConfirmed INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN cancelledCycleIndex INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN completedBreaksCount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN earnedSavingsTotal REAL NOT NULL DEFAULT 0.0"
                )
                db.execSQL(
                    "UPDATE subscriptions SET isBillingDateConfirmed = 0, notes = '' WHERE notes = '$LEGACY_UNCONFIRMED_NOTE'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN bonusActiveDays INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE subscriptions SET isBillingDateConfirmed = 0, notes = '' WHERE notes = '$LEGACY_UNCONFIRMED_NOTE'"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN isStrategyConfirmed INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subtrack_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

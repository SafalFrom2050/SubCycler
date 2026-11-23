package com.subtrack.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.SubscriptionCategory
import java.time.LocalDate

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cost: Double,
    val currency: String = "USD",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val category: SubscriptionCategory = SubscriptionCategory.STREAMING,
    val startDate: LocalDate,
    val strategy: RotationStrategy = RotationStrategy.ROTATION_45_DAYS,
    val customActiveDays: Int = 30,
    val customBlackoutDays: Int = 15,
    val colorHex: String = "#10B981",
    val iconName: String = "Tv",
    val notes: String = "",
    val isAutoRenewCancelled: Boolean = false,
    val pairedWithSubscriptionId: Long? = null,
    val isBillingDateConfirmed: Boolean = true,
    val isStrategyConfirmed: Boolean = true,
    val cancelledCycleIndex: Int? = null,
    val completedBreaksCount: Int = 0,
    val earnedSavingsTotal: Double = 0.0,
    val bonusActiveDays: Int = 0
)

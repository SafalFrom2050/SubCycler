package com.subtrack.core.data.local

import androidx.room.TypeConverter
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.SubscriptionCategory
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromStrategy(value: RotationStrategy?): String? {
        return value?.name
    }

    @TypeConverter
    fun toStrategy(value: String?): RotationStrategy? {
        return value?.let { RotationStrategy.valueOf(it) }
    }

    @TypeConverter
    fun fromCategory(value: SubscriptionCategory?): String? {
        return value?.name
    }

    @TypeConverter
    fun toCategory(value: String?): SubscriptionCategory? {
        return value?.let { SubscriptionCategory.valueOf(it) }
    }
}

package com.mobichill.justconcentration.base.database

import androidx.room.TypeConverter
import com.mobichill.justconcentration.model.SubscriptionDetails
import com.google.gson.Gson

class Converters {
    // Use Gson or another JSON library
    private val gson = Gson()

    @TypeConverter
    fun fromSubscriptionDetails(details: SubscriptionDetails?): String? {
        return if (details == null) null else gson.toJson(details)
    }

    @TypeConverter
    fun toSubscriptionDetails(detailsString: String?): SubscriptionDetails? {
        return if (detailsString == null) null else gson.fromJson(
            detailsString,
            SubscriptionDetails::class.java
        )
    }
}
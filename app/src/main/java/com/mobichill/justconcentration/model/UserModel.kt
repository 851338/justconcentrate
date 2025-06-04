package com.mobichill.justconcentration.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserModel(
    @PrimaryKey var uid: String = "",
    var name: String? = null,
    var email: String? = null,
    var profilePic: String? = null,
    var createdAt: Long? = null,
    var lastLogin: Long = 0L,
    var subscriptionStatus: Boolean = false,
    var subscriptionDetails: SubscriptionDetails? = null
) {
    // For firebase mapping
    constructor() : this("", null, null, null, null, 0L, false, null)
}
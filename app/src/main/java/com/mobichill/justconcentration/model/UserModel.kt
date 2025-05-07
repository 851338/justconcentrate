package com.mobichill.justconcentration.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserModel(
    @PrimaryKey val uid: String,
    var name: String?,
    var email: String?,
    var profilePic: String?,
    var createdAt: Long?,
    var lastLogin: Long,
    var subscriptionStatus: Boolean = false,
    var subscriptionStartDate: Long? = null,
    var subscriptionExpiryDate: Long? = null
)
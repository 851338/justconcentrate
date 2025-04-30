package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "badges")
data class BadgeModel(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconResId: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val goal: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) : Parcelable

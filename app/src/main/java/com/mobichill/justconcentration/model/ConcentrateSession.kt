package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "focus_sessions")
@Parcelize
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goal: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val completed: Boolean
) : Parcelable
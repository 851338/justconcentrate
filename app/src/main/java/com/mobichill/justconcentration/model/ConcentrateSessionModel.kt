package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@IgnoreExtraProperties
@Entity(tableName = "focus_sessions")
data class ConcentrateSessionModel(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Unique ID for both Room & FireStore

    val goal: String = "",

    val startTime: Long = 0L,

    val endTime: Long = 0L,

    val durationMinutes: Int = 0,

    val wasCompleted: Boolean = false,

    val isSynced: Boolean = false
) :
    Parcelable
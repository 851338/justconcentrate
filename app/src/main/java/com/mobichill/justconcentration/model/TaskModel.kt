package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity(tableName = "tasks")
data class TaskModel(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Unique ID for both Room & FireStore
    val taskText: String = "",
    val alarmTimeMillis: Long = 0L,
    val requestCode: Int = 0,
    val completed: Boolean = false,
    val alarmSoundUri: String = "",
    val deletedAt: Long? = null,
    val createdAt: Long = 0L,
    val isSynced: Boolean = false,
    val completedAt: Long? = null,
    val lastModified: Long? = null
) : Parcelable
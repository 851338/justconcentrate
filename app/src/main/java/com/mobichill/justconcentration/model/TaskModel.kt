package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "tasks")
data class TaskModel(
    @PrimaryKey(autoGenerate = false) // Use FireStore ID
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("taskText") @set:PropertyName("taskText")
    var taskText: String = "",

    @get:PropertyName("alarmTimeMillis") @set:PropertyName("alarmTimeMillis")
    var alarmTimeMillis: Long = 0L,

    @get:PropertyName("requestCode") @set:PropertyName("requestCode")
    var requestCode: Int = 0,

    @get:PropertyName("completed") @set:PropertyName("completed")
    var completed: Boolean = false,

    @get:PropertyName("alarmSoundUri") @set:PropertyName("alarmSoundUri")
    var alarmSoundUri: String = "",

    @get:PropertyName("deletedAt") @set:PropertyName("deletedAt")
    var deletedAt: Long? = null
) : Parcelable {
    constructor() : this("", "", 0L, 0, false, "", null) // Required for FireStore deserialize
}
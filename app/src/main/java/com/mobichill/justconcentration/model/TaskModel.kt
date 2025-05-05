package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
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
    val serverLastUpdatedMillis: Long? = null,
    var needsUpload: Boolean = true,
    var isDeletedLocally: Boolean = false
) : Parcelable {
    companion object {
        fun fromFireStoreMap(docId: String, map: Map<String, Any?>): TaskModel? {
            try {
                // Extract server timestamp first for local storage
                val serverTimestamp = map["lastUpdated"] as? Timestamp
                val serverLastUpdatedMillis = serverTimestamp?.toDate()?.time

                val task = TaskModel(
                    id = docId, // Use the document ID as the primary key

                    // Extract core fields with safe casting and defaults
                    taskText = map["taskText"] as? String ?: "",
                    alarmTimeMillis = map["alarmTimeMillis"] as? Long ?: 0L,
                    requestCode = (map["requestCode"] as? Long)?.toInt()
                        ?: 0, // FireStore stores numbers as Long by default
                    completed = map["completed"] as? Boolean == true,
                    alarmSoundUri = map["alarmSoundUri"] as? String ?: "",
                    createdAt = map["createdAt"] as? Long
                        ?: System.currentTimeMillis(), // Default if missing
                    completedAt = map["completedAt"] as? Long, // Nullable field

                    // Sync/Delete fields
                    deletedAt = map["deletedAt"] as? Long, // Nullable field

                    // --- Set Local State for Downloaded Item ---
                    serverLastUpdatedMillis = serverLastUpdatedMillis, // Store the server time locally
                    isSynced = true,       // Item came from server, so it's synced
                    needsUpload = false,   // Doesn't need upload immediately after download
                    isDeletedLocally = false // Not marked for local deletion
                )

                return task

            } catch (e: Exception) {
                return null // Return null if conversion fails
            }
        }
    }
    fun toFireStoreMap(): MutableMap<String, Any?> {
        return mutableMapOf(
            "taskText" to this.taskText,
            "alarmTimeMillis" to this.alarmTimeMillis,
            "requestCode" to this.requestCode,
            "completed" to this.completed,
            "createdAt" to this.createdAt,
            "completedAt" to this.completedAt,
            "deletedAt" to this.deletedAt
        )
    }
}


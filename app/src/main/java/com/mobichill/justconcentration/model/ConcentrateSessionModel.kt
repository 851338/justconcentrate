package com.mobichill.justconcentration.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@IgnoreExtraProperties
@Entity(tableName = "focus_sessions")
data class ConcentrateSessionModel(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Unique ID for both Room & Firestore
    val goal: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val date: String = "",
    val durationMinutes: Int = 0,
    val wasCompleted: Boolean = false,
    var serverLastUpdatedMillis: Long? = null,
    var isSynced: Boolean = false,
    var needsUpload: Boolean = true,
) : Parcelable {
    companion object {
        fun fromFirestoreMap(docId: String, map: Map<String, Any?>): ConcentrateSessionModel? {
            try {
                val serverTimestamp = map["lastUpdated"] as? Timestamp
                val serverMillis = serverTimestamp?.toDate()?.time

                val session = ConcentrateSessionModel(
                    id = docId,
                    goal = map["goal"] as? String ?: "",
                    startTime = map["startTime"] as? Long ?: 0L,
                    endTime = map["endTime"] as? Long ?: 0L,
                    date = map["date"] as? String ?: "",
                    durationMinutes = (map["durationMinutes"] as? Long)?.toInt() ?: 0,
                    wasCompleted = map["wasCompleted"] as? Boolean == true,

                    // Local State
                    serverLastUpdatedMillis = serverMillis,
                    isSynced = true,
                    needsUpload = false,
                )

                return session

            } catch (e: Exception) {
                return null
            }
        }
    }

    /**
     * Converts ConcentrateSessionModel to a MutableMap for Firestore.
     * Excludes local state fields.
     * Sync logic MUST add 'lastUpdated' FieldValue.serverTimestamp().
     */
    fun toFirestoreMap(): MutableMap<String, Any?> {
        return mutableMapOf(
            "goal" to this.goal,
            "startTime" to this.startTime,
            "endTime" to this.endTime,
            "date" to this.date,
            "durationMinutes" to this.durationMinutes,
            "wasCompleted" to this.wasCompleted
        )
    }
}
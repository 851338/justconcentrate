package com.mobichill.justconcentration.model

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "badges")
data class BadgeModel(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconName: String = "",
    val criteria: String = "",
    var progress: Int = 0,
    var isUnlocked: Boolean = false,
    var unlockedAt: Long? = null,
    val goal: Int = 0,
    val isPro: Boolean = false,
    var serverLastUpdatedMillis: Long? = null,
    var isSynced: Boolean = false,
    var needsUpload: Boolean = false
) : Parcelable {
    @Ignore
    @DrawableRes
    fun getDrawableResourceId(context: Context): Int {
        return context.resources.getIdentifier(
            iconName,       // The name stored in the DB/Firestore
            "drawable",     // The resource type
            context.packageName
        ).let { if (it == 0) android.R.drawable.btn_star_big_on else it } }

    companion object {
        private const val TAG = "BadgeModelMapper"

        /**
         * Creates a BadgeModel instance from Firestore data.
         * Assumes Firestore stores progress data per user.
         * Updates local state flags appropriately.
         */
        fun fromFirestoreMap(docId: String, map: Map<String, Any?>): BadgeModel? {
            try {
                val serverTimestamp = map["lastUpdated"] as? Timestamp
                val serverMillis = serverTimestamp?.toDate()?.time

                // Fetch static data from a local source/definition if not storing it per user in Firestore
                // For simplicity here, we assume name/desc/icon/goal might be in the map
                // but ideally, only progress/unlock state is synced per user.
                val badge = BadgeModel(
                    id = docId, // Use Firestore doc ID (which should match badge definition ID)

                    // Static definition data (fetch locally OR from map if stored in FS)
                    name = map["name"] as? String ?: "", // Potentially fetch locally instead
                    criteria = map["criteria"] as? String ?: "",
                    description = map["description"] as? String ?: "", // Potentially fetch locally
                    iconName = map["iconName"] as? String ?: "", // Potentially fetch locally
                    goal = (map["goal"] as? Long)?.toInt() ?: 0, // Potentially fetch locally
                    isPro = (map["isPro"] as? Boolean) == true,

                    // User Progress Data from Firestore
                    progress = (map["progress"] as? Long)?.toInt() ?: 0,
                    isUnlocked = map["isUnlocked"] as? Boolean ?: false,
                    unlockedAt = map["unlockedAt"] as? Long,

                    // Local State
                    serverLastUpdatedMillis = serverMillis,
                    isSynced = true,
                    needsUpload = false
                )

                if (serverMillis == null) {
                    Log.w(TAG, "Firestore 'lastUpdated' timestamp missing for badge: $docId")
                    // return null // Decide if timestamp is mandatory
                }

                return badge

            } catch (e: Exception) {
                Log.e(TAG, "Error converting Firestore map to BadgeModel for doc: $docId", e)
                return null
            }
        }
    }

    /**
     * Converts BadgeModel progress data to a MutableMap for Firestore.
     * Excludes static definition fields (assuming they aren't stored per user)
     * and local state fields.
     * Sync logic MUST add 'lastUpdated' FieldValue.serverTimestamp().
     */
    fun toFirestoreMap(): MutableMap<String, Any?> {
        return mutableMapOf(
            // --- User Progress Data to Store ---
            "progress" to progress,
            "isUnlocked" to isUnlocked,
            "unlockedAt" to unlockedAt // Store unlock time

            // --- Excluded ---
            // "id" -> Use as document ID
            // "serverLastUpdatedMillis", "isSynced", "needsUpload" -> Local state only
        )
    }
}

package com.mobichill.justconcentration.dao
import androidx.room.*
import com.mobichill.justconcentration.model.BadgeModel
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: BadgeModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgeModel>)

    @Insert(onConflict = OnConflictStrategy.IGNORE) // IGNORE if somehow a badge already exists
    suspend fun insertAll(badges: List<BadgeModel>)

    @Update
    suspend fun updateBadge(badge: BadgeModel)

    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, unlockedAt ASC")
    fun getAllBadges(): Flow<List<BadgeModel>>

    @Query("SELECT * FROM badges WHERE id = :id")
    fun getBadgeById(id: String): Flow<BadgeModel>

    @Query("DELETE FROM badges")
    suspend fun clearAllBadges()

    @Query("SELECT * FROM badges WHERE needsUpload = 1")
    suspend fun getBadgesNeedingUpload(): List<BadgeModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBadge(model: BadgeModel)

    @Query("UPDATE badges SET isSynced = 1, needsUpload = 0, serverLastUpdatedMillis = :serverTimestampMillis WHERE id = :id")
    suspend fun markBadgeAsSyncedById(id: String, serverTimestampMillis: Long)

    @Query("UPDATE badges SET isSynced = 1, needsUpload = 0 WHERE id IN (:ids)")
    suspend fun markBadgesAsSyncedAfterUpload(ids: List<String>)

    @Query("SELECT COUNT(*) FROM badges")
    suspend fun getBadgeCount(): Int
}

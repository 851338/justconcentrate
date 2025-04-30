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

    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<BadgeModel>>

    @Query("SELECT * FROM badges WHERE id = :id")
    fun getBadgeById(id: String): Flow<BadgeModel>

    @Query("DELETE FROM badges")
    suspend fun clearAllBadges()
}

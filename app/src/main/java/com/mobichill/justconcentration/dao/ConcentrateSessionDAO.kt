package com.mobichill.justconcentration.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcentrateSessionDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConcentrateSessionModel)

    @Update
    suspend fun updateSession(session: ConcentrateSessionModel)

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: String) : Flow<ConcentrateSessionModel>

    @Query("SELECT * FROM focus_sessions WHERE wasCompleted = 0 ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConcentrateSessionModel>>

    @Query("SELECT * FROM focus_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<ConcentrateSessionModel>

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ConcentrateSessionModel)

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ConcentrateSessionModel

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConcentrateSessionModel>>

    @Query("SELECT * FROM focus_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<ConcentrateSessionModel>

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE wasCompleted = 1")
    suspend fun getTotalCompletedSessionCount(): Int

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE wasCompleted = 1")
    suspend fun getTotalFocusMinutes(): Int

    @Query("SELECT * FROM focus_sessions WHERE needsUpload = 1")
    suspend fun getSessionsNeedingUpload(): List<ConcentrateSessionModel>

    @Query("UPDATE focus_sessions SET isSynced = 1, needsUpload = 0, serverLastUpdatedMillis = :serverTimestampMillis WHERE id = :id")
    suspend fun markSessionAsSyncedById(id: String, serverTimestampMillis: Long)

    @Query("UPDATE focus_sessions SET isSynced = 1, needsUpload = 0 WHERE id IN (:ids)")
    suspend fun markSessionsAsSyncedAfterUpload(ids: List<String>)
}
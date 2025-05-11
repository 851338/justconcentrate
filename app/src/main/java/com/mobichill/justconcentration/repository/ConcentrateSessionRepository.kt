package com.mobichill.justconcentration.repository

import android.util.Log
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.format.DateTimeParseException

class ConcentrateSessionRepository(private val concentrateSessionDAO: ConcentrateSessionDAO) {
    companion object {
        private val TAG = ConcentrateSessionRepository::class.java.simpleName
    }

    suspend fun addConcentrateSessionToRoom(session: ConcentrateSessionModel) {
        concentrateSessionDAO.insertSession(session)
    }

    suspend fun updateSession(session: ConcentrateSessionModel) {
        concentrateSessionDAO.updateSession(session)
    }

    suspend fun upsertSession(session: ConcentrateSessionModel) {
        concentrateSessionDAO.upsertSession(session)
    }

    fun getAllSessions() = concentrateSessionDAO.getAllSessions()

    suspend fun getUnsyncedSessions() = concentrateSessionDAO.getUnsyncedSessions()

    suspend fun getSessionById(id: String) = concentrateSessionDAO.getSessionById(id)

    suspend fun getCurrentFocusStreak(): Int {
        val completedSessions: List<ConcentrateSessionModel> = try {
            concentrateSessionDAO.getAllSessions().first().filter { it.wasCompleted }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sessions for streak", e)
            return 0 // Return 0 if data fetching fails
        }
        if (completedSessions.isEmpty()) {
            return 0
        }
        val completedDates = completedSessions
            .mapNotNull { session ->
                try {
                    LocalDate.parse(session.date, DATE_FORMATTER)
                } catch (e: DateTimeParseException) {
                    Log.w(TAG, "Could not parse date: ${session.date}", e)
                    null // Exclude this session from streak calculation
                }
            }
            .distinct()
            .sortedDescending()

        if (completedDates.isEmpty()) {
            return 0
        }

        var streak = 0
        var dateToCheck = LocalDate.now()
        for (date in completedDates) {
            if (date == dateToCheck) {
                // Found a session for the day we are checking
                streak++
                // Move to check the previous day
                dateToCheck = dateToCheck.minusDays(1)
            } else if (date.isBefore(dateToCheck)) {
                // We found a date that is older than the next expected day in the streak.
                // This means there's a gap, so the streak ended before this date.
                break // Exit the loop, the current streak value is final.
            } // If date.isAfter(dateToCheck), it means we might have multiple sessions on the same day
        }
        return streak
    }

    suspend fun getTotalCompletedSessionCount(): Int {
        return try {
            concentrateSessionDAO.getTotalCompletedSessionCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching session minutes", e)
            0
        }
    }

    suspend fun getSessionsNeedingUpload() = concentrateSessionDAO.getSessionsNeedingUpload()

    suspend fun markSessionAsSyncedById(id: String, serverTimestampMillis: Long) {
        concentrateSessionDAO.markSessionAsSyncedById(id, serverTimestampMillis)
    }

    suspend fun markSessionsAsSyncedAfterUpload(ids: List<String>) {
        concentrateSessionDAO.markSessionsAsSyncedAfterUpload(ids)
    }

    suspend fun getTotalFocusMinutes(): Int {
        return try {
            concentrateSessionDAO.getTotalFocusMinutes()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching session minutes", e)
            0
        }
    }

    suspend fun getCompletedSessionsBetween(startTimeMillis: Long, endTimeMillis: Long) =
        concentrateSessionDAO.getCompletedSessionsBetween(startTimeMillis, endTimeMillis)

    suspend fun getSessionsStartedBetween(startTimeMillis: Long, endTimeMillis: Long) =
        concentrateSessionDAO.getSessionsStartedBetween(startTimeMillis, endTimeMillis)
}
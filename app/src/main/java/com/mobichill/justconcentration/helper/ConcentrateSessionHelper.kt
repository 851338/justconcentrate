package com.mobichill.justconcentration.helper

import android.util.Log
import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import kotlinx.coroutines.flow.firstOrNull

class ConcentrateSessionHelper(private val concentrateSessionDAO: ConcentrateSessionDAO) {
    suspend fun addConcentrateSessionToRoom(session: ConcentrateSessionModel) {
        try {
            val rowId = concentrateSessionDAO.insertSession(session)

            if (rowId != -1L) {
                // Success
                Log.d("aaaaa", "success")
            } else {
                Log.e("aaaaa", "failed")
                // Insert failed
            }
            val sessions = concentrateSessionDAO.getAllSessions()
            sessions.collect { sessions ->
                Log.d("DB", "Sessions count: ${sessions.size}")
            }
            sessions.collect { sessions ->
                sessions.forEach { Log.d("DB", "Sessions: ${it.goal}") }
            }
        } catch (e: Exception) {
            Log.e("Insert", "Error inserting session: ${e.message}")
        }
    }

    suspend fun updateSession(session: ConcentrateSessionModel) {
        concentrateSessionDAO.updateSession(session)
    }

    fun getAllSessions() = concentrateSessionDAO.getAllSessions()

    suspend fun getUnsyncedSessions() = concentrateSessionDAO.getUnsyncedSessions()

    suspend fun syncSessionsToRoom(fireStoreSessions: List<ConcentrateSessionModel>) {
        fireStoreSessions.forEach { fireStoreSession ->
            val roomSession =
                concentrateSessionDAO.getSessionById(fireStoreSession.id).firstOrNull()
            if (roomSession == null)
                concentrateSessionDAO.insertSession(fireStoreSession.copy(isSynced = true))
            else
            // If not synced from Room to FireStore, skip
                if (!roomSession.isSynced) {
                    return@forEach
                } else concentrateSessionDAO.updateSession(fireStoreSession)
        }
    }
}
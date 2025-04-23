package com.mobichill.justconcentration.helper

import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import kotlinx.coroutines.flow.firstOrNull

class ConcentrateSessionHelper(private val concentrateSessionDAO: ConcentrateSessionDAO) {
    suspend fun addConcentrateSessionToRoom(session: ConcentrateSessionModel) {
        concentrateSessionDAO.insertSession(session)
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
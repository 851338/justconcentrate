package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.helper.ConcentrateSessionHelper
import com.mobichill.justconcentration.model.ConcentrateSessionModel

class ConcentrateSessionRepository(private val concentrateSessionHelper: ConcentrateSessionHelper) {
    suspend fun addConcentrateSessionToRoom(session: ConcentrateSessionModel) {
        concentrateSessionHelper.addConcentrateSessionToRoom(session)
    }

    suspend fun updateSession(session: ConcentrateSessionModel) {
        concentrateSessionHelper.updateSession(session)
    }

    suspend fun getUnsyncedSessions() = concentrateSessionHelper.getUnsyncedSessions()

    suspend fun syncSessionsToRoom(sessions: List<ConcentrateSessionModel>) {
        concentrateSessionHelper.syncSessionsToRoom(sessions)
    }

    fun getAllSessions() = concentrateSessionHelper.getAllSessions()
}
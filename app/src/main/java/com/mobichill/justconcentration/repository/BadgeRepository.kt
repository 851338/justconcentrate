package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.dao.BadgeDAO
import com.mobichill.justconcentration.model.BadgeModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(private val badgeDAO: BadgeDAO) {
    suspend fun insertBadge(badge: BadgeModel) {
        badgeDAO.insertBadge(badge)
    }

    suspend fun insertBadges(badges: List<BadgeModel>) {
        badgeDAO.insertBadges(badges)
    }

    suspend fun updateBadge(badge: BadgeModel) {
        badgeDAO.updateBadge(badge)
    }

    suspend fun clearAllBadges() {
        badgeDAO.clearAllBadges()
    }

    fun upsertBadge(badgeModel: BadgeModel) {
        badgeDAO.upsertBadge(badgeModel)
    }

    suspend fun markBadgeAsSyncedById(id: String, serverTimestampMillis: Long) {
        badgeDAO.markBadgeAsSyncedById(id, serverTimestampMillis)
    }

    suspend fun markBadgesAsSyncedAfterUpload(ids: List<String>) {
        badgeDAO.markBadgesAsSyncedAfterUpload(ids)
    }

    fun getAllBadges(): Flow<List<BadgeModel>> = badgeDAO.getAllBadges()

    fun getBadgeById(id: String): Flow<BadgeModel> = badgeDAO.getBadgeById(id)

    suspend fun getBadgesNeedingUpload() = badgeDAO.getBadgesNeedingUpload()
}

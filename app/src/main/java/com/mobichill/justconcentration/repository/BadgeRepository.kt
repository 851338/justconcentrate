package com.mobichill.justconcentration.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobichill.justconcentration.dao.BadgeDAO
import com.mobichill.justconcentration.model.BadgeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BadgeRepository(private val badgeDAO: BadgeDAO) {
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

    fun getAllBadges(): Flow<List<BadgeModel>> = badgeDAO.getAllBadges()

    fun getBadgeById(id: String): Flow<BadgeModel> = badgeDAO.getBadgeById(id)

//    fun syncBadgesToFireStore() {
//        // Get badges from Room
//        CoroutineScope(Dispatchers.IO).launch {
//            val badges = getAllBadges().filter { !it.isSynced }
//            badges.forEach { badge ->
//                firestore.collection("users")
//                    .document(userId)
//                    .collection("badges")
//                    .document(badge.id)
//                    .set(badge)
//                    .addOnSuccessListener {
//                        // Update badge as synced in Room
//                        CoroutineScope(Dispatchers.IO).launch {
//                            badgeDao.insertBadge(badge.copy(isSynced = true))
//                        }
//                    }
//            }
//        }
//    }

//    fun fetchBadgesFromFireStore() {
//        firestore.collection("users")
//            .document(userId)
//            .collection("badges")
//            .get()
//            .addOnSuccessListener { result ->
//                val badgeList = result.documents.mapNotNull { doc ->
//                    doc.toObject(BadgeModel::class.java)
//                }
//                CoroutineScope(Dispatchers.IO).launch {
//                    badgeDao.insertBadges(badgeList)
//                }
//            }
//    }

}

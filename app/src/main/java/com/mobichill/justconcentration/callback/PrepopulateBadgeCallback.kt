package com.mobichill.justconcentration.callback

import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobichill.justconcentration.constants.Achievements
import com.mobichill.justconcentration.dao.BadgeDAO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrepopulateBadgeCallback(
    // Use Provider for lazy DAO access if using Dagger/Hilt
    private val daoProvider: () -> BadgeDAO
) : RoomDatabase.Callback() {
    // Use a dedicated scope for database operations during creation
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Called only when the database is first created
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch {
            populateDatabase()
        }
    }

    private suspend fun populateDatabase() {
        val badgeDao = daoProvider()
        try {
            Log.d("PrepopulateBadgeCallback", "Pre-populating badges...")
            badgeDao.insertAll(Achievements.allBadges) // Assuming insertAll takes List<BadgeModel>
            Log.d("PrepopulateBadgeCallback", "Badges pre-populated successfully.")
        } catch (e: Exception) {
            Log.e("PrepopulateBadgeCallback", "Error pre-populating badges", e)
        }
    }
}
package com.mobichill.justconcentration.repository

import android.util.Log
import androidx.room.withTransaction
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.constants.Achievements
import com.mobichill.justconcentration.dao.BadgeDAO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCleanupRepository @Inject constructor(
    private val myRoomDatabase: MyRoomDatabase,
    private val badgeDao: BadgeDAO
) {

    companion object {
        private val TAG = DataCleanupRepository::class.java.simpleName
    }

    suspend fun clearAllAndPrepopulateDatabase() {
         try {
             myRoomDatabase.withTransaction {
                 // Clear all data
                 Log.d(TAG, "Clearing all tables...")
                 myRoomDatabase.clearAllTables()
                 Log.d(TAG, "All tables cleared.")

                 // Prepopulate badges data
                 Log.d(TAG, "Prepopulating badges...")
                 badgeDao.insertAll(Achievements.allBadges)
                 Log.d(TAG, "Badges prepopulated successfully.")
             }
             Log.d(TAG, "Database clear and prepopulate completed successfully.")

         } catch (e: Exception) {
             Log.e(TAG, "Error during database clear and prepopulate", e)
             // Rethrow the exception so the caller (ViewModel) can handle it
             throw e
         }
    }
}
package com.mobichill.justconcentration.base.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobichill.justconcentration.constants.Achievements
import com.mobichill.justconcentration.constants.Constants.OTHERS.DB_NAME
import com.mobichill.justconcentration.dao.BadgeDAO
import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.dao.UserDAO
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [TaskModel::class,
        UserModel::class,
        ConcentrateSessionModel::class,
        BadgeModel::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract val taskDAO: TaskDAO
    abstract val userDAO: UserDAO
    abstract val concentrateSessionDAO: ConcentrateSessionDAO
    abstract val badgeDAO: BadgeDAO

    companion object {
        @Volatile
        private var TAG = MyRoomDatabase::class.java.simpleName
        private var INSTANCE: MyRoomDatabase? = null

        /**
         * Gets the singleton instance of the database.
         * Uses double-checked locking to ensure thread safety.
         *
         * @param context Application context.
         * @return The singleton MyRoomDatabase instance.
         */
        fun getInstance(context: Context): MyRoomDatabase {
            // Return existing instance if available
            return INSTANCE ?: synchronized(this) {
                // Re-check instance inside synchronized block to prevent race condition
                INSTANCE ?: buildDatabase(context).also {
                    Log.d(TAG, "Database instance created and assigned.")
                    INSTANCE = it // Assign the newly created instance
                }
            }
        }

        // Separate build function for clarity
        private fun buildDatabase(context: Context): MyRoomDatabase {
            Log.d(TAG, "Building database: $DB_NAME")
            return Room.databaseBuilder(
                context.applicationContext, // Use application context
                MyRoomDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration(false)
                .addCallback(PrepopulateBadgeCallback(context.applicationContext)).build()
        }

        class PrepopulateBadgeCallback(
            private val context: Context
        ) : Callback() {
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
                val badgeDao = getInstance(context.applicationContext).badgeDAO
                try {
                    val badgeCount = badgeDao.getBadgeCount()
                    Log.d(TAG, "Current badge count: $badgeCount")
                    if (badgeCount == 0) {
                        Log.d(TAG, "Badges table is empty. Pre-populating...")
                        badgeDao.insertAll(Achievements.allBadges)
                        Log.d(TAG, "Badges pre-populated successfully.")
                    } else {
                        Log.d(TAG, "Badges table already populated. Skipping pre-population.")
                    }
                } catch (e: Exception) {
                    Log.e("PrepopulateBadgeCallback", "Error pre-populating badges", e)
                }
            }
        }
    }
}
package com.mobichill.justconcentration.base.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobichill.justconcentration.callback.PrepopulateBadgeCallback
import com.mobichill.justconcentration.constants.Constants.OTHERS.DB_NAME
import com.mobichill.justconcentration.dao.BadgeDAO
import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.dao.UserDAO
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel

@Database(
    entities = [TaskModel::class,
        UserModel::class,
        ConcentrateSessionModel::class,
        BadgeModel::class],
    version = 1,
    exportSchema = false
)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract val taskDAO: TaskDAO
    abstract val userDAO: UserDAO
    abstract val concentrateSessionDAO: ConcentrateSessionDAO
    abstract val badgeDAO: BadgeDAO

    companion object {
        @Volatile
        private var TAG = this::class.java.simpleName
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
                .addCallback(PrepopulateBadgeCallback { INSTANCE!!.badgeDAO }).build()
        }
    }
}
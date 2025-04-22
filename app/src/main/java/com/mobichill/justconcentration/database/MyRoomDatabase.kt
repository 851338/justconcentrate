package com.mobichill.justconcentration.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.dao.UserDAO
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel

@Database(
    entities = [TaskModel::class,
        UserModel::class,
        ConcentrateSessionModel::class],
    version = 1,
    exportSchema = false
)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract val taskDAO: TaskDAO
    abstract val userDAO: UserDAO
    abstract val concentrateSessionDAO: ConcentrateSessionDAO

    companion object {
        @Volatile
        private var INSTANCE: MyRoomDatabase? = null

        fun getInstance(context: Context): MyRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyRoomDatabase::class.java,
                    "jc_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
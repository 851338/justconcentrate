package com.mobichill.justconcentration.helper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.dao.UserDao
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel

@Database(entities = [TaskModel::class, UserModel::class], version = 1, exportSchema = false)
abstract class RoomHelper : RoomDatabase() {
    abstract fun taskDao(): TaskDAO
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: RoomHelper? = null

        fun getInstance(context: Context): RoomHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoomHelper::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration() // Clears database if schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
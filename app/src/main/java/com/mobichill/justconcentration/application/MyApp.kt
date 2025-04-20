package com.mobichill.justconcentration.application

import android.app.Application
import com.mobichill.justconcentration.database.MyRoomDatabase
import com.mobichill.justconcentration.helper.ConcentrateSessionHelper
import com.mobichill.justconcentration.helper.TaskHelper
import com.mobichill.justconcentration.helper.UserHelper
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.TaskRepository
import com.mobichill.justconcentration.repository.UserRepository

class MyApp : Application() {

    lateinit var taskRepository: TaskRepository
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var concentrateSessionRepository: ConcentrateSessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val db = MyRoomDatabase.getInstance(applicationContext)
        val taskHelper = TaskHelper(db.taskDAO)
        val userHelper = UserHelper(db.userDAO)
        val concentrateSessionHelper = ConcentrateSessionHelper(db.concentrateSessionDAO)
        taskRepository = TaskRepository(taskHelper)
        userRepository = UserRepository(userHelper)
        concentrateSessionRepository = ConcentrateSessionRepository(concentrateSessionHelper)
    }

    companion object {
        lateinit var instance: MyApp
            private set
    }
}

package com.mobichill.justconcentration.base.application

import android.app.Application
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.repository.BadgeRepository
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
    lateinit var badgeRepository: BadgeRepository
        private set
    lateinit var badgeProgressManager: BadgeProgressManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val db = MyRoomDatabase.getInstance(applicationContext)
        taskRepository = TaskRepository(db.taskDAO)
        userRepository = UserRepository(db.userDAO)
        concentrateSessionRepository = ConcentrateSessionRepository(db.concentrateSessionDAO)
        badgeRepository = BadgeRepository(db.badgeDAO)
        badgeProgressManager = BadgeProgressManager()
    }

    companion object {
        lateinit var instance: MyApp
            private set
    }
}

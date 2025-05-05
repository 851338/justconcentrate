package com.mobichill.justconcentration.base.application

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.factory.SyncWorkerFactory
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.repository.BadgeRepository
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.TaskRepository
import com.mobichill.justconcentration.repository.UserRepository

class MyApp : Application(), Configuration.Provider {

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

    lateinit var syncWorkerFactory: SyncWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            Log.i("MyApp", "Providing WorkManager Configuration with custom factory NOW.")
            if (!::syncWorkerFactory.isInitialized) {
                Log.e("MyApp", "CRITICAL: syncWorkerFactory accessed before initialization!")
                // Handle this error state appropriately, maybe throw an exception
                // or return a default configuration to prevent a crash here.
                // This check is defensive.
            }
            return Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .setWorkerFactory(syncWorkerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        Log.i("MyApp", "MyApp.onCreate - START")

        instance = this
        val db = MyRoomDatabase.getInstance(applicationContext)

        taskRepository = TaskRepository(db.taskDAO)
        userRepository = UserRepository(db.userDAO)
        concentrateSessionRepository = ConcentrateSessionRepository(db.concentrateSessionDAO)
        badgeRepository = BadgeRepository(db.badgeDAO)
        badgeProgressManager = BadgeProgressManager()

        syncWorkerFactory =
            SyncWorkerFactory(taskRepository, concentrateSessionRepository, badgeRepository, db)
        Log.i("MyApp", "MyApp.onCreate - SyncWorkerFactory CREATED")
        Log.i("MyApp", "MyApp.onCreate - END")
    }

    companion object {
        lateinit var instance: MyApp
            private set
    }
}

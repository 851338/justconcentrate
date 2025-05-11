package com.mobichill.justconcentration.base.application

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.factory.SyncWorkerFactory
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.manager.ProBadgeManager
import com.mobichill.justconcentration.repository.BadgeRepository
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.FirestoreRepository
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
    lateinit var proBadgeManager: ProBadgeManager
        private set
    lateinit var syncWorkerFactory: SyncWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            if (!::syncWorkerFactory.isInitialized) {
                Log.e("MyApp", "CRITICAL: syncWorkerFactory accessed before initialization!")
                // This check is defensive.
            }
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setWorkerFactory(syncWorkerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()

        instance = this
        val db = MyRoomDatabase.getInstance(applicationContext)

        taskRepository = TaskRepository(db.taskDAO)
        userRepository = UserRepository(db.userDAO)
        concentrateSessionRepository = ConcentrateSessionRepository(db.concentrateSessionDAO)
        badgeRepository = BadgeRepository(db.badgeDAO)
        badgeProgressManager = BadgeProgressManager()
        proBadgeManager = ProBadgeManager(badgeRepository, FirestoreRepository())
        syncWorkerFactory =
            SyncWorkerFactory(taskRepository, concentrateSessionRepository, badgeRepository, db)

        FirebaseApp.initializeApp(this)
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
    }

    companion object {
        lateinit var instance: MyApp
            private set
    }
}

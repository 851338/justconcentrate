package com.mobichill.justconcentration.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.repository.BadgeRepository
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.TaskRepository
import com.mobichill.justconcentration.worker.SyncWorker

class SyncWorkerFactory(
    private val taskRepository: TaskRepository,
    private val sessionRepository: ConcentrateSessionRepository,
    private val badgeRepository: BadgeRepository,
    private val myRoomDatabase: MyRoomDatabase
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name ->
                SyncWorker(
                    appContext,
                    workerParameters,
                    taskRepository,
                    sessionRepository,
                    badgeRepository,
                    myRoomDatabase
                )
            // ... potentially other workers ...
            else -> null // Let default factory handle others or throw error
        }
    }
}
package com.mobichill.justconcentration.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobichill.justconcentration.helper.FireStoreHelper
import com.mobichill.justconcentration.helper.RoomHelper

class AutoDeleteOldTasksWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        RoomHelper.getInstance(applicationContext).taskDao()
            .permanentlyDeleteOldTasks(sevenDaysAgo)
        return if (FireStoreHelper.getInstance().permanentlyDeleteOldTasks())
            Result.success()
        else Result.retry()
    }
}

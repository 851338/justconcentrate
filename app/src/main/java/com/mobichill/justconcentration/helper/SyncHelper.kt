package com.mobichill.justconcentration.helper

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.WorkManager
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.worker.SyncWorker
import java.util.concurrent.TimeUnit

object SyncHelper {

    const val UNIQUE_ONETIME_SYNC_WORK_NAME = "OneTimeSyncWorker"
    private const val TAG = "SyncScheduler"

    fun enqueueOneTimeSync(context: Context) {
         if (!SharedPreferencesUtils(context.applicationContext).isUserLoggedIn()) {
             Log.d(TAG, "User not logged in, skipping one-time sync request.")
             return
         }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeSyncRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )

            .build()

        Log.d(TAG, "One-time sync work enqueued (or kept if existing).")
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_ONETIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP, // KEEP is often suitable for app start sync
            oneTimeSyncRequest
        )
        Log.d(TAG, "One-time sync work enqueued (or kept if existing).")
    }
}
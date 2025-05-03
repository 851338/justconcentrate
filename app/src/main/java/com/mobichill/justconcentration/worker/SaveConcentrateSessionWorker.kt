package com.mobichill.justconcentration.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_CANCEL_SESSION
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_SESSION_COMPLETE
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_INTENT_ACTION
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_CONFIG_DURATION
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_DATE
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_GOAL
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_START_TIME
import com.mobichill.justconcentration.helper.FireStoreHelper
import com.mobichill.justconcentration.manager.BadgeProgressManager
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import java.util.concurrent.TimeUnit

class SaveConcentrateSessionWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    private val TAG = javaClass.simpleName
    private val sessionRepository: ConcentrateSessionRepository by lazy {
        (appContext.applicationContext as MyApp).concentrateSessionRepository
    }
    private val badgeProgressManager: BadgeProgressManager by lazy {
        (appContext.applicationContext as MyApp).badgeProgressManager
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started.")

        // Get inputData
        val startTime = inputData.getLong(KEY_SESSION_START_TIME, -1L)
        val configuredDuration = inputData.getInt(KEY_SESSION_CONFIG_DURATION, -1)
        val sessionDate = inputData.getString(KEY_SESSION_DATE) // Make non-null if required
        val intentAction = inputData.getString(KEY_INTENT_ACTION)
        val goal = inputData.getString(KEY_SESSION_GOAL)
            ?: appContext.getString(R.string.complete_a_session)

        // Validate inputData
        if (startTime == -1L || configuredDuration == -1 || sessionDate == null) {
            Log.e(TAG, "Invalid input data received. Failing worker.")
            return Result.failure()
        }

        // Construct/Determine Final Session State
        // Create a base session object from input data
        val sessionInProgress = ConcentrateSessionModel(
            startTime = startTime,
            goal = goal,
            durationMinutes = configuredDuration, // Start with configured
            date = sessionDate,
            endTime = startTime + TimeUnit.MINUTES.toMillis(configuredDuration.toLong()), // Default end time
            wasCompleted = false, // Default completion state
            isSynced = false // Default sync state

        )
        val sessionToSave: ConcentrateSessionModel = when (intentAction) {
            ACTION_CANCEL_SESSION -> {
                val actualEndTime = System.currentTimeMillis()
                val actualDurationMillis = actualEndTime - sessionInProgress.startTime
                val actualDurationMinutes =
                    TimeUnit.MILLISECONDS.toMinutes(actualDurationMillis).toInt().coerceAtLeast(0)

                Log.d(TAG, "Session cancelled. Actual duration: $actualDurationMinutes min")
                sessionInProgress.copy(
                    endTime = actualEndTime,
                    durationMinutes = actualDurationMinutes,
                    wasCompleted = false
                )
            }

            ACTION_SESSION_COMPLETE -> {
                Log.d(TAG, "Session completed. Duration: ${sessionInProgress.durationMinutes} min")
                sessionInProgress.copy(wasCompleted = true)
            }

            else -> {
                Log.w(
                    TAG,
                    "Unknown or null intent action. Saving session as incomplete with configured duration."
                )
                sessionInProgress
            }
        }

        // Attempt FireStore sync
        var isSyncedSuccessfully = false
        if (SharedPreferencesUtils(applicationContext).isUserLoggedIn() && Utils.isNetworkAvailable(appContext)) {
            try {
                Log.d(TAG, "Attempting FireStore sync for session ${sessionToSave.id}")
                // Sync the potentially modified session
                FireStoreHelper.getInstance()
                    .addConcentrateSessionToFireStore(sessionToSave.copy(isSynced = true)) // Try FireStore with isSynced=true
                isSyncedSuccessfully = true // Mark as synced ONLY if FireStore call succeeds
                Log.d(TAG, "FireStore sync SUCCESS for session ${sessionToSave.id}")
            } catch (e: Exception) {
                Log.e(TAG, "FireStore sync FAILED for session ${sessionToSave.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on FireStore failure
            }
        } else {
            Log.d(
                TAG,
                "Skipping FireStore sync (Conditions not met) for session ${sessionToSave.id}"
            )
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }

        // Always save to Room with the final determined state
        try {
            Log.d(
                TAG,
                "Saving final session state to Room ${sessionToSave.id} (Completed: ${sessionToSave.wasCompleted}, Duration: ${sessionToSave.durationMinutes}, Synced: $isSyncedSuccessfully)"
            )
            sessionRepository.addConcentrateSessionToRoom(
                sessionToSave.copy(isSynced = isSyncedSuccessfully) // Use the final sync status
            )
            Log.d(TAG, "Room save successful for session ${sessionToSave.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Room save FAILED for session ${sessionToSave.id}", e)
            // If Room save fails, we cannot reliably update achievements
            return Result.failure()
        }

        Log.d(TAG, "Session completed. Fetching stats for achievement update...")
        val totalSessions = sessionRepository.getTotalCompletedSessionCount()
        val totalMinutes = sessionRepository.getTotalFocusMinutes()
        val focusStreakDays = sessionRepository.getCurrentFocusStreak()

        // Basic validation
        if (totalSessions < 0 || totalMinutes < 0 || focusStreakDays < 0) {
            Log.e(TAG, "Invalid input data")
            return Result.failure()
        }
        Log.d(
            TAG,
            "Stats fetched: Sessions=$totalSessions, Minutes=$totalMinutes, Streak=$focusStreakDays"
        )

        return try {
            badgeProgressManager.updateAfterFocusSession(
                totalSessions = totalSessions,
                totalMinutes = totalMinutes,
                focusStreakDays = focusStreakDays
            )
            Log.d(TAG, "Achievements updated successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating achievements", e)
            Result.retry() // Or Result.failure() depending on the error
        }
    }
}

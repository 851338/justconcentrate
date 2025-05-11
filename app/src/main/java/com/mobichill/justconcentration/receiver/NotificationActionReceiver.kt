package com.mobichill.justconcentration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_SESSION
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_CANCEL_SESSION
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_SESSION_COMPLETE
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_INTENT_ACTION
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_DATE
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_GOAL
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_START_TIME
import com.mobichill.justconcentration.constants.Constants.OTHERS.KEY_SESSION_CONFIG_DURATION
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.service.FocusService
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.worker.SaveConcentrateSessionWorker
import androidx.work.workDataOf

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        private val TAG = NotificationActionReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action // Get action early
        Log.d(TAG, "onReceive triggered for action: $action")

        // Get session from service
        val session = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(FOCUS_SESSION, ConcentrateSessionModel::class.java) // API 33+
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(FOCUS_SESSION) // API 26-32
        }

        if (session == null)
            return
        // Handle immediate main thread actions first (toast, stop service)
        handleIntentActions(context, intent)

        //Calling worker here
        val sessionData = workDataOf(
            KEY_SESSION_START_TIME to session.startTime,
            KEY_SESSION_CONFIG_DURATION to session.durationMinutes,
            KEY_SESSION_DATE to session.date,
            KEY_INTENT_ACTION to intent.action,
            KEY_SESSION_GOAL to session.goal
        )
        val saveConcentrateSessionWorker =
            OneTimeWorkRequestBuilder<SaveConcentrateSessionWorker>()
                .setInputData(sessionData)
                .build()
        WorkManager.getInstance(context).enqueue(saveConcentrateSessionWorker)
    }

    private fun handleIntentActions(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL_SESSION -> {
                Log.d(TAG, "Action: Cancel Session - Stopping Service")
                try {
                    val stopIntent = Intent(context, FocusService::class.java)
                    context.stopService(stopIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping service", e)
                }
                Utils.showToast(context, context.getString(R.string.focus_session_canceled))
            }

            ACTION_SESSION_COMPLETE -> {
                Log.d(TAG, "Action: Session Complete - Showing Toast")
                // Service should stop itself on completion before sending this intent
                Utils.showToast(context, context.getString(R.string.focus_session_completed))
            }

            else -> {
                Log.w(TAG, "Received unhandled action: ${intent.action}")
            }
        }
    }
}

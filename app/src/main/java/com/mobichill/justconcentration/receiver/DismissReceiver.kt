package com.mobichill.justconcentration.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.manager.VibrationManager
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.REQUEST_CODE
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.TASK_ID
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.service.AlarmService
import com.mobichill.justconcentration.util.SFUtils
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class DismissReceiver : BroadcastReceiver() {
    private val TAG = javaClass.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(REQUEST_CODE, 0)
        val taskId = intent.getStringExtra(TASK_ID) ?: return
        val pendingResult = goAsync() // Like await

        // Cancel the alarm receiver
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Cancel the notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(requestCode)

        // Stop the alarm sound service
        val stopIntent = Intent(context, AlarmService::class.java)
        context.stopService(stopIntent)

        //Alarm completed
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(3000) { // 3 seconds timeout
                    updateTaskStatusToDismissed(context, taskId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateTaskStatusToDismissed: ", e)
            } finally {
                pendingResult.finish() //Work done
            }
        }
        VibrationManager.stopVibration()
        Utils.showToast(context, context.getString(R.string.alarm_dismissed))
    }

    private suspend fun updateTaskStatusToDismissed(context: Context, taskId: String) {
        val task = MyApp.instance.taskRepository.getTaskById(taskId)
        task.collect { t ->
            //update to fire store if connected and logged in
            var isSynced = false
            if (Utils.isNetworkAvailable(context) && SFUtils.isUserLoggedIn(context)) {
                isSynced = true
                FireStoreRepository().updateTaskToFireStore(
                    t.copy(
                        isSynced = true,
                        completed = true
                    )
                ) { complete, error ->
                    if (complete) {
                        Log.d(TAG, context.getString(R.string.alarm_updated_success))
                    } else {
                        Log.e(TAG, "Update alarm status:" + error?.message.toString())
                    }
                }
            } else isSynced = false
            //update to room
            MyApp.instance.taskRepository.updateTaskToRoom(
                t.copy(
                    completed = true,
                    isSynced = isSynced
                )
            )
        }
    }
}

package com.mobichill.justconcentration.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.repository.RoomRepository
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.REQUEST_CODE
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.TASK_ID
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DismissReceiver : BroadcastReceiver() {
    private val  TAG = javaClass.canonicalName

    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(REQUEST_CODE, 0)
        val taskId = intent.getStringExtra(TASK_ID) ?: return

        // Cancel the alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)

        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(requestCode)

        //Alarm completed
        CoroutineScope(Dispatchers.IO).launch {
            updateTaskStatusToDismissed(context, taskId)
        }
        Utils.showToast(context, context.getString(R.string.alarm_dismissed))
    }

    private suspend fun updateTaskStatusToDismissed(context: Context, taskId: String) {
        val roomRepository = RoomRepository(RoomHelper.getInstance(context))
        val task = roomRepository.getTaskById(taskId)
        task.collect { t ->
            t.completed = true
            //update to fire store if connected and logged in
            if (Utils.isNetworkAvailable(context) && Utils.isUserLoggedIn(context))
                FireStoreRepository().updateTaskToFireStore(t) { complete, error ->
                    if (complete) {
                        Log.d(TAG, context.getString(R.string.alarm_updated_success))
                    } else {
                        Log.e(TAG, "Update alarm status:" + error?.message.toString())
                    }
                }
            //update to room
            roomRepository.updateTaskToRoom(t)
        }
    }
}

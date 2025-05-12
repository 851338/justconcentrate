package com.mobichill.justconcentration.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.manager.VibrationManager
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.SNOOZE_MINUTES
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.TASK_ID
import com.mobichill.justconcentration.di.ReceiverDependencies
import com.mobichill.justconcentration.service.AlarmService
import com.mobichill.justconcentration.utils.Utils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SnoozeReceiver : BroadcastReceiver() {
    companion object {
        private val TAG = SnoozeReceiver::class.java.simpleName
    }

    private lateinit var task: TaskModel

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TASK_ID) ?: return
        var snoozeMinutes = intent.getIntExtra(SNOOZE_MINUTES, 5) // Default 5 min

        val dependencies = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverDependencies::class.java
        )
        val taskRepository = dependencies.taskRepository()

        CoroutineScope(Dispatchers.IO).launch {
            task = taskRepository.getTaskById(taskId).firstOrNull() ?: return@launch

            // Cancel the alarm receiver
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.requestCode,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelling previous alarm for requestCode: ${task.requestCode}")

            // Cancel the notification
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(task.requestCode)

            // Stop the alarm sound service
            val stopIntent = Intent(context, AlarmService::class.java)
            context.stopService(stopIntent)

            //todo testing
            snoozeMinutes = 1
            val newReminderTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000
            val alarmHelper = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ReceiverDependencies::class.java
            ).alarmHelper()
            alarmHelper.setAlarm(context, newReminderTime, task.requestCode, task.alarmSoundUri)
            withContext(Dispatchers.Main) {
                Utils.showToast(
                    context,
                    context.getString(R.string.task_snoozed_for_minutes, snoozeMinutes)
                )
            }
            Log.d(TAG, "Setting new alarm at $newReminderTime for task: ${task.taskText}")
            VibrationManager.stopVibration()
        }
    }
}
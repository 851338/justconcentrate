package com.mobichill.justconcentration.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.ALARM_URI
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.REQUEST_CODE
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.SNOOZE_MINUTES
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.TASK_ID
import com.mobichill.justconcentration.others.Constants.OTHERS.ALARM_CHANNEL
import com.mobichill.justconcentration.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = this::class.java.canonicalName
    private var task: TaskModel? = null

    override fun onReceive(context: Context, intent: Intent?) {

        val alarmUri = intent?.getStringExtra(ALARM_URI) ?: ""
        val requestCode = intent?.getIntExtra(REQUEST_CODE, 0) ?: 0
        // Launch a coroutine to fetch the task
        CoroutineScope(Dispatchers.IO).launch {
            task = MyApp.instance.taskRepository.getTaskByRequestCode(requestCode).firstOrNull()
            if (task == null)
                return@launch
            // Now switch to the Main thread to show the notification (UI operation)
            withContext(Dispatchers.Main) {
                showNotification(context, task!!.taskText)
            }
            Log.d(TAG, "Alarm received for task: ${task!!.taskText} at ${System.currentTimeMillis()}")

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra(REQUEST_CODE, requestCode)
                putExtra(ALARM_URI, alarmUri)
            }
            context.startForegroundService(serviceIntent)
        }
    }

    private fun showNotification(context: Context, taskTitle: String) {
        val channelId = ALARM_CHANNEL
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val dismissIntent = createDismissIntent(context, task!!)

        // Snooze durations in minutes
        val snoozeTimes = listOf(5, 10, 30)

        val actions = snoozeTimes.map { snoozeMinutes ->
            val snoozeIntent = createSnoozeIntent(context, task!!, snoozeMinutes)
            NotificationCompat.Action.Builder(
                R.drawable.ic_snooze, "Snooze $snoozeMinutes min", snoozeIntent
            ).build()
        }

        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.ic_dismiss, "Dismiss", dismissIntent
        ).build()

        // Create Notification Channel (Android 8.0+)
        val channel = NotificationChannel(
            channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Task alarm!")
            .setContentText(taskTitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(dismissAction)
            .setAutoCancel(true)

        actions.forEach { builder.addAction(it) }

        notificationManager.notify(task!!.requestCode, builder.build())
    }

    private fun createSnoozeIntent(context: Context, task: TaskModel, minutes: Int): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(TASK_ID, task.id)
            putExtra(SNOOZE_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            (task.requestCode * 100) + minutes,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDismissIntent(context: Context, task: TaskModel): PendingIntent {
        val intent = Intent(context, DismissReceiver::class.java).apply {
            putExtra(TASK_ID, task.id)
            putExtra(REQUEST_CODE, task.requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}


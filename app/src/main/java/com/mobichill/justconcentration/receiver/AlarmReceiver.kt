package com.mobichill.justconcentration.receiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.RoomRepository
import com.mobichill.justconcentration.service.AlarmService
import com.mobichill.justconcentration.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmReceiver : BroadcastReceiver() {
    private lateinit var task: TaskModel

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {

        val alarmUri = intent?.getStringExtra("ALARM_URI") ?: ""
        val requestCode = intent?.getIntExtra("REQUEST_CODE", 0) ?: 0
        CoroutineScope(Dispatchers.IO).launch {
            RoomRepository(RoomHelper.getInstance(context)).getTaskByRequestCode(requestCode)
                .collect { t ->
                    task = t
                }
        }
        // Trigger
        showNotification(context, task.taskText)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("REQUEST_CODE", requestCode)
            putExtra("ALARM_URI", alarmUri)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun showNotification(context: Context, taskTitle: String) {
        val channelId = "task_reminder_channel"
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Create Notification Channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Reminder")
            .setContentText(taskTitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}


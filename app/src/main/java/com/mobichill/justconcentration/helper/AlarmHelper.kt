package com.mobichill.justconcentration.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.mobichill.justconcentration.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AlarmHelper {
    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun setAlarm(context: Context, triggerTime: Long, requestCode: Int, alarmUri: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("REQUEST_CODE", requestCode)
            putExtra("ALARM_URI", alarmUri)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        //Used when you need to trigger an alarm even if the device is asleep.
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
        )
    }

    //use to cancel after update alarm in task or triggered alarm
    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java) // Use the same receiver
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    //rescheduleAlarms after reboot
    @SuppressLint("ScheduleExactAlarm")
    fun rescheduleAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val tasks = RoomHelper.getInstance(context).taskDao().getAllTasks()
            tasks.collect { list ->
                list.forEach { task ->
                    if (task.alarmTimeMillis != 0L && task.alarmTimeMillis > System.currentTimeMillis()) {
                        setAlarm(
                            context,
                            task.alarmTimeMillis,
                            task.requestCode,
                            task.alarmSoundUri
                        )
                    }
                }
            }
        }
    }
}
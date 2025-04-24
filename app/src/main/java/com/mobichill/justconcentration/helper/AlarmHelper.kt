package com.mobichill.justconcentration.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.ALARM_URI
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.REQUEST_CODE
import com.mobichill.justconcentration.receiver.AlarmReceiver
import com.mobichill.justconcentration.util.AudioUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmHelper {
    fun setAlarm(context: Context, triggerTime: Long, requestCode: Int, alarmUri: String) {
        val finalUri = if (alarmUri.isEmpty()) AudioUtils.defaultAlarmString else alarmUri

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(REQUEST_CODE, requestCode)
            putExtra(ALARM_URI, finalUri)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        //Used when you need to trigger an alarm even if the device is asleep.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check if the app can schedule exact alarms
            if (context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            } else {
                requestExactAlarmPermission(context)
            }
        } else {
            // For devices below API 31, just schedule the alarm normally
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            )
        }
    }

    //Checked
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestExactAlarmPermission(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
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
    fun rescheduleAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val tasks = MyApp.instance.taskRepository.getAllActiveTasks()
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
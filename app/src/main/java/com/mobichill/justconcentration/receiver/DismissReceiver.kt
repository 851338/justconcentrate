package com.mobichill.justconcentration.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.manager.VibrationManager
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.REQUEST_CODE
import com.mobichill.justconcentration.service.AlarmService
import com.mobichill.justconcentration.utils.Utils

class DismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(REQUEST_CODE, 0)

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

        VibrationManager.stopVibration()
        Utils.showToast(context, context.getString(R.string.alarm_dismissed))
    }
}

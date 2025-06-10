package com.mobichill.justconcentration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobichill.justconcentration.worker.RescheduleAlarmsWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val rescheduleRequest = OneTimeWorkRequestBuilder<RescheduleAlarmsWorker>().build()
            WorkManager.getInstance(context).enqueue(rescheduleRequest)
        }
    }
}
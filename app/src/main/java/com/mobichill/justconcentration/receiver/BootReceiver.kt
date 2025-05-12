package com.mobichill.justconcentration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobichill.justconcentration.di.ReceiverDependencies
import dagger.hilt.android.EntryPointAccessors

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarms here
            val alarmHelper = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ReceiverDependencies::class.java
            ).alarmHelper()
            alarmHelper.rescheduleAlarms(context)
        }
    }
}
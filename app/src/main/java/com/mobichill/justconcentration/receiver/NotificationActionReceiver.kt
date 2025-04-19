package com.mobichill.justconcentration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobichill.justconcentration.service.FocusService
import com.mobichill.justconcentration.util.Constants.OTHERS.ACTION_CANCEL_SESSION
import com.mobichill.justconcentration.util.Constants.OTHERS.ACTION_SESSION_COMPLETE
import com.mobichill.justconcentration.util.Utils

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL_SESSION -> {
                val stopIntent = Intent(context, FocusService::class.java)
                context.stopService(stopIntent)
                Utils.showToast(context, "Focus session canceled")
            }
            ACTION_SESSION_COMPLETE -> {
                Utils.showToast(context, "Focus session completed 🎉")
            }
        }
    }
}
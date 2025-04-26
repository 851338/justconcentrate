package com.mobichill.justconcentration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.application.MyApp
import com.mobichill.justconcentration.helper.FireStoreHelper
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.FOCUS_SESSION
import com.mobichill.justconcentration.others.Constants.OTHERS.ACTION_CANCEL_SESSION
import com.mobichill.justconcentration.others.Constants.OTHERS.ACTION_SESSION_COMPLETE
import com.mobichill.justconcentration.service.FocusService
import com.mobichill.justconcentration.util.SFUtils
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val session = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(FOCUS_SESSION, ConcentrateSessionModel::class.java) // API 33+
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(FOCUS_SESSION) // API 26-32
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (session != null) {
                var isSynced = false
                if (SFUtils.isUserLoggedIn(context) && Utils.isNetworkAvailable(context)) {
                    isSynced = true
                    FireStoreHelper.getInstance()
                        .addConcentrateSessionToFireStore(session.copy(isSynced = true))
                } else isSynced = false
                MyApp.instance.concentrateSessionRepository.addConcentrateSessionToRoom(
                    session.copy(
                        isSynced = isSynced
                    )
                )
            }
        }
        when (intent.action) {
            ACTION_CANCEL_SESSION -> {
                val stopIntent = Intent(context, FocusService::class.java)
                context.stopService(stopIntent)
                Utils.showToast(context, context.getString(R.string.focus_session_canceled))
            }

            ACTION_SESSION_COMPLETE -> {
                Utils.showToast(context, context.getString(R.string.focus_session_completed))
            }
        }
    }
}
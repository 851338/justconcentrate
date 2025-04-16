package com.mobichill.justconcentration.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.RoomRepository
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.SNOOZE_MINUTES
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.TASK_ID
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnoozeReceiver : BroadcastReceiver() {
    private lateinit var task: TaskModel

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TASK_ID) ?: return
        val snoozeMinutes = intent.getIntExtra(SNOOZE_MINUTES, 5) // Default 5 min

        CoroutineScope(Dispatchers.IO).launch {
            RoomRepository(RoomHelper.Companion.getInstance(context)).getTaskById(taskId)
                .collect { t ->
                    task = t
                }
        }

        val newReminderTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000
        AlarmHelper().setAlarm(context, newReminderTime, task.requestCode, task.alarmSoundUri)
        Utils.showToast(
            context,
            context.getString(R.string.task_snoozed_for_minutes, snoozeMinutes)
        )
    }
}
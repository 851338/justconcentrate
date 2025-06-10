package com.mobichill.justconcentration.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RescheduleAlarmsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val alarmHelper: AlarmHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val tasks = taskRepository.getAllActiveTasks().first()
            tasks.forEach { task ->
                if (task.alarmTimeMillis != 0L && task.alarmTimeMillis > System.currentTimeMillis()) {
                    alarmHelper.setAlarm(
                        applicationContext,
                        task.alarmTimeMillis,
                        task.requestCode,
                        task.alarmSoundUri
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
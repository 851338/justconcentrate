package com.mobichill.justconcentration.helper

import android.util.Log
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMAT
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatsCalculateHelper(
    private val taskRepository: TaskRepository,
    private val sessionRepository: ConcentrateSessionRepository
) {

    companion object {
        private val TAG = StatsCalculateHelper::class.java.simpleName
    }
    /**
     * Calculates total focus duration per day within a range.
     * Returns a map of "MMM-dd, yyyy" to total minutes.
     */
    suspend fun getFocusTimeTrend(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val sessions = sessionRepository.getCompletedSessionsBetween(startTimeMillis, endTimeMillis)
        val trendData = mutableMapOf<String, Int>()

        // Efficiently format dates
        val dateFormat = java.text.SimpleDateFormat(DATE_FORMAT, java.util.Locale.getDefault())

        sessions.forEach { session ->
            // Ensure endTime is valid and within range (DAO query should handle range, but double-check)
            if (session.endTime in (startTimeMillis + 1)..endTimeMillis && session.wasCompleted) {
                val dateKey = dateFormat.format(java.util.Date(session.endTime))
                trendData[dateKey] = (trendData[dateKey] ?: 0) + session.durationMinutes
            }
        }

        // Optional: Fill gaps with 0s for the chart (more complex)
        // You'd iterate from start date to end date and add entries if missing

        return@withContext trendData
    }

    /**
     * Calculates session success rate for sessions STARTED within a range.
     * Returns percentage (0.0 to 100.0).
     */
    suspend fun getSessionSuccessRate(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Double = withContext(Dispatchers.IO) {
        val sessionsStarted = sessionRepository.getSessionsStartedBetween(startTimeMillis, endTimeMillis)
        if (sessionsStarted.isEmpty()) {
            return@withContext 0.0
        }

        val completedCount = sessionsStarted.count { it.wasCompleted }
        return@withContext (completedCount.toDouble() / sessionsStarted.size.toDouble()) * 100.0
    }

    /**
     * Calculates task completion rate for tasks CREATED within a range.
     * Excludes deleted tasks. Returns percentage (0.0 to 100.0).
     */
    suspend fun getTaskCompletionRate(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Double = withContext(Dispatchers.IO) {

        val tasksFromRepo = taskRepository.getTasksCreatedBetween(startTimeMillis, endTimeMillis)
        val tasksCreated = tasksFromRepo.filter {
            it.deletedAt == null
        }

        if (tasksCreated.isEmpty())
            return@withContext 0.0

        val completedCount = tasksCreated.count { it.completed } // Uses 'completed' flag
        return@withContext (completedCount.toDouble() / tasksCreated.size.toDouble()) * 100.0
    }

    /**
     * Analyzes tasks DUE within a range.
     * Requires TaskModel.dueDate to be implemented and populated!
     * Returns analysis data.
     */
    data class OverdueAnalysisResult(
        val tasksDueInPeriod: Int = 0,
        val completedLate: Int = 0,
        val currentlyOverdue: Int = 0
        // Add percentages if needed
    )

    suspend fun getOverdueTaskAnalysis(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): OverdueAnalysisResult = withContext(Dispatchers.IO) {
        val tasksDue = taskRepository.getTasksDueBetween(startTimeMillis, endTimeMillis)
            .filter { it.deletedAt == null && it.dueDate != 0L && it.dueDate > 0 } // Filter relevant

        var countDue = 0
        var countLate = 0
        var countOverdue = 0
        val now = System.currentTimeMillis()

        tasksDue.forEach { task ->
            Log.d(TAG, "$task")
            // Only consider tasks actually due in the period based on their dueDate
            if (task.dueDate in startTimeMillis..endTimeMillis) {
                countDue++
                if (task.completed && task.completedAt != null) {
                    if (task.completedAt > task.dueDate) {
                        countLate++
                    }
                } else if (!task.completed) { // Not completed
                    if (now > task.dueDate) {
                        countOverdue++
                    }
                }
            }
        }

        return@withContext OverdueAnalysisResult(
            tasksDueInPeriod = countDue,
            completedLate = countLate,
            currentlyOverdue = countOverdue
        )
    }
}
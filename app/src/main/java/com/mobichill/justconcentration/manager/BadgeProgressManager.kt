package com.mobichill.justconcentration.manager

import com.mobichill.justconcentration.base.application.MyApp
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class BadgeProgressManager {
    private val myApplication = MyApp.instance
    private val badgeRepository = myApplication.badgeRepository
    private val taskRepository = myApplication.taskRepository

    suspend fun updateAfterTaskCompletion(
        totalTasks: Int,
        taskTime: LocalTime,
        completedDate: LocalDate,
        currentDate: LocalDate,
        taskStreakDays: Int
    ) {
        update("task_1", totalTasks, 1)
        update("task_10", totalTasks, 10)
        update("task_100", totalTasks, 100)
        update("task_streak", taskStreakDays, 5)

        if (completedDate == currentDate) {
            val calendar = Calendar.getInstance()

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            val todayCompletedCount =
                taskRepository.getTodayCompletedTaskCount(startOfDay, endOfDay)
            update("today_star", todayCompletedCount, 3)
        }

        if (taskTime.hour >= 21) {
            update("night_owl", 1, 1)
        }

        if (taskTime.hour < 7) {
            update("early_bird", 1, 1)
        }
    }

    suspend fun updateAfterFocusSession(
        totalSessions: Int,
        totalMinutes: Int,
        focusStreakDays: Int
    ) {
        update("focus_1", totalSessions, 1)
        update("focus_5", totalSessions, 5)
        update("focus_hour_5", totalMinutes / 60, 5)
        update("focus_hour_25", totalMinutes / 60, 25)
        update("focus_streak_3", focusStreakDays, 3)
    }

    suspend fun updateLoginStreak(days: Int) {
        update("streak_3", days, 3)
    }

    suspend fun updateComeback(lastActiveDaysAgo: Int) {
        if (lastActiveDaysAgo >= 3) {
            update("comeback", 1, 1)
        }
    }

    private suspend fun update(badgeId: String, progress: Int, goal: Int) {
        val badge = badgeRepository.getBadgeById(badgeId).firstOrNull()
        if (badge == null) return
        if (badge.isUnlocked) return

        val updatedBadge = badge.copy(
            progress = progress.coerceAtMost(goal),
            goal = goal,
            isUnlocked = progress >= goal,
            unlockedAt = if (progress >= goal) System.currentTimeMillis() else null
        )

        badgeRepository.insertBadge(updatedBadge)
//        syncBadgeToFireStore(userId, updatedBadge)
    }
}

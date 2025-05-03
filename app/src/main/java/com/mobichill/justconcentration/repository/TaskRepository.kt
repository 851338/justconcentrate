package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.model.TaskModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TaskRepository(private val taskDAO: TaskDAO) {

    suspend fun saveTaskToRoom(taskModel: TaskModel) {
        taskDAO.insertTask(taskModel)
    }

    suspend fun upsertTask(taskModel: TaskModel) {
        taskDAO.upsertTask(taskModel)
    }

    suspend fun markTaskAsSyncedById(id: String, serverTimestampMillis: Long) {
        taskDAO.markTaskAsSyncedById(id, serverTimestampMillis)
    }

    suspend fun markTasksAsSyncedAfterUpload(ids: List<String>) {
        taskDAO.markTasksAsSyncedAfterUpload(ids)
    }

    suspend fun updateTaskToRoom(taskModel: TaskModel) {
        taskDAO.updateTask(taskModel)
    }

    suspend fun deleteTask(taskModel: TaskModel) {
        // Actually just mark it as deleted it
        var updatedTask =
            taskModel.copy(deletedAt = System.currentTimeMillis(), isSynced = false)
        taskDAO.updateTask(updatedTask)
    }

    suspend fun deleteTaskPermanentlyById(id: String) {
        taskDAO.deleteTaskPermanentlyById(id)
    }

    suspend fun deleteTasksPermanentlyByIds(ids: List<String>) {
        taskDAO.deleteTasksPermanentlyByIds(ids)
    }

    fun getTaskById(taskId: String): Flow<TaskModel> = taskDAO.getTaskById(taskId)

    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel> =
        taskDAO.getTaskByRequestCode(requestCode)

    fun searchTasks(query: String): Flow<List<TaskModel>> = taskDAO.searchTasks(query)

    suspend fun getUnsyncedActiveTasks(): List<TaskModel> = taskDAO.getUnsyncedActiveTasks()

    suspend fun getUnsyncedDeletedTasks(): List<TaskModel> = taskDAO.getUnsyncedDeletedTasks()

    suspend fun getTodayCompletedTaskCount(startOfDay: Long, endOfDay: Long): Int =
        taskDAO.getTodayCompletedTaskCount(startOfDay, endOfDay)

    suspend fun getCompletedTaskCount(): Int = taskDAO.getCompletedTaskCount()

    fun getAllActiveTasks(): Flow<List<TaskModel>> = taskDAO.getAllActiveTasks()

    suspend fun getCompletedTaskDates(): List<String> = taskDAO.getCompletedTaskDates()
}
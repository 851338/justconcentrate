package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.helper.TaskHelper
import com.mobichill.justconcentration.model.TaskModel
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskHelper: TaskHelper) {

    fun searchTasks(query: String): Flow<List<TaskModel>> = taskHelper.searchTasks(query)

    suspend fun deleteTask(taskModel: TaskModel) {
        taskHelper.deleteTask(taskModel)
    }

    suspend fun syncTasksToRoom(tasks: List<TaskModel>) {
        taskHelper.syncTasksToRoom(tasks)
    }

    suspend fun saveTaskToRoom(taskModel: TaskModel) {
        taskHelper.saveTaskToRoom(taskModel)
    }

    suspend fun updateTaskToRoom(taskModel: TaskModel) {
        taskHelper.updateTaskToRoom(taskModel)
    }

    fun getAllActiveTasks(): Flow<List<TaskModel>> = taskHelper.getAllActiveTasks()

    fun getTaskById(taskId: String): Flow<TaskModel> = taskHelper.getTaskById(taskId)

    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel> =
        taskHelper.getTaskByRequestCode(requestCode)

    suspend fun getUnsyncedActiveTasks(): List<TaskModel> = taskHelper.getUnsyncedActiveTasks()

    suspend fun getUnsyncedDeletedTasks(): List<TaskModel> = taskHelper.getUnsyncedDeletedTasks()
}
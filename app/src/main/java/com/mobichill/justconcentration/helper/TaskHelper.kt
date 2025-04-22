package com.mobichill.justconcentration.helper

import android.util.Log
import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.model.TaskModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TaskHelper(private val taskDAO: TaskDAO) {

    suspend fun syncTasksToRoom(fireStoreTasks: List<TaskModel>) {
        fireStoreTasks.forEach { firestoreTask ->
            val roomTask = taskDAO.getTaskById(firestoreTask.id).firstOrNull()
            if (roomTask == null)
                taskDAO.insertTask(firestoreTask.copy(isSynced = true))
            else
            // If not synced from Room to FireStore, skip
                if (!roomTask.isSynced) {
                    return@forEach
                } else taskDAO.updateTask(firestoreTask)
        }
    }

    suspend fun saveTaskToRoom(taskModel: TaskModel) {
        try {
            val rowId = taskDAO.insertTask(taskModel)
            if (rowId != -1L) {
                // Success
                Log.d("aaaaa", "success")
            } else {
                Log.e("aaaaa", "failed")
                // Insert failed
            }
            val tasks = taskDAO.getAllActiveTasks()
            tasks.collect { tasks ->
                Log.d("DB", "Tasks count: ${tasks.size}")
            }
            tasks.collect { tasks ->
                tasks.forEach { Log.d("DB", "Task: ${it.taskText}") }
            }
        } catch (e: Exception) {
            Log.e("Insert", "Error inserting task: ${e.message}")
        }
        taskDAO.insertTask(taskModel)
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

    fun getTaskById(taskId: String): Flow<TaskModel> = taskDAO.getTaskById(taskId)

    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel> =
        taskDAO.getTaskByRequestCode(requestCode)

    fun searchTasks(query: String): Flow<List<TaskModel>> = taskDAO.searchTasks(query)

    suspend fun getUnsyncedActiveTasks(): List<TaskModel> = taskDAO.getUnsyncedActiveTasks()

    suspend fun getUnsyncedDeletedTasks(): List<TaskModel> = taskDAO.getUnsyncedDeletedTasks()

    fun getAllActiveTasks(): Flow<List<TaskModel>> = taskDAO.getAllActiveTasks()

}
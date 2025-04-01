package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.helper.RoomHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RoomRepository(db: RoomHelper) {
    private val taskDao = db.taskDao()
    private val userDao = db.userDao()

    fun getAllTasks(): Flow<List<TaskModel>> = taskDao.getAllTasks()

    suspend fun saveTasksToRoom(tasks: List<TaskModel>) {
        taskDao.clearTasks() // Remove old data
        taskDao.insertTasks(tasks)
    }

    suspend fun saveTaskToRoom(taskModel: TaskModel) {
        taskDao.insertTask(taskModel)
    }

    suspend fun updateTaskToRoom(taskModel: TaskModel) {
        taskDao.updateTask(taskModel)
    }

    fun getTaskById(taskId: Int): Flow<TaskModel> {
        return taskDao.getTaskById(taskId)
    }

    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel> {
        return taskDao.getTaskByRequestCode(requestCode)
    }

    fun saveUserToRoom(userModel: UserModel) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingUser = userDao.getUserById(userModel.uid)
            val createdAt = existingUser?.createdAt ?: System.currentTimeMillis()
            if (existingUser == null) {
                // Insert if new user
                userModel.createdAt = createdAt
                userDao.insertUser(userModel)
            } else {
                // Update existing user without changing createdAt
                userDao.updateUser(userModel.copy(createdAt = existingUser.createdAt))
            }
        }
    }
}
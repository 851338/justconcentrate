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

    fun getAllActiveTasks(): Flow<List<TaskModel>> = taskDao.getAllActiveTasks()
    fun getAllDeletedTasks(): Flow<List<TaskModel>> = taskDao.getAllDeletedTasks()

    suspend fun getUserById(userId: String): UserModel? {
        return userDao.getUserById(userId)
    }

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

    fun getTaskById(taskId: String): Flow<TaskModel> {
        return taskDao.getTaskById(taskId)
    }

    fun getTaskByRequestCode(requestCode: Int): Flow<TaskModel> {
        return taskDao.getTaskByRequestCode(requestCode)
    }

    fun searchTasks(query: String): Flow<List<TaskModel>> {
        return taskDao.searchTasks(query)
    }

    suspend fun removeOrRestoreTask(taskModel: TaskModel, isRemove: Boolean) {
        var updatedTask =
            taskModel.copy(deletedAt = if (isRemove) System.currentTimeMillis() else null)
        taskDao.updateTask(updatedTask)
    }

    //google, signIn, signUp
    fun saveUserToRoom(userModel: UserModel) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingUser = userDao.getUserById(userModel.uid)
            if (existingUser == null) {
                // Insert if new user
                userDao.insertUser(userModel)
            } else {
                // Update existing user without changing createdAt
                userDao.updateUser(userModel.copy(createdAt = existingUser.createdAt))
            }
        }
    }
}
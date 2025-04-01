package com.mobichill.justconcentration.repository

import android.content.Context
import android.util.Log
import com.mobichill.justconcentration.helper.FireStoreHelper
import com.mobichill.justconcentration.model.TaskModel

class FireStoreRepository {
    private val TAG = javaClass.canonicalName
    suspend fun getTasksFromFireStore(): List<TaskModel> {
        return FireStoreHelper.getInstance().getAllTasksFromFireStore()
    }

    fun saveTaskToFireStore(task: TaskModel) {
        FireStoreHelper.getInstance().saveTaskToFireStore(task)
    }

    fun updateTaskToFireStore(task: TaskModel) {
        FireStoreHelper.getInstance().updateTaskToFireStore(task, {
            Log.d(TAG, "Task updated successfully!")
        }, { exception ->
            Log.e(TAG, "Error updating task: ${exception.message}")
            exception.printStackTrace()
        })
    }

    fun checkIfUserExists(
        context: Context,
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ) {
        FireStoreHelper.getInstance().checkIfUserExistsAndSave(context, uid, name, email, photoUrl)
    }

    fun addNewUserBySigningUp(
        context: Context,
        userId: String,
        username: String,
        hashedPassword: String
    ) {
        FireStoreHelper.getInstance()
            .addNewUserBySigningUp(context, userId, username, hashedPassword)
    }
}
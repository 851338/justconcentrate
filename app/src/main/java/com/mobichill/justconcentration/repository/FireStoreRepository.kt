package com.mobichill.justconcentration.repository

import android.content.Context
import com.mobichill.justconcentration.helper.FireStoreHelper
import com.mobichill.justconcentration.model.TaskModel

class FireStoreRepository {
    fun getTasksFromFireStore(onCompleted: (List<TaskModel>, Exception?) -> Unit) {
        FireStoreHelper.getInstance().getAllTasksFromFireStore { onComplete, e ->
            if (onComplete.isNotEmpty())
                onCompleted(onComplete, null)
            else onCompleted(emptyList(), e)
        }
    }

    fun saveTaskToFireStore(task: TaskModel, onComplete: (Boolean, Exception?) -> Unit) {
        FireStoreHelper.getInstance().saveTaskToFireStore(task, onComplete)
    }

    fun updateTaskToFireStore(task: TaskModel, onComplete: (Boolean, Exception?) -> Unit) {
        FireStoreHelper.getInstance().updateTaskToFireStore(task, onComplete)
    }

    fun removeOrRestoreTask(
        task: TaskModel,
        onComplete: (Boolean, Exception?) -> Unit,
        isRemove: Boolean
    ) {
        FireStoreHelper.getInstance().removeOrRestoreTask(task, onComplete, isRemove)
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
        email: String,
        name: String
    ) {
        FireStoreHelper.getInstance()
            .addNewUserBySigningUp(context, userId, email, name)
    }

    fun fetchUserFromFireStore(context: Context, uid: String) {
        FireStoreHelper.getInstance().fetchUserFromFireStore(context, uid)
    }
}
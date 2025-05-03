package com.mobichill.justconcentration.repository

import android.content.Context
import com.mobichill.justconcentration.helper.FireStoreHelper
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.TaskModel

class FireStoreRepository {
    private val db = FireStoreHelper.getInstance()

    fun saveTaskToFireStore(task: TaskModel) {
        db.saveTaskToFireStore(task)
    }

    fun updateTaskToFireStore(task: TaskModel) {
        db.updateTaskToFireStore(task)
    }

    suspend fun deleteTaskFromFireStore(task: TaskModel) {
        db.deleteTaskFromFireStore(task)
    }

    fun checkIfUserExists(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ) {
        db.checkIfUserExistsAndSave(uid, name, email, photoUrl)
    }

    fun addNewUserBySigningUp(
        context: Context,
        userId: String,
        email: String,
        name: String
    ) {
        db.addNewUserBySigningUp(context, userId, email, name)
    }

    fun fetchUserFromFireStore(uid: String) {
        db.fetchUserFromFireStore(uid)
    }
}
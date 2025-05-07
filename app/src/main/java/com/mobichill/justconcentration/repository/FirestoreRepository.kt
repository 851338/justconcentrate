package com.mobichill.justconcentration.repository

import android.content.Context
import com.mobichill.justconcentration.helper.FirestoreHelper
import com.mobichill.justconcentration.model.TaskModel

class FirestoreRepository {
    private val db = FirestoreHelper.getInstance()

    fun saveTaskToFirestore(task: TaskModel) {
        db.saveTaskToFirestore(task)
    }

    fun updateTaskToFirestore(task: TaskModel) {
        db.updateTaskToFirestore(task)
    }

    suspend fun deleteTaskFromFirestore(task: TaskModel) {
        db.deleteTaskFromFirestore(task)
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

    fun fetchUserFromFirestore(uid: String) {
        db.fetchUserFromFirestore(uid)
    }

    suspend fun getSubscriptionStartDateMillis() = db.getSubscriptionStartDateMillis()


    suspend fun isUserPro() = db.isUserPro()
}
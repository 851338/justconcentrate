package com.mobichill.justconcentration.helper

import com.mobichill.justconcentration.dao.UserDAO
import com.mobichill.justconcentration.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserHelper(private val userDAO: UserDAO) {
    suspend fun getUserById(userId: String): UserModel? = userDAO.getUserById(userId)

    fun saveUserToRoom(userModel: UserModel) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingUser = userDAO.getUserById(userModel.uid)
            if (existingUser == null) {
                // Insert if new user
                userDAO.insertUser(userModel)
            } else {
                // Update existing user without changing createdAt
                userDAO.updateUser(userModel.copy(createdAt = existingUser.createdAt))
            }
        }
    }

}
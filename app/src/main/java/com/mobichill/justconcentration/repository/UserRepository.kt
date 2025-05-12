package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.dao.UserDAO
import com.mobichill.justconcentration.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val userDAO: UserDAO) {
    suspend fun getUserById(userId: String): UserModel? = userDAO.getUserById(userId)

    //google, signIn, signUp
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
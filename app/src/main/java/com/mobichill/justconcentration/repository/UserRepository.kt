package com.mobichill.justconcentration.repository

import com.mobichill.justconcentration.helper.UserHelper
import com.mobichill.justconcentration.model.UserModel

class UserRepository(private val userHelper: UserHelper) {
    //google, signIn, signUp
    fun saveUserToRoom(userModel: UserModel) {
        userHelper.saveUserToRoom(userModel)
    }

    suspend fun getUserById(userId: String): UserModel? = userHelper.getUserById(userId)
}
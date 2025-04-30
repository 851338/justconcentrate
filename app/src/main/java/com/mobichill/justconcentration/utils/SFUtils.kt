package com.mobichill.justconcentration.utils

import android.content.Context
import androidx.core.content.edit
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_IS_LOGGED_IN
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_USERID_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_USER_INFO_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_USER_SESSION_PREFS

object SFUtils {
    fun saveLoginState(context: Context, isLoggedIn: Boolean) {
        val sharedPref =
            context.getSharedPreferences(NAME_USER_SESSION_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val sharedPref =
            context.getSharedPreferences(NAME_USER_SESSION_PREFS, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_IS_LOGGED_IN, false)  // Default is false
    }

    fun saveUserInfoToSF(context: Context, userId: String) {
        //Save userid to sharedPreference
        saveUserIdAfterLogin(context, userId = userId)
        // Store login state
        saveLoginState(context, true)
        //TODO Subscription info
    }

    fun saveUserIdAfterLogin(context: Context, userId: String) {
        val sharedPref =
            context.getSharedPreferences(NAME_USER_INFO_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { putString(KEY_USERID_PREFS, userId) }
    }

    fun getUserIdFromSF(context: Context): String {
        val sharedPref =
            context.getSharedPreferences(NAME_USER_INFO_PREFS, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_USERID_PREFS, "Unknown") ?: "Unknown"
    }

    fun clearUserInfoPref(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences(NAME_USER_INFO_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit { clear() }
    }
}
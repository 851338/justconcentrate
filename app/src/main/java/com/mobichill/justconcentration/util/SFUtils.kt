package com.mobichill.justconcentration.util

import android.content.Context
import androidx.core.content.edit
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.IS_LOGGED_IN_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.USERID_PREFS_KEY
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.USER_INFO_PREFS_NAME
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.USER_SESSION_PREFS_NAME

object SFUtils {
    fun saveLoginState(context: Context, isLoggedIn: Boolean) {
        val sharedPref =
            context.getSharedPreferences(USER_SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putBoolean(IS_LOGGED_IN_KEY, isLoggedIn) }
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val sharedPref =
            context.getSharedPreferences(USER_SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(IS_LOGGED_IN_KEY, false)  // Default is false
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
            context.getSharedPreferences(USER_INFO_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putString(USERID_PREFS_KEY, userId) }
    }

    fun getUserIdFromSF(context: Context): String {
        val sharedPref =
            context.getSharedPreferences(USER_INFO_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(USERID_PREFS_KEY, "Unknown") ?: "Unknown"
    }

    fun clearUserInfoPref(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences(USER_INFO_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit { clear() }
    }
}
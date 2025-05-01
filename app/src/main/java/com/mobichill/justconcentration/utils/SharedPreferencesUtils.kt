package com.mobichill.justconcentration.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_ACCEPTED_POLICY
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_FOCUS_SESSION_ACTIVE
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_IS_LOGGED_IN
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTINGS_DARK_MODE
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTINGS_DEFAULT_ALARM_SOUND
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTINGS_SYNC
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTINGS_VIBRATION
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTING_DEFAULT_SESSION_SOUND
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SKIPPED_LOGIN
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_USERID_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_APP_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_SETTINGS_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_USER_INFO_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_USER_SESSION_PREFS

class SharedPreferencesUtils(context: Context) {
    private val appContext = context.applicationContext

    private val appPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(NAME_APP_PREFS, MODE_PRIVATE)
    }

    private val settingPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(NAME_SETTINGS_PREFS, MODE_PRIVATE)
    }

    private val userInfoPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(NAME_USER_INFO_PREFS, MODE_PRIVATE)
    }

    private val userSessionPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(NAME_USER_SESSION_PREFS, MODE_PRIVATE)
    }


    // User login/logout
    fun saveLoginState(isLoggedIn: Boolean) {
        userSessionPrefs.edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isUserLoggedIn(): Boolean {
        return userSessionPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveUserId(userId: String) {
        userInfoPrefs.edit { putString(KEY_USERID_PREFS, userId) }
    }

    fun getUserId(): String {
        // getString with a non-null default returns String, but adding ?: for extra safety costs little.
        return userInfoPrefs.getString(KEY_USERID_PREFS, "Unknown") ?: "Unknown"
    }

    fun clearUserInfo() {
        userInfoPrefs.edit { clear() }
    }

    fun saveUserInfoToSF(userId: String) {
        saveUserId(userId)
        saveLoginState(true)
        //TODO Subscription info
    }

    fun logout() {
        clearUserInfo() // Or specific keys if clear() is too broad
        saveLoginState(false)
    }

    // App Preferences
    fun hasAcceptedPolicy() = appPrefs.getBoolean(KEY_ACCEPTED_POLICY, false)

    fun isSkippedLogin() = appPrefs.getBoolean(KEY_SKIPPED_LOGIN, false)

    fun isFocusSessionActive() = appPrefs.getBoolean(KEY_FOCUS_SESSION_ACTIVE, false)

    fun setSkippedLogin(isSkipped: Boolean) {
        appPrefs.edit { putBoolean(KEY_SKIPPED_LOGIN, isSkipped) }
    }

    fun setAcceptedPolicy(isAccepted: Boolean) {
        appPrefs.edit { putBoolean(KEY_ACCEPTED_POLICY, isAccepted) }
    }

    fun setFocusSessionActive(isActive: Boolean) {
        appPrefs.edit { putBoolean(KEY_FOCUS_SESSION_ACTIVE, isActive) }
    }

    // Settings Shared Preferences
    fun isSettingsSyncEnabled() = settingPrefs.getBoolean(KEY_SETTINGS_SYNC, false)

    fun isVibrationEnabled() = settingPrefs.getBoolean(KEY_SETTINGS_VIBRATION, false)

    fun isDarkModeEnabled() = settingPrefs.getBoolean(KEY_SETTINGS_DARK_MODE, false)

    fun getDefaultAlarmSound() = settingPrefs.getString(KEY_SETTINGS_DEFAULT_ALARM_SOUND, "")

    fun getDefaultSessionSound() = settingPrefs.getString(KEY_SETTING_DEFAULT_SESSION_SOUND, "")

    fun updateSettingSync(isChecked: Boolean) {
        settingPrefs.edit { putBoolean(KEY_SETTINGS_SYNC, isChecked) }
    }

    fun updateSettingsVibration(isChecked: Boolean) {
        settingPrefs.edit { putBoolean(KEY_SETTINGS_VIBRATION, isChecked) }
    }

    fun updateSettingsDarkMode(isChecked: Boolean) {
        settingPrefs.edit { putBoolean(KEY_SETTINGS_DARK_MODE, isChecked) }
    }

    fun updateSettingsDefaultAlarmSound(uri: Uri) {
        settingPrefs.edit { putString(KEY_SETTINGS_DEFAULT_ALARM_SOUND, uri.toString()) }
    }

    fun updateSettingsDefaultSessionSound(uri: Uri) {
        settingPrefs.edit { putString(KEY_SETTING_DEFAULT_SESSION_SOUND, uri.toString()) }
    }

    fun removeSettingsDefaultSessionSound() {
        settingPrefs.edit { remove(KEY_SETTING_DEFAULT_SESSION_SOUND) }
    }

//
//    fun getLastActiveDate(context: Context): LocalDate? {
//        val dateStr = getAppPrefs(context).getString(KEY_LAST_ACTIVE_DATE, null)
//        return dateStr?.let {
//            try { LocalDate.parse(it, DATE_FORMATTER) } catch (e: Exception) { null }
//        }
//    }
}
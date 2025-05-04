package com.mobichill.justconcentration.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.Timestamp
import com.mobichill.justconcentration.constants.Constants.OTHERS.DATE_FORMATTER
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_ACCEPTED_POLICY
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_FOCUS_SESSION_ACTIVE
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_IS_LOGGED_IN
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_LAST_ACTIVE_DATE
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_LAST_SYNC_TIMESTAMP_NANOS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_LAST_SYNC_TIMESTAMP_SECONDS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_LOGIN_STREAK
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
import java.time.LocalDate

class SharedPreferencesUtils(context: Context) {
    private val TAG = javaClass.simpleName
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

    fun getCurrentLoginStreak() = appPrefs.getInt(KEY_LOGIN_STREAK, 0)

    fun getLastActiveDate(): LocalDate? = appPrefs.getString(KEY_LAST_ACTIVE_DATE, null)?.let {
        try {
            LocalDate.parse(it, DATE_FORMATTER)
        } catch (e: Exception) {
            Log.e(TAG, "getLastActiveDate: ", e)
            null
        }
    }

    fun setCurrentLoginStreak(streak: Int) {
        appPrefs.edit { putInt(KEY_LOGIN_STREAK, streak) }
    }

    fun setLastActiveDate(date: LocalDate) {
        appPrefs.edit { putString(KEY_LAST_ACTIVE_DATE, date.format(DATE_FORMATTER)) }

    }

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

    // Sync preferences
    fun getLastSyncTimestamp(): Timestamp {
        // Default to 0 seconds, 0 nanoseconds (start of epoch) for the very first sync
        val seconds = appPrefs.getLong(KEY_LAST_SYNC_TIMESTAMP_SECONDS, 0L)
        val nanos = appPrefs.getInt(KEY_LAST_SYNC_TIMESTAMP_NANOS, 0)

        Log.d(TAG, "Retrieved last sync timestamp: seconds=$seconds, nanos=$nanos")
        return Timestamp(seconds, nanos)
    }

    fun saveLastSyncTimestamp(timestamp: Timestamp) {
        Log.d(TAG, "Saving last sync timestamp: seconds=${timestamp.seconds}, nanos=${timestamp.nanoseconds}")
        appPrefs.edit {
            putLong(KEY_LAST_SYNC_TIMESTAMP_SECONDS, timestamp.seconds)
                .putInt(KEY_LAST_SYNC_TIMESTAMP_NANOS, timestamp.nanoseconds)
        }
    }
}
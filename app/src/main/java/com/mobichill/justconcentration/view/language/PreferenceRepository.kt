package com.mobichill.justconcentration.view.language

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mobichill.justconcentration.constants.Constants

class PreferenceRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.SHARED_PREFERENCES.KEY_NAME_PREFERENCE, Context.MODE_PRIVATE)

    fun isLanguageAlreadySet(): Boolean {
        return sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES.KEY_LANGUAGE_ALREADY_SET, false)
    }

    // New method to get both language ISO and type
    fun getAppLanguageAndType(): Pair<String, Int> {
        val lang = sharedPreferences.getString(
            Constants.SHARED_PREFERENCES.KEY_APP_LANGUAGE,
            "en"
        ) ?: "en"
        val type = sharedPreferences.getInt(
            Constants.SHARED_PREFERENCES.KEY_APP_LANGUAGE_TYPE,
            0 // Default to 0 if no specific type was saved
        )
        return Pair(lang, type)
    }

    fun getAppName(): String {
        return sharedPreferences.getString(
            Constants.SHARED_PREFERENCES.KEY_APP_NAME,
            ""
        ) ?: ""
    }

    fun setAppVersion(value: String) {
        sharedPreferences.edit { putString(Constants.SHARED_PREFERENCES.KEY_APP_VERSION, value) }
    }

    fun getAppVersion(): String {
        return sharedPreferences.getString(
            Constants.SHARED_PREFERENCES.KEY_APP_VERSION,
            ""
        ) ?: ""
    }

    // New method to set both language ISO and type
    fun setAppLanguageAndType(isoLanguage: String, type: Int) {
        sharedPreferences.edit {
            putString(Constants.SHARED_PREFERENCES.KEY_APP_LANGUAGE, isoLanguage)
            putInt(Constants.SHARED_PREFERENCES.KEY_APP_LANGUAGE_TYPE, type) // Save the type
            putBoolean(Constants.SHARED_PREFERENCES.KEY_LANGUAGE_ALREADY_SET, true)
        }
    }

    // You might want to remove or deprecate getAppLanguage() if getAppLanguageAndType() is always used
    // For now, let's keep it but note its limited use.
    fun getAppLanguage(): String {
        return sharedPreferences.getString(
            Constants.SHARED_PREFERENCES.KEY_APP_LANGUAGE,
            "en"
        ) ?: "en"
    }

    fun getAuthorization(): String {
        return sharedPreferences.getString(
            Constants.SHARED_PREFERENCES.KEY_AUTHORIZATION_MOVIE,
            ""
        ) ?: ""
    }
}
package com.mobichill.justconcentration.helper

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.APP_PREFS_NAME
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.THEME_KEY
import androidx.core.content.edit

object ThemeHelper {

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        val themeMode = prefs.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun setTheme(context: Context, isDarkMode: Boolean) {
        val prefs = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() {
            putInt(
                THEME_KEY,
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

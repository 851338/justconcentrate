package com.mobichill.justconcentration.helper

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_APP_PREFS
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_THEME
import androidx.core.content.edit

object ThemeHelper {

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(NAME_APP_PREFS, Context.MODE_PRIVATE)
        val themeMode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun setTheme(context: Context, isDarkMode: Boolean) {
        val prefs = context.getSharedPreferences(NAME_APP_PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(
                KEY_THEME,
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

package com.mobichill.justconcentration.helper

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.mobichill.justconcentration.utils.SharedPreferencesUtils

object ThemeHelper {

    fun applyTheme(context: Context) {
        val prefsUtils = SharedPreferencesUtils(context)
        val themeMode = prefsUtils.getThemeMode()
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun setTheme(context: Context, themeMode: Int) {
        val prefsUtils = SharedPreferencesUtils(context)
        prefsUtils.setThemeMode(themeMode)
        prefsUtils.setShouldSuppressSwitchListener(true)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
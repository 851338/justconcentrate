package com.mobichill.justconcentration.helper

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        return updateResources(context, language)
    }

    fun updateResources(context: Context, language: String): Context {
        // Create a Locale object with the desired language
        val locale = Locale(language)
        Locale.setDefault(locale)

        // Get the current configuration
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        // Return the context with the updated configuration
        return context.createConfigurationContext(configuration)
    }

}
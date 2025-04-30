package com.mobichill.justconcentration.constants

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

class MyContextWrapper(base: Context?) : ContextWrapper(base) {
    companion object {
        fun wrap(context: Context?, language: String): ContextWrapper {
            val config = context?.resources?.configuration
            var sysLocale = getSystemLocale(config)
            if (language.isNotEmpty() && sysLocale?.language != language) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                setSystemLocale(config, locale)
            }
            return MyContextWrapper(context?.createConfigurationContext(config!!))
        }

        fun getSystemLocale(config: Configuration?): Locale? {
            return config?.locales[0]
        }

        fun setSystemLocale(config: Configuration?, locale: Locale) {
            config?.setLocale(locale)
        }
    }
}
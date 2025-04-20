package com.mobichill.justconcentration.util

import java.util.regex.Pattern

object Constants {

    object OTHERS {
        const val ALARM_CHANNEL = "task_alarm_channel"
        const val FOCUS_CHANNEL = "focus_session_channel"
        const val ACTION_START_SESSION = "com.mobichill.justconcentration.ACTION_START_SESSION"
        const val ACTION_CANCEL_SESSION = "com.mobichill.justconcentration.ACTION_CANCEL_SESSION"
        const val ACTION_SESSION_COMPLETE = "com.mobichill.justconcentration.ACTION_SESSION_COMPLETE"
        const val POLICY_URL =
            "https://www.privacypolicies.com/live/d032f7b3-71ba-48c9-83fe-27566be9254e"
        val EMAIL_REGEX: Pattern =
            Pattern.compile("^[0-9a-z]+(?:\\.[0-9a-z]+)*@[a-z0-9]{2,}(?:\\.[a-z]{2,})?$")
    }

    object INTENT_EXTRA {
        const val TASK_KEY = "task_key"
        const val TASK_ID = "task_id"
        const val REQUEST_CODE = "request_code"
        const val SNOOZE_MINUTES = "snooze_minutes"
        const val FOCUS_SESSION = "focus_session"
        const val ALARM_URI = "alarm_uri"
        const val FOCUS_AUDIO_URI = "focus_audio_uri"
        const val FOCUS_DURATION = "focus_duration"
        const val FOCUS_USER_GOAL = "focus_user_goal"
        const val FOCUS_QUOTE = "focus_quote"
    }

    object SHARED_PREFERENCES {
        const val APP_PREFS_NAME = "app_prefs"
        const val ALARM_PREFS_NAME = "alarm_prefs"
        const val USER_SESSION_PREFS_NAME = "user_session"
        const val USER_INFO_PREFS_NAME = "user_info"
        const val FOCUS_SESSION_PREFS_NAME = "focus_session"
        const val IS_LOGGED_IN_KEY = "is_logged_in"
        const val REQUEST_CODE_PREFS_KEY = "request_code"
        const val USERID_PREFS_KEY = "userid_prefs"
        const val ACCEPTED_POLICY_KEY = "accepted_privacy_policy"
        const val SKIPPED_LOGIN_KEY = "skipped_login"
        const val FOCUS_SESSION_ACTIVE_KEY = "is_focus_active"
        const val THEME_KEY = "theme_mode"
    }

}
package com.mobichill.justconcentration.constants

import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object Constants {

    object BACKEND {
        const val BASE_URL = "https://validatepurchaseapi-viyslskjca-uc.a.run.app"
        const val VALIDATE_PURCHASE_ENDPOINT = "/api/validate-purchase"
    }

    object SUBSCRIPTION {
        const val PRO_SUBSCRIPTION_ID = "premium_monthly_v1"

        const val PRO_MONTHLY_BASE_PLAN_ID = "subscription-1month"
        const val PRO_6_MONTH_BASE_PLAN_ID = "subscription-6month"
        const val PRO_ANNUAL_BASE_PLAN_ID = "subscription-12month"
    }

    object NOTIFICATION {
        const val NOTIFICATION_ID_FOCUS_SERVICE = 1001
        const val NOTIFICATION_ID_ALARM_AUDIO_SERVICE = 1002
    }

    object PRO_BADGES{
        private const val LOYALIST_3_MONTH_GOAL_DAYS = 90
        private const val LOYALIST_6_MONTH_GOAL_DAYS = 180
        private const val LOYALIST_12_MONTH_GOAL_DAYS = 365

        val LOYALIST_BADGES_WITH_GOALS = mapOf(
            "pro_loyalist_3" to LOYALIST_3_MONTH_GOAL_DAYS,
            "pro_loyalist_6" to LOYALIST_6_MONTH_GOAL_DAYS,
            "pro_loyalist_12" to LOYALIST_12_MONTH_GOAL_DAYS
        )
        const val PRO_SUPPORTER_BADGE_ID = "pro_supporter"
    }

    object REQUEST_CODE {
        const val REQUEST_SYSTEM_RINGTONE = 10001
        const val REQUEST_LOCAL_SOUND = 10002
        const val REQUEST_APP_UPDATE = 210399

    }

    object OTHERS {
        // Worker keys
        const val KEY_SESSION_ID = "session_id"
        const val KEY_SESSION_GOAL = "session_goal"
        const val KEY_SESSION_START_TIME = "session_start_time"
        const val KEY_SESSION_CONFIG_DURATION = "session_config_duration"
        const val KEY_SESSION_DATE = "session_date"
        const val KEY_INTENT_ACTION = "intent_action"

        // Notification channels
        const val CHANNEL_ALARM = "task_alarm_channel"
        const val CHANNEL_FOCUS = "focus_session_channel"

        // Service intent actions
        const val ACTION_START_SESSION = "com.mobichill.justconcentration.ACTION_START_SESSION"
        const val ACTION_CANCEL_SESSION = "com.mobichill.justconcentration.ACTION_CANCEL_SESSION"
        const val ACTION_SESSION_COMPLETE = "com.mobichill.justconcentration.ACTION_SESSION_COMPLETE"

        // App constants
        const val TIME_FORMAT = "MMM dd, yyyy '-' hh:mm a"
        const val DB_NAME = "jc_database"
        const val POLICY_URL =
            "https://www.privacypolicies.com/live/d032f7b3-71ba-48c9-83fe-27566be9254e"
        val EMAIL_REGEX: Pattern =
            Pattern.compile("^[0-9a-z]+(?:\\.[0-9a-z]+)*@[a-z0-9]{2,}(?:\\.[a-z]{2,})?$")
        const val DATE_FORMAT = "MMM dd, yyyy"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    }

    object INTENT_EXTRA {
        const val TASK_KEY = "task_key"
        const val TASK_ID = "task_id"
        const val REQUEST_CODE = "request_code"
        const val SNOOZE_MINUTES = "snooze_minutes"
        const val ALARM_URI = "alarm_uri"
        const val FOCUS_AUDIO_URI = "focus_audio_uri"
        const val FOCUS_DURATION = "focus_duration"
        const val FOCUS_USER_GOAL = "focus_user_goal"
        const val FOCUS_START_TIME = "focus_start_time"
        const val FOCUS_QUOTE = "focus_quote"
        const val FOCUS_SESSION = "focus_session"
    }

    object SHARED_PREFERENCES {
        //SharedPreferences Name
        const val NAME_APP_PREFS = "app_prefs"
        const val NAME_SETTINGS_PREFS = "settings_prefs"
        const val NAME_ALARM_PREFS = "alarm_prefs"
        const val NAME_USER_SESSION_PREFS = "user_session"
        const val NAME_USER_INFO_PREFS = "user_info"
        const val NAME_FOCUS_SESSION_PREFS = "focus_session"

        // SharedPreferences Key
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_REQUEST_CODE_PREFS = "request_code"
        const val KEY_USERID_PREFS = "userid_prefs"
        const val KEY_ACCEPTED_POLICY = "accepted_privacy_policy"
        const val KEY_SKIPPED_LOGIN = "skipped_login"
        const val KEY_FOCUS_SESSION_ACTIVE = "is_focus_active"
        const val KEY_SETTINGS_SYNC = "sync_with_cloud"
        const val KEY_SETTINGS_VIBRATION = "is_vibration_enabled"
        const val KEY_SETTING_THEME_MODE = "settings_theme_mode"
        const val KEY_SETTINGS_DEFAULT_ALARM_SOUND = "settings_default_alarm_sound"
        const val KEY_SETTING_DEFAULT_SESSION_SOUND = "settings_default_session_sound"
        const val KEY_LAST_ACTIVE_DATE = "user_last_active_date"
        const val KEY_LOGIN_STREAK = "user_login_streak"
        const val KEY_LAST_SYNC_TIMESTAMP_SECONDS = "last_sync_timestamp_seconds"
        const val KEY_LAST_SYNC_TIMESTAMP_NANOS = "last_sync_timestamp_nanos"
    }

}
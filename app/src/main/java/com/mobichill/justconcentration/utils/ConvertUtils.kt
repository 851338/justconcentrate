package com.mobichill.justconcentration.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.constants.Constants.OTHERS.TIME_FORMAT
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object ConvertUtils {
    private val TAG = javaClass.simpleName

    fun dp(value: Int): Int = (value * Resources.getSystem().displayMetrics.density).toInt()

    fun px(value: Int): Int = (value * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    fun convertTimeMillisIntoText(context: Context, timeMillis: Long): String {
        if (timeMillis == 0L)
            return context.getString(R.string.no_date_selected)
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        return sdf.format(date)
    }

    fun convertTimeMillisIntoDateString(timeMillis: Long): String {
        return Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        // result: "Apr 22, 2025"
    }

    fun convertTimeMillisIntoLocalDate(timeMillis: Long): LocalDate =
        Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    fun convertTextIntoTimeMillis(timeString: String): Long {
        return try {
            val format = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
            val date = format.parse(timeString)
            date?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "convertTextIntoTimeMillis: ", e)
            return 0L
        }
    }
}
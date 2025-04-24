package com.mobichill.justconcentration.util

import android.content.Context
import android.content.res.Resources
import com.mobichill.justconcentration.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object ConvertUtils {
    fun dp(value: Int): Int = (value * Resources.getSystem().displayMetrics.density).toInt()

    fun px(value: Int): Int = (value * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    fun convertTimeMillisIntoText(context: Context, timeMillis: Long): String {
        if (timeMillis == 0L)
            return context.getString(R.string.no_date_selected)
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    fun convertTimeMillisIntoDate(timeMillis: Long): String {
        return Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        // result: "Apr 22, 2025"
    }
}
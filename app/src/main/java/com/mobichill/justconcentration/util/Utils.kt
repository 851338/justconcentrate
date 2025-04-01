package com.mobichill.justconcentration.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.edit
import com.mobichill.justconcentration.util.Constants.REQUEST_CODE_PICK_AUDIO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun convertTimeMillisIntoText(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    fun getNextRequestCode(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(Constants.ALARM_PREFS, Context.MODE_PRIVATE)
        val nextRequestCode =
            sharedPreferences.getInt(Constants.REQUEST_CODE_PREFS, 1000) // Default starting value

        // Save the incremented value for next use
        sharedPreferences.edit { putInt(Constants.REQUEST_CODE_PREFS, nextRequestCode + 1) }

        return nextRequestCode
    }

    fun saveLoginState(context: Context, isLoggedIn: Boolean) {
        val sharedPref =
            context.getSharedPreferences(Constants.USER_SESSION_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { putBoolean(Constants.IS_LOGGED_IN, isLoggedIn) }
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val sharedPref =
            context.getSharedPreferences(Constants.USER_SESSION_PREFS, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(Constants.IS_LOGGED_IN, false)  // Default is false
    }

    fun saveUserNameAfterLogin(context: Context, username: String) {
        val sharedPref =
            context.getSharedPreferences(Constants.USER_INFO_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { putString(Constants.USERNAME_PREFS, username) }
    }

    fun getUserNameFromSF(context: Context): String {
        val sharedPref =
            context.getSharedPreferences(Constants.USER_INFO_PREFS, Context.MODE_PRIVATE)
        return sharedPref.getString(Constants.USERNAME_PREFS, "Unknown") ?: "Unknown"
    }

    fun clearUserInfoPref(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences(Constants.USER_INFO_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit { clear() }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun getUsernameFromEmail(email: String): String {
        if (email.contains("@")) {
            return email.split("@".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0] // Get the part before '@'
        }
        return "Unknown"
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun openAudioPicker(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (SAF)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
        } else {
            // Android 10 and below (Legacy Storage)
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
        }
    }

    fun persistUriPermission(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }
}
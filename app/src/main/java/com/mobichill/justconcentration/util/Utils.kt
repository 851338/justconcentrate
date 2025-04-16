package com.mobichill.justconcentration.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.bumptech.glide.Glide
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.util.Constants.OTHERS.POLICY_URL
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.ALARM_PREFS_NAME
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.IS_LOGGED_IN_KEY
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.REQUEST_CODE_PREFS_KEY
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.USERID_PREFS_KEY
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.USER_INFO_PREFS_NAME
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.USER_SESSION_PREFS_NAME
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun convertTimeMillisIntoText(timeMillis: Long): String {
        val date = Date(timeMillis)
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    fun getAudioNameFromUri(context: Context, uri: Uri): String? {
        // Try to get DISPLAY_NAME via ContentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrEmpty()) return name
            }
        }

        // Fallback: try to extract filename from Uri
        return DocumentFile.fromSingleUri(context, uri)?.name
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Unknown_Audio_File"
    }

    fun getNextRequestCode(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        val nextRequestCode =
            sharedPreferences.getInt(REQUEST_CODE_PREFS_KEY, 1000) // Default starting value

        // Save the incremented value for next use
        sharedPreferences.edit { putInt(REQUEST_CODE_PREFS_KEY, nextRequestCode + 1) }

        return nextRequestCode
    }

    fun saveLoginState(context: Context, isLoggedIn: Boolean) {
        val sharedPref =
            context.getSharedPreferences(USER_SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putBoolean(IS_LOGGED_IN_KEY, isLoggedIn) }
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val sharedPref =
            context.getSharedPreferences(USER_SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(IS_LOGGED_IN_KEY, false)  // Default is false
    }

    fun saveUserInfoToSF(context: Context, userId: String) {
        //Save userid to sharedPreference
        saveUserIdAfterLogin(context, userId = userId)
        // Store login state
        saveLoginState(context, true)
        //TODO Subscription info
    }

    fun saveUserIdAfterLogin(context: Context, userId: String) {
        val sharedPref =
            context.getSharedPreferences(USER_INFO_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putString(USERID_PREFS_KEY, userId) }
    }

    fun getUserIdFromSF(context: Context): String {
        val sharedPref =
            context.getSharedPreferences(USER_INFO_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(USERID_PREFS_KEY, "Unknown") ?: "Unknown"
    }

    fun clearUserInfoPref(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences(USER_INFO_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit { clear() }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun persistUriPermission(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    fun showCustomPermissionDialog(
        context: Context,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Permission Needed")
        builder.setMessage("We need notification permission to send reminders for your tasks. Please grant the permission to continue.")
        builder.setPositiveButton("Grant Permission") { _, _ ->
            // After user clicks 'Grant Permission', request the permission
            checkNotificationPermission(context, requestPermissionLauncher)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun checkNotificationPermission(
        context: Context,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    fun showKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun openPrivacyPolicy(activity: Activity) {
        val intent = Intent(Intent.ACTION_VIEW, POLICY_URL.toUri())
        activity.startActivity(intent)
    }

    fun gotoStore(activity: Activity) {
        val uri =
            "https://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun openLicensePage(activity: Activity) {
        OssLicensesMenuActivity.setActivityTitle("Open Source Licenses")
        activity.startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
    }

    fun setAvatar(context: Context, avatarUrl: String?, avatarImageView: CircleImageView) {
        if (avatarUrl.isNullOrEmpty()) {
            // Load avatar from URL (Google sign-in)
            Glide.with(context)
                .load(avatarUrl)
                .placeholder(R.drawable.default_avatar)
                .circleCrop()
                .into(avatarImageView)
        } else {
            // Manual login/signup -> use default avatar
            avatarImageView.setImageResource(R.drawable.default_avatar)
        }
    }
}
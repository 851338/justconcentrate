package com.mobichill.justconcentration.util

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.databinding.CustomToastLayoutBinding
import com.mobichill.justconcentration.others.Constants.OTHERS.POLICY_URL
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.ALARM_PREFS_NAME
import com.mobichill.justconcentration.others.Constants.SHARED_PREFERENCES.REQUEST_CODE_PREFS_KEY
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar

object Utils {
    fun getNextRequestCode(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        val nextRequestCode =
            sharedPreferences.getInt(REQUEST_CODE_PREFS_KEY, 1000) // Default starting value

        // Save the incremented value for next use
        sharedPreferences.edit { putInt(REQUEST_CODE_PREFS_KEY, nextRequestCode + 1) }

        return nextRequestCode
    }

    fun showToast(context: Context, message: String) {
        val binding = CustomToastLayoutBinding.inflate(LayoutInflater.from(context))
        binding.toastText.text = message

        val toast = Toast(context)
        toast.view = binding.root
        toast.duration = Toast.LENGTH_SHORT
        toast.setGravity(Gravity.CENTER, 0, 0) // Works because it's a custom toast
        toast.show()
//        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
//        toast.setGravity(Gravity.CENTER, 0, 0)
//        toast.show()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        if (!avatarUrl.isNullOrEmpty()) {
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

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        positive: String,
        negative: String,
        onConfirmed: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positive) { _, _ -> onConfirmed() }
            .setNegativeButton(negative, null)
            .show()
    }

    fun showDateTimePicker(context: Context, onDateTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()

        // Show Date Picker
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Show Time Picker after date is picked
                val timePicker = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        onDateTimeSelected(calendar)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false // is24HourView
                )
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}
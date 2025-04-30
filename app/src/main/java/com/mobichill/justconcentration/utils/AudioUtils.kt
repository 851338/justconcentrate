package com.mobichill.justconcentration.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.constants.Constants.REQUEST_CODE.REQUEST_SYSTEM_RINGTONE
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTINGS_DEFAULT_ALARM
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.NAME_SETTINGS_PREFS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context.MODE_PRIVATE
import com.mobichill.justconcentration.constants.Constants.SHARED_PREFERENCES.KEY_SETTING_DEFAULT_SESSION_SOUND

object AudioUtils {
    // Alarm sound
    fun defaultAlarmUri(context: Context): Uri {
        val prefs = context.getSharedPreferences(NAME_SETTINGS_PREFS, MODE_PRIVATE)
        val alarmString = prefs.getString(KEY_SETTINGS_DEFAULT_ALARM, "")
        return if (alarmString.isNullOrEmpty())
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else alarmString.toUri()
    }
    fun defaultAlarmString(context: Context): String = defaultAlarmUri(context).toString()
    fun defaultAlarmName(context: Context): String =
        getAudioNameFromUri(R.string.unknown_audio_file, context, defaultAlarmUri(context))

    // Session sound
    fun defaultSessionUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(NAME_SETTINGS_PREFS, MODE_PRIVATE)
        val sessionString = prefs.getString(KEY_SETTING_DEFAULT_SESSION_SOUND, "")
        return if (sessionString.isNullOrEmpty())
            null
        else sessionString.toUri()
    }
    fun defaultSessionString(context: Context): String = defaultSessionUri(context).toString()
    fun defaultSessionName(context: Context): String {
        val defaultSessionUri = defaultSessionUri(context)
        return if (defaultSessionUri == null)
            context.getString(R.string.silence)
        else getAudioNameFromUri(R.string.unknown_audio_file, context, defaultSessionUri)
    }

    fun showSoundChoiceDialog(
        activity: Activity,
        pickAudioLauncher: ActivityResultLauncher<Intent>
    ) {
        val options = arrayOf("Choose from system sounds", "Choose from local files")

        AlertDialog.Builder(activity)
            .setTitle("Select Alarm Sound")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openSystemRingtonePicker(activity)
                    1 -> openFilePicker(pickAudioLauncher)
                }
            }
            .show()
    }

    fun openFilePicker(pickAudioLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickAudioLauncher.launch(intent)
    }

    fun openSystemRingtonePicker(activity: Activity) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
        activity.startActivityForResult(intent, REQUEST_SYSTEM_RINGTONE)
    }

    suspend fun isValidAudioFile(context: Context, uri: Uri): Boolean {
        // Check MIME type
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && mimeType.startsWith("audio/")) {
            return true
        }

        // Check file extension
        val fileExtension = uri.lastPathSegment?.substringAfterLast(".")
        val validExtensions = listOf("mp3", "wav", "ogg", "flac", "m4a")
        if (fileExtension != null && fileExtension in validExtensions) {
            return true
        }

        // Perform MediaPlayer check in the background to avoid blocking the UI thread
        return withContext(Dispatchers.IO) {
            try {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(context, uri)
                mediaPlayer.prepare()
                mediaPlayer.release() // Release when done
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun persistUriPermission(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    fun getAudioNameFromUri(stringId: Int, context: Context, uri: Uri): String {
        return try {
            // First try OpenableColumns (works for document/file pickers)
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrEmpty()) return name
                }
            }

            // If it's a ringtone URI, use RingtoneManager
            if (uri.toString().startsWith("content://media/internal") || uri.toString()
                    .startsWith("content://media/external")
            ) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone.getTitle(context)
                if (!title.isNullOrEmpty()) return title
            }

            // Fallbacks
            DocumentFile.fromSingleUri(context, uri)?.name
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: context.getString(stringId)

        } catch (e: Exception) {
            Log.e("AudioUtils", "getAudioNameFromUri", e)
            context.getString(stringId)
        }
    }


}
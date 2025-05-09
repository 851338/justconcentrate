package com.mobichill.justconcentration.manager

import android.app.Activity
import android.app.PendingIntent
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.mobichill.justconcentration.listener.AppUpdateListener

class MyUpdateManager(
    private val activity: ComponentActivity,
    private val updateType: Int = AppUpdateType.FLEXIBLE,
    private val appUpdateListener: AppUpdateListener
) : DefaultLifecycleObserver {

    private val TAG = this::class.java.simpleName
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateListener.showUpdateDownloadedSnackbar {
                appUpdateManager.completeUpdate()
            }
        } else if (state.installStatus() == InstallStatus.FAILED) {
            Log.e(TAG, "Update failed! Status: ${state.installErrorCode()}")
            //appUpdateListener.showUpdateFailedError(state.installErrorCode())
        }
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    // Called when the Activity's onCreate is called
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        appUpdateManager = AppUpdateManagerFactory.create(activity.applicationContext)

        appUpdateResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            handleUpdateResult(result.resultCode)
        }

        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedListener)
        }
    }

    fun checkForUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                Log.d(TAG, "Update available for type: $updateType.")
                startUpdateFlow(appUpdateInfo)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                Log.d(TAG, "No update available.")
            } else {
                Log.d(TAG, "Update availability: ${appUpdateInfo.updateAvailability()}")
                // Handle other states like UPDATE_UNKNOWN, DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to check for update: ${e.message}", e)
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        try {
            val pendingIntent: PendingIntent? = appUpdateInfo.resolutionIntent()
            if (pendingIntent != null) {
                val intentSender: IntentSender = pendingIntent.intentSender
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()

                this.appUpdateResultLauncher.launch(intentSenderRequest)
                Log.d(TAG, "Update flow launched using MyUpdateManager's launcher.")
            } else {
                Log.e(
                    TAG,
                    "resolutionIntent() returned null. Cannot start update flow for type $updateType. UpdateAvailability: ${appUpdateInfo.updateAvailability()}"
                )
            }
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Error starting update flow for type $updateType: ${e.message}", e)
        } catch (e: Exception) { // Catching general exceptions can help diagnose other issues
            Log.e(TAG, "Unexpected error in startUpdateFlow for type $updateType: ${e.message}", e)
        }
    }

    private fun handleUpdateResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Update flow successful (user accepted or immediate update progressing)")
            }

            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Update flow cancelled by user.")
            }

            else -> { // Includes ActivityResult.RESULT_IN_APP_UPDATE_FAILED
                Log.e(TAG, "Update flow failed with result code: $resultCode")
            }
        }
    }

    // Called when Activity's onResume is called
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (updateType == AppUpdateType.IMMEDIATE) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    Log.d(TAG, "Resuming IMMEDIATE update.")
                    startUpdateFlow(appUpdateInfo)
                }
            }
        }
    }

    // Called when Activity's onDestroy is called
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        if (updateType == AppUpdateType.FLEXIBLE) {
            try {
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            } catch (e: RuntimeException) {
                // Can happen if listener was not registered (e.g. Play Store not available)
                Log.w(TAG, "Error unregistering listener: ${e.message}")
            }
        }
        activity.lifecycle.removeObserver(this) // Clean up observer
    }
}
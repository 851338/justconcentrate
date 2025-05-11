package com.mobichill.justconcentration.manager

import android.app.Activity
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
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.mobichill.justconcentration.constants.Constants.REQUEST_CODE.REQUEST_APP_UPDATE
import com.mobichill.justconcentration.listener.AppUpdateListener

class MyUpdateManager(
    private val activity: ComponentActivity,
    private val currentUpdateType: Int = AppUpdateType.FLEXIBLE,
    private val appUpdateListener: AppUpdateListener,
    private val checkForUpdateOnStart: Boolean = false
) : DefaultLifecycleObserver {

    companion object {
        private val TAG = MyUpdateManager::class.java.simpleName
    }
    private var appUpdateManagerInstance: AppUpdateManager? = null
    private lateinit var appUpdateResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        val manager = appUpdateManagerInstance ?: return@InstallStateUpdatedListener
        if (state.installStatus() == InstallStatus.DOWNLOADED && currentUpdateType == AppUpdateType.FLEXIBLE) {
            appUpdateListener.showUpdateDownloadedSnackbar {
                manager.completeUpdate()
            }
        } else if (state.installStatus() == InstallStatus.FAILED) {
            Log.e(TAG, "Update download failed! Status: ${state.installErrorCode()}")
        }
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        appUpdateManagerInstance = AppUpdateManagerFactory.create(activity.applicationContext)

        appUpdateResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            // The result from the ActivityResultLauncher
            handleUpdateResult(result.resultCode)
        }

        if (currentUpdateType == AppUpdateType.FLEXIBLE && appUpdateManagerInstance != null) {
            appUpdateManagerInstance!!.registerListener(installStateUpdatedListener)
        }

        // Check for update right after being initialized
        if (checkForUpdateOnStart) {
            checkForUpdate()
        }
    }

    fun checkForUpdate() {
        val manager = appUpdateManagerInstance
        if (manager == null) {
            Log.e(TAG, "checkForUpdate called but appUpdateManagerInstance is null!")
            appUpdateListener.onUpdateFlowStartFailed(IllegalStateException("AppUpdateManager not initialized yet."))
            return
        }

        Log.d(TAG, "checkForUpdate: Proceeding with manager: $manager")
        val appUpdateInfoTask = manager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(currentUpdateType)
            ) {
                Log.d(TAG, "Update available for type: $currentUpdateType.")
                startUpdateFlow(appUpdateInfo, manager) // Pass the initialized manager
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                currentUpdateType == AppUpdateType.IMMEDIATE
            ) {
                Log.d(TAG, "Immediate update already in progress. Attempting to resume.")
                startUpdateFlow(appUpdateInfo, manager) // Pass the initialized manager
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                Log.d(TAG, "No update available.")
                appUpdateListener.onUpdateNotAvailable()
            } else {
                Log.w(
                    TAG,
                    "Update availability: ${appUpdateInfo.updateAvailability()}, Requested type $currentUpdateType, Type allowed: ${
                        appUpdateInfo.isUpdateTypeAllowed(currentUpdateType)
                    }"
                )
                appUpdateListener.onUpdateFlowStartFailed(Exception("Update state not ideal: avail=${appUpdateInfo.updateAvailability()}"))
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to check for update: ${e.message}", e)
            appUpdateListener.onUpdateFlowStartFailed(e)
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo, manager: AppUpdateManager) {
        Log.d(
            TAG,
            "Attempting to start update flow for type: $currentUpdateType with manager: $manager"
        )

        val starter =
            IntentSenderForResultStarter { intentSender, requestCode, fillInIntent, flagsMask, flagsValues, _, options ->
                // The requestCode here is APP_UPDATE_REQUEST_CODE
                // The intentSender is prepared by 'manager.startUpdateFlowForResult'
                Log.d(
                    TAG,
                    "IntentSenderForResultStarter invoked by AppUpdateManager. RequestCode: $requestCode"
                )
                try {
                    val request = IntentSenderRequest.Builder(intentSender)
                        .setFillInIntent(fillInIntent)
                        .setFlags(flagsValues, flagsMask)
                        .build()
                    appUpdateResultLauncher.launch(request)
                    Log.d(TAG, "Launched update flow via ActivityResultLauncher.")
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error in IntentSenderForResultStarter when trying to launch: ${e.message}",
                        e
                    )
                    appUpdateListener.onUpdateFlowStartFailed(e)
                }
            }

        val updateStartedSuccessfully = manager.startUpdateFlowForResult(
            appUpdateInfo,
            currentUpdateType,
            starter,
            REQUEST_APP_UPDATE
        )

        if (updateStartedSuccessfully) {
            Log.d(
                TAG,
                "manager.startUpdateFlowForResult call was successful. Starter should be invoked."
            )
        } else {
            Log.w(
                TAG,
                "manager.startUpdateFlowForResult call returned false. Starter likely not invoked. Conditions not met."
            )
            Log.w(
                TAG,
                "AppUpdateInfo state: availability=${appUpdateInfo.updateAvailability()}, typeAllowed=${
                    appUpdateInfo.isUpdateTypeAllowed(currentUpdateType)
                }"
            )
            appUpdateListener.onUpdateFlowStartFailed(Exception("startUpdateFlowForResult returned false, conditions likely not met."))
        }
    }

    private fun handleUpdateResult(resultCode: Int) {
        // This is called when appUpdateResultLauncher gets a result
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d(
                    TAG,
                    "Update flow successful (user accepted or immediate update progressing). Type: $currentUpdateType"
                )
                if (currentUpdateType == AppUpdateType.FLEXIBLE) {
                    Log.d(TAG, "Flexible update download initiated by user.")
                }
                appUpdateListener.onUpdateFlowResultOk()
            }

            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Update flow cancelled by user for type $currentUpdateType.")
                appUpdateListener.onUpdateFlowResultCancelled()
            }

            else -> { // Includes com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
                Log.e(
                    TAG,
                    "Update flow failed with result code: $resultCode for type $currentUpdateType"
                )
                appUpdateListener.onUpdateFlowResultFailed(resultCode)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val manager = appUpdateManagerInstance
        if (manager == null) {
            Log.w(TAG, "onResume: appUpdateManagerInstance is null.")
            return
        }

        if (currentUpdateType == AppUpdateType.IMMEDIATE) {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    Log.d(TAG, "Resuming IMMEDIATE update on app resume.")
                    startUpdateFlow(appUpdateInfo, manager) // Pass the initialized manager
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "MyUpdateManager's Observer onDestroy CALLED for $currentUpdateType")
        val manager = appUpdateManagerInstance
        if (manager != null && currentUpdateType == AppUpdateType.FLEXIBLE) {
            try {
                Log.d(TAG, "Unregistering install state listener.")
                manager.unregisterListener(installStateUpdatedListener)
            } catch (e: RuntimeException) { // Catch more specific if possible, but RuntimeException is broad
                Log.w(TAG, "Error unregistering listener (might not have been registered or Play services issue): ${e.message}")
            }
        }
        activity.lifecycle.removeObserver(this)
        appUpdateManagerInstance = null // Help GC
    }
}
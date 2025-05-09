package com.mobichill.justconcentration.listener

import androidx.activity.result.IntentSenderRequest

interface AppUpdateListener {

    fun showUpdateDownloadedSnackbar(onCompleteUpdate: () -> Unit)

    fun launchUpdateFlow(intentSenderRequest: IntentSenderRequest)
}
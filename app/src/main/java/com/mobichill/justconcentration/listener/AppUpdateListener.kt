package com.mobichill.justconcentration.listener

interface AppUpdateListener {
    fun showUpdateDownloadedSnackbar(onCompleteUpdate: () -> Unit)
    fun onUpdateFlowStartFailed(error: Exception) // Called if startUpdateFlowForResult returns false or starter fails
    fun onUpdateFlowResultOk()
    fun onUpdateFlowResultCancelled()
    fun onUpdateNotAvailable()
    fun onUpdateFlowResultFailed(resultCode: Int)
}
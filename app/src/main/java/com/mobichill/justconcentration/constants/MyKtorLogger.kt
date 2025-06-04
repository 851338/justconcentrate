package com.mobichill.justconcentration.constants

import android.util.Log
import io.ktor.client.plugins.logging.Logger

object MyKtorLogger : Logger {
    private const val TAG = "KtorLog"

    override fun log(message: String) {
        Log.d(TAG, message)
    }
}
package com.mobichill.justconcentration.manager
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

object VibrationManager {
    private var vibrator: Vibrator? = null

    fun startVibration(context: Context) {
        vibrator = context.getSystemService(Vibrator::class.java)

        val vibrationEffect = VibrationEffect.createWaveform(
            longArrayOf(0, 500, 1000, 500, 1000), // vibrate, sleep, vibrate, sleep
            0 // repeat indefinitely
        )
        vibrator?.vibrate(vibrationEffect)
    }

    fun stopVibration() {
        vibrator?.cancel()
    }
}

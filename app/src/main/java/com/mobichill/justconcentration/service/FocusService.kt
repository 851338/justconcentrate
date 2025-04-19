package com.mobichill.justconcentration.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_AUDIO_URI
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_DURATION
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_QUOTE
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.FOCUS_USER_GOAL
import com.mobichill.justconcentration.util.Constants.OTHERS.FOCUS_CHANNEL
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.FOCUS_SESSION_ACTIVE_KEY
import com.mobichill.justconcentration.util.Constants.SHARED_PREFERENCES.FOCUS_SESSION_NAME
import java.util.Locale

class FocusService : Service() {
    private lateinit var countDownTimer: CountDownTimer
    private val NOTIFICATION_ID = 1001
    private lateinit var notificationManager: NotificationManager
    private var mediaPlayer: MediaPlayer? = null
    private var tickCount = 0 // Keep track of every tick
    private var isQuote = false
    private val prefs by lazy {
        getSharedPreferences(FOCUS_SESSION_NAME, MODE_PRIVATE)
    }
    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        isRunning = true
        prefs.edit { putBoolean(FOCUS_SESSION_ACTIVE_KEY, true) }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val durationInMinutes = intent.getIntExtra(FOCUS_DURATION, 0)
        val goal = intent.getStringExtra(FOCUS_USER_GOAL) ?: "Stay focused"
        val soundUri = intent.getStringExtra(FOCUS_AUDIO_URI)
        val quote = intent.getStringExtra(FOCUS_QUOTE) ?: "You can do it!"

        val durationInMillis = durationInMinutes * 60 * 1000L

        startForeground(NOTIFICATION_ID, buildNotification(goal, durationInMinutes))
        //Play music with user's selected sound, else play silence
        if (!soundUri.isNullOrEmpty()) {
            startPlayingSound(soundUri)
        }
        startCountDownTimer(durationInMillis, goal, quote)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        stopPlayingSound()
        isRunning = false
        prefs.edit { putBoolean(FOCUS_SESSION_ACTIVE_KEY, false) }
    }

    private fun startCountDownTimer(durationInMillis: Long, goal: String, quote: String) {
        countDownTimer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted =
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

                val content = if (tickCount % 4 == 0) {
                    // If tickCount is a multiple of 4, toggle between goal and quote
                    isQuote = !isQuote
                    if (isQuote) quote else "🎯 Goal: $goal\n⏳ $timeFormatted left"
                } else {
                    // Otherwise, just show goal + remaining time (no toggle)
                    if (isQuote) quote else "🎯 Goal: $goal\n⏳ $timeFormatted left"
                }
                updateNotification(content)
            }

            override fun onFinish() {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                sendFinishedNotification()
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOCUS_CHANNEL,
                "Focus Session",
                NotificationManager.IMPORTANCE_LOW
            )

            notificationManager.createNotificationChannel(channel)
        } // no else block needed because under 8.0 there is no channel
    }

    private fun buildNotification(goal: String, durationInMinutes: Int): Notification {
        return NotificationCompat.Builder(this, FOCUS_CHANNEL)
            .setContentTitle("Concentration Mode")
            .setContentText("🎯 Goal: $goal\n⏳ Duration: $durationInMinutes mins")
            .setSmallIcon(R.drawable.ic_concentrate)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = NotificationCompat.Builder(this, FOCUS_CHANNEL)
            .setContentTitle("Concentration Mode")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_concentrate)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendFinishedNotification() {
        val notification = NotificationCompat.Builder(this, FOCUS_CHANNEL)
            .setContentTitle("Session Complete!")
            .setContentText("Good job staying focused 🎯")
            .setSmallIcon(R.drawable.ic_concentrate)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun startPlayingSound(uri: String) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(uri)
            prepareAsync()
            setOnPreparedListener {
                start()
            }
        }
    }

    private fun stopPlayingSound() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }
}

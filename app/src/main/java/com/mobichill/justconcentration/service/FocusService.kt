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
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.util.Constants.OTHERS.FOCUS_AUDIO_URI
import com.mobichill.justconcentration.util.Constants.OTHERS.FOCUS_CHANNEL
import com.mobichill.justconcentration.util.Constants.OTHERS.FOCUS_DURATION
import com.mobichill.justconcentration.util.Constants.OTHERS.FOCUS_USER_GOAL
import java.util.Locale

class FocusService : Service() {
    private lateinit var countDownTimer: CountDownTimer
    private val NOTIFICATION_ID = 1001
    private lateinit var notificationManager: NotificationManager
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val durationInMinutes = intent.getIntExtra(FOCUS_DURATION, 0)
        val goal = intent.getStringExtra(FOCUS_USER_GOAL) ?: "Stay focused"
        val soundUri = intent.getStringExtra(FOCUS_AUDIO_URI)

        val durationInMillis = durationInMinutes * 60 * 1000L

        startForeground(NOTIFICATION_ID, buildNotification(goal, durationInMinutes))
        //Play music with user's selected sound, else play silence
        if (!soundUri.isNullOrEmpty()) {
            startPlayingSound(soundUri)
        }
        countDownTimer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted =
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                updateNotification(goal, timeFormatted)
            }

            override fun onFinish() {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                sendFinishedNotification()
            }

        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        stopPlayingSound()
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
            .setContentText("Goal: $goal | Duration: $durationInMinutes min")
            .setSmallIcon(R.drawable.ic_concentrate)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(goal: String, remaining: String) {
        val notification = NotificationCompat.Builder(this, FOCUS_CHANNEL)
            .setContentTitle("Concentration Mode")
            .setContentText("Goal: $goal | Time left: $remaining")
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

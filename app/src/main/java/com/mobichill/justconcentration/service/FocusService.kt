package com.mobichill.justconcentration.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_AUDIO_URI
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_DURATION
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_QUOTE
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_SESSION
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.FOCUS_USER_GOAL
import com.mobichill.justconcentration.constants.Constants.NOTIFICATION.NOTIFICATION_ID_FOCUS_SERVICE
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_CANCEL_SESSION
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_SESSION_COMPLETE
import com.mobichill.justconcentration.constants.Constants.OTHERS.ACTION_START_SESSION
import com.mobichill.justconcentration.constants.Constants.OTHERS.CHANNEL_FOCUS
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.receiver.NotificationActionReceiver
import com.mobichill.justconcentration.utils.ConvertUtils
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import java.util.Locale

class FocusService : Service() {
    private val TAG = this::class.java.simpleName
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var notificationManager: NotificationManager
    private var mediaPlayer: MediaPlayer? = null
    private var tickCount = 0 // Keep track of every tick
    private var isQuote = false
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(applicationContext)
    }

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        isRunning = true
        sfUtils.setFocusSessionActive(true)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val durationInMinutes = intent.getIntExtra(FOCUS_DURATION, 0)
        val goal = intent.getStringExtra(FOCUS_USER_GOAL) ?: "Stay focused"
        val soundUri = intent.getStringExtra(FOCUS_AUDIO_URI)
        val quote = intent.getStringExtra(FOCUS_QUOTE) ?: "You can do it!"
        val durationInMillis = durationInMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        val tempSession = ConcentrateSessionModel(
            goal = goal,
            startTime = now,
            endTime = now + durationInMillis,
            date = ConvertUtils.convertTimeMillisIntoDateString(now),
            durationMinutes = durationInMinutes,
            wasCompleted = false
        )

        // Create the cancel pending intent
        val cancelIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_CANCEL_SESSION
            putExtra(FOCUS_SESSION, tempSession)
        }
        val cancelPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when (intent.action) {
            ACTION_START_SESSION -> {
                //Start service
                startForeground(
                    NOTIFICATION_ID_FOCUS_SERVICE,
                    buildNotification(goal, durationInMinutes, cancelPendingIntent)
                )
                if (!soundUri.isNullOrEmpty()) {
                    startPlayingSound(soundUri)
                }
                startCountDownTimer(
                    durationInMillis,
                    goal,
                    quote,
                    cancelPendingIntent,
                    tempSession
                )
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopPlayingSound()
        notificationManager.cancel(NOTIFICATION_ID_FOCUS_SERVICE)
        countDownTimer.cancel()
        isRunning = false
        sfUtils.setFocusSessionActive(false)
    }

    private fun startCountDownTimer(
        durationInMillis: Long,
        goal: String,
        quote: String,
        cancelPendingIntent: PendingIntent,
        tempSession: ConcentrateSessionModel
    ) {
        val finishIntent =
            Intent(applicationContext, NotificationActionReceiver::class.java).apply {
                action = ACTION_SESSION_COMPLETE
                putExtra(FOCUS_SESSION, tempSession)
            }
        countDownTimer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted =
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                tickCount++
                val content = if (tickCount % 4 == 0) {
                    // If tickCount is a multiple of 4, toggle between goal and quote
                    isQuote = !isQuote
                    if (isQuote) quote else "🎯 Goal: $goal\n⏳ $timeFormatted left"
                } else {
                    // Otherwise, just show goal + remaining time (no toggle)
                    if (isQuote) quote else "🎯 Goal: $goal\n⏳ $timeFormatted left"
                }
                updateNotification(content, cancelPendingIntent)
            }

            override fun onFinish() {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                sendFinishedNotification()
                sendBroadcast(finishIntent)
            }
        }.start()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_FOCUS,
            "Focus Session",
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        goal: String,
        durationInMinutes: Int,
        cancelPendingIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_FOCUS)
            .setContentTitle("Concentration Mode")
            .setContentText("🎯 Goal: $goal\n⏳ Duration: $durationInMinutes minutes")
            .setSmallIcon(R.drawable.ic_concentrate)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPendingIntent)
            .build()
    }

    private fun updateNotification(content: String, cancelPendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(this, CHANNEL_FOCUS)
            .setContentTitle("Concentration Mode")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_concentrate)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_FOCUS_SERVICE, notification)
    }

    private fun sendFinishedNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_FOCUS)
            .setContentTitle("Session Complete!")
            .setContentText("Good job staying focused 🎯")
            .setSmallIcon(R.drawable.ic_concentrate)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_FOCUS_SERVICE + 1, notification)
    }

    private fun startPlayingSound(uri: String) {
        try {
            val afd = contentResolver.openAssetFileDescriptor(uri.toUri(), "r")
            afd?.use {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(it.fileDescriptor)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startPlayingSound: ", e)
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

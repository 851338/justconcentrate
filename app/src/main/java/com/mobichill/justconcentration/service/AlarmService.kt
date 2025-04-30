package com.mobichill.justconcentration.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.ALARM_URI
import com.mobichill.justconcentration.constants.Constants.NOTIFICATION.NOTIFICATION_ID_ALARM_AUDIO_SERVICE
import com.mobichill.justconcentration.utils.AudioUtils

class AlarmService : Service() {
    private val TAG = this::class.java.simpleName
    private lateinit var mediaPlayer: MediaPlayer
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmUri = intent?.getStringExtra(ALARM_URI)
        val soundUri: Uri =
            if (alarmUri.isNullOrEmpty()) AudioUtils.defaultAlarmUri(this) else alarmUri.toUri()

        // Create and start foreground notification
        startForeground(NOTIFICATION_ID_ALARM_AUDIO_SERVICE, createNotification())

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, soundUri)
                setAudioAttributes(audioAttributes)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Alarm play media: ", e)
        }


        // Stop the alarm after 1 minute (Optional)
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 60000)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "Alarm Service",
            NotificationManager.IMPORTANCE_MIN
        )
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm Playing")
            .setContentText("Your alarm is currently ringing")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
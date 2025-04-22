package com.mobichill.justconcentration.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.net.toUri
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.ALARM_URI

class AlarmService: Service() {
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmUri = intent?.getStringExtra(ALARM_URI)

        val soundUri: Uri = if (alarmUri.isNullOrEmpty()) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Default sound
        } else {
            alarmUri.toUri() // Selected alarm sound
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@AlarmService, soundUri)
            setAudioAttributes(audioAttributes)
            isLooping = true
            prepare()
            start()
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
}
package com.fajralarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build

class FajrAlarmApp : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "fajr_alarm_channel"
        const val ALARM_CHANNEL_NAME = "Fajr & Mission Alarms"
    }

    override fun onCreate() {
        super.onCreate()
        createAlarmNotificationChannel()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority mission alarm notification channel"
                setSound(null, null) // Audio is handled explicitly by AlarmService MediaPlayer
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

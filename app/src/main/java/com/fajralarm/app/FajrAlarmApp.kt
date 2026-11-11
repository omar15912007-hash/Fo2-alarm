package com.fajralarm.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.fajralarm.app.receiver.WakeUpCheckReceiver
import com.fajralarm.app.service.AlarmService

class FajrAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createAlarmNotificationChannels()
    }

    private fun createAlarmNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val alarmChannel = NotificationChannel(
                AlarmService.CHANNEL_ID,
                "Critical Alarm & Wake-Up Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority full-screen alarms with wake-up missions"
                setSound(null, null) // Audio is handled explicitly by AlarmService MediaPlayer
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val wakeUpCheckChannel = NotificationChannel(
                WakeUpCheckReceiver.CHANNEL_ID,
                "Wake-Up Check Verification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Follow-up verification check to prevent falling back asleep"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(alarmChannel)
            manager.createNotificationChannel(wakeUpCheckChannel)
        }
    }
}

package com.fajralarm.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fajralarm.app.R
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.ringing.AlarmRingingActivity
import kotlinx.coroutines.*

class WakeUpCheckReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WAKE_UP_CHECK = "com.fajralarm.app.ACTION_WAKE_UP_CHECK"
        const val ACTION_CONFIRM_AWAKE = "com.fajralarm.app.ACTION_CONFIRM_AWAKE"
        const val CHANNEL_ID = "wake_up_check_channel"
        const val NOTIF_ID = 8881

        private var timeoutJob: Job? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("EXTRA_ALARM_ID") ?: ""

        if (intent.action == ACTION_CONFIRM_AWAKE) {
            timeoutJob?.cancel()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
            return
        }

        createChannel(context)

        val confirmIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
            action = ACTION_CONFIRM_AWAKE
            putExtra("EXTRA_ALARM_ID", alarmId)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context,
            1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("👀 Wake-Up Check!")
            .setContentText("Are you still awake? Tap to confirm within 100 seconds or alarm will restart!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(R.mipmap.ic_launcher, "I'm Awake!", confirmPending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)

        // 100-Second Timeout countdown. If not confirmed, restart full alarm!
        timeoutJob?.cancel()
        timeoutJob = CoroutineScope(Dispatchers.Default).launch {
            delay(100 * 1000L) // 100 seconds
            val repository = AlarmRepository(context)
            val alarm = repository.getAlarmById(alarmId)

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("EXTRA_ALARM_ID", alarmId)
                putExtra("EXTRA_SOUND_RES", alarm?.soundResName ?: "siren")
                putExtra("EXTRA_LABEL", "Wake-Up Check Failed!")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                putExtra("EXTRA_ALARM_ID", alarmId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(ringIntent)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wake-Up Check Verification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Follow-up verification check to prevent falling back asleep"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}

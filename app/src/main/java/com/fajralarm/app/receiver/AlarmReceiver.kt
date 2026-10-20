package com.fajralarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.ringing.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            // Reschedule all active alarms after phone reboot
            val repository = AlarmRepository(context)
            repository.getAllAlarms().filter { it.isEnabled }.forEach { alarm ->
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
            return
        }

        val alarmId = intent.getStringExtra("EXTRA_ALARM_ID") ?: return
        val repository = AlarmRepository(context)
        val alarm = repository.getAlarmById(alarmId) ?: return

        // 1. Acquire PowerManager.WakeLock to keep CPU awake while service and UI initialize
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AlarmApp:AlarmWakeLock"
            )
            wakeLock.acquire(60_000L) // Safe 60-second wake lock
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error acquiring wake lock", e)
        }

        // 2. Start Foreground Audio & Vibration Service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarm.id)
            putExtra("EXTRA_SOUND_RES", alarm.soundResName)
            putExtra("EXTRA_LABEL", alarm.label)
            putExtra("EXTRA_GENTLE_MINUTES", alarm.gentleWakeUpMinutes)
            putExtra("EXTRA_VIBRATE", alarm.isVibrate)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 3. Launch Full-screen Mission Ringing Activity
        val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarm.id)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        context.startActivity(ringIntent)
    }
}

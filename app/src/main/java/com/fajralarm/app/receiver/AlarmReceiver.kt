package com.fajralarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.ringing.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            // Reschedule all active alarms after phone reboot, update, or time change
            val repository = AlarmRepository(context)
            repository.getAllAlarms().filter { it.isEnabled }.forEach { alarm ->
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
            return
        }

        val alarmId = intent.getStringExtra("EXTRA_ALARM_ID") ?: return
        val repository = AlarmRepository(context)
        val alarm = repository.getAlarmById(alarmId) ?: return
        if (!alarm.isEnabled) {
            Log.d(TAG, "Ignoring trigger for disabled alarm: $alarmId")
            return
        }

        // 1. Acquire PowerManager.WakeLock (PARTIAL_WAKE_LOCK only; ACQUIRE_CAUSES_WAKEUP is invalid with PARTIAL_WAKE_LOCK)
        try {
            val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Fo2App:AlarmReceiverWakeLock"
            )
            wakeLock.acquire(60_000L) // Safe 60-second wake lock
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }

        // 2. Start Foreground Audio & Vibration Service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarm.id)
            putExtra("EXTRA_SOUND_RES", alarm.soundResName)
            putExtra("EXTRA_LABEL", alarm.label)
            putExtra("EXTRA_GENTLE_MINUTES", alarm.gentleWakeUpMinutes)
            putExtra("EXTRA_VIBRATE", alarm.isVibrate)
            putExtra("EXTRA_VOLUME", alarm.volume)
            putExtra("EXTRA_LOUD_BOOST", alarm.isLoudBoostEnabled)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // 3. Launch Full-screen Mission Ringing Activity
        try {
            val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                putExtra("EXTRA_ALARM_ID", alarm.id)
                putExtra("EXTRA_IS_WAKE_UP_CHECK", false)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            context.startActivity(ringIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct activity launch deferred to fullScreenIntent", e)
        }
    }
}

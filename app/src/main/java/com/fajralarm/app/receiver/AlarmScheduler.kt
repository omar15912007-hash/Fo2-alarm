package com.fajralarm.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fajralarm.app.MainActivity
import com.fajralarm.app.data.AlarmModel
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Pure deterministic calculation of the next alarm trigger timestamp in epoch milliseconds.
     */
    fun computeNextTriggerMillis(
        hour24: Int,
        minute: Int,
        daysOfWeek: Collection<Int>,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }

            if (daysOfWeek.isNotEmpty()) {
                var guard = 0
                while (!daysOfWeek.contains(get(Calendar.DAY_OF_WEEK)) && guard < 8) {
                    add(Calendar.DAY_OF_YEAR, 1)
                    guard++
                }
            }
        }
        return calendar.timeInMillis
    }

    fun scheduleAlarm(context: Context, alarm: AlarmModel) {
        if (!alarm.isEnabled) return

        val triggerAtMillis = computeNextTriggerMillis(
            hour24 = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = alarm.daysOfWeek
        )
        scheduleTriggerAt(context, alarm.id, triggerAtMillis)
    }

    /**
     * Schedules an exact snooze wake-up at [triggerAtMillis] without mutating the alarm's saved hour/minute or repeat days.
     */
    fun scheduleSnooze(context: Context, alarmId: String, triggerAtMillis: Long) {
        if (alarmId.isBlank()) return
        scheduleTriggerAt(context, alarmId, triggerAtMillis)
    }

    private fun scheduleTriggerAt(context: Context, alarmId: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canScheduleExact) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                Log.w(TAG, "Exact alarm permission missing; using idle-safe fallback.")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm id=$alarmId for $triggerAtMillis")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm; falling back", se)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled with id: $alarmId")
    }
}

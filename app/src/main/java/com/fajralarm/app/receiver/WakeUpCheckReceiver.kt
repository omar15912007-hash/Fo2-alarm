package com.fajralarm.app.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fajralarm.app.R
import com.fajralarm.app.data.AlarmModel
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.ringing.AlarmRingingActivity

/**
 * Handles Follow-Up Wake-Up Checks:
 * If enabled, fires X minutes after mission completion, shows a Heads-Up notification
 * with an "I'm Awake" action, and only restarts the alarm siren if 100 seconds expire
 * without user confirmation.
 */
class WakeUpCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WakeUpCheckReceiver"
        const val ACTION_WAKE_UP_CHECK = "com.fajralarm.app.ACTION_WAKE_UP_CHECK"
        const val ACTION_CONFIRM_AWAKE = "com.fajralarm.app.ACTION_CONFIRM_AWAKE"
        const val ACTION_CANCEL_WAKE_UP_CHECK = "com.fajralarm.app.ACTION_CANCEL_WAKE_UP_CHECK"
        const val ACTION_TIMEOUT_EXPIRED = "com.fajralarm.app.ACTION_TIMEOUT_EXPIRED"

        const val CHANNEL_ID = "wake_up_check_channel_v2"
        const val NOTIF_ID = 8881
        private const val TIMEOUT_SECONDS = 100

        private var wakeLock: PowerManager.WakeLock? = null

        /**
         * Schedules the initial Wake-Up Check in X minutes via AlarmManager.
         */
        fun scheduleWakeUpCheck(context: Context, alarm: AlarmModel) {
            if (!alarm.isWakeUpCheckEnabled || alarm.wakeUpCheckMinutes <= 0) return

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val checkIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_WAKE_UP_CHECK
                putExtra("EXTRA_ALARM_ID", alarm.id)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                getCheckRequestCode(alarm.id),
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + (alarm.wakeUpCheckMinutes * 60 * 1000L)
            scheduleExactOrFallback(am, triggerTime, pending)
            Log.d(TAG, "Scheduled wake-up check for alarm ${alarm.id} in ${alarm.wakeUpCheckMinutes} min")
        }

        private fun scheduleExactOrFallback(am: AlarmManager, triggerAtMillis: Long, pending: PendingIntent) {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            try {
                if (canExact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "Exact alarm restricted; falling back to setAndAllowWhileIdle", se)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        }

        /**
         * Cancels all pending wake-up checks and timeouts for an alarm ID.
         */
        fun cancelWakeUpCheck(context: Context, alarmId: String) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 1. Cancel initial check
            val checkIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_WAKE_UP_CHECK
            }
            val checkPending = PendingIntent.getBroadcast(
                context,
                getCheckRequestCode(alarmId),
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(checkPending)

            // 2. Cancel 100s timeout check
            val timeoutIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_TIMEOUT_EXPIRED
            }
            val timeoutPending = PendingIntent.getBroadcast(
                context,
                getTimeoutRequestCode(alarmId),
                timeoutIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(timeoutPending)

            // 3. Cancel confirm intent
            val confirmIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_CONFIRM_AWAKE
            }
            val confirmPending = PendingIntent.getBroadcast(
                context,
                getConfirmRequestCode(alarmId),
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(confirmPending)

            // 4. Dismiss notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)

            // 5. Release CPU wake lock if held
            releaseWakeLock()

            Log.d(TAG, "Cancelled all wake-up checks for alarm $alarmId")
        }

        private fun getCheckRequestCode(alarmId: String): Int = (alarmId + "_check").hashCode()
        private fun getConfirmRequestCode(alarmId: String): Int = (alarmId + "_confirm").hashCode()
        private fun getTimeoutRequestCode(alarmId: String): Int = (alarmId + "_timeout").hashCode()

        private fun acquireWakeLock(context: Context) {
            try {
                if (wakeLock == null) {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "AlarmApp:WakeUpCheckLock"
                    )
                }
                if (wakeLock?.isHeld != true) {
                    wakeLock?.acquire(110_000L) // 110s max safety timeout
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring wake lock", e)
            }
        }

        private fun releaseWakeLock() {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
                wakeLock = null
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wake lock", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getStringExtra("EXTRA_ALARM_ID") ?: ""
        Log.d(TAG, "onReceive action=$action, alarmId=$alarmId")

        when (action) {
            ACTION_CANCEL_WAKE_UP_CHECK, ACTION_CONFIRM_AWAKE -> {
                cancelWakeUpCheck(context, alarmId)
            }

            ACTION_WAKE_UP_CHECK -> {
                acquireWakeLock(context)
                createChannel(context)

                // 1. Prepare "I'm Awake" Action Intent
                val confirmIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
                    this.action = ACTION_CONFIRM_AWAKE
                    putExtra("EXTRA_ALARM_ID", alarmId)
                }
                val confirmPending = PendingIntent.getBroadcast(
                    context,
                    getConfirmRequestCode(alarmId),
                    confirmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. High-Priority Heads-Up Notification with Action Button
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_alarm)
                    .setContentTitle("👀 Wake-Up Check!")
                    .setContentText("Are you still awake? Tap 'I'm Awake' within 100 seconds or the alarm siren will restart!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(confirmPending)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_stat_alarm, "I'm Awake", confirmPending)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, notification)

                // 3. Schedule 100-Second Timeout via AlarmManager (Tracked & Cancellable)
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val timeoutIntent = Intent(context, WakeUpCheckReceiver::class.java).apply {
                    this.action = ACTION_TIMEOUT_EXPIRED
                    putExtra("EXTRA_ALARM_ID", alarmId)
                }
                val timeoutPending = PendingIntent.getBroadcast(
                    context,
                    getTimeoutRequestCode(alarmId),
                    timeoutIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerAtMillis = System.currentTimeMillis() + (TIMEOUT_SECONDS * 1000L)
                scheduleExactOrFallback(am, triggerAtMillis, timeoutPending)
            }

            ACTION_TIMEOUT_EXPIRED -> {
                // User did NOT confirm within 100 seconds! Sound the siren and launch ringing screen.
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIF_ID)
                releaseWakeLock()

                val repository = AlarmRepository(context)
                val alarm = repository.getAlarmById(alarmId)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("EXTRA_ALARM_ID", alarmId)
                    putExtra("EXTRA_SOUND_RES", alarm?.soundResName ?: "siren")
                    putExtra("EXTRA_LABEL", if (alarm?.label?.isNotBlank() == true) alarm.label + " (Check Failed)" else "Wake-Up Check Failed!")
                    putExtra("EXTRA_VIBRATE", alarm?.isVibrate ?: true)
                    putExtra("EXTRA_VOLUME", alarm?.volume ?: 1.0f)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                try {
                    val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                        putExtra("EXTRA_ALARM_ID", alarmId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(ringIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Direct activity launch deferred to fullScreenIntent", e)
                }
            }
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

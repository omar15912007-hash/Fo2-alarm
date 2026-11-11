package com.fajralarm.app.receiver

import android.app.AlarmManager
import android.app.Notification
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
import com.fajralarm.app.MainActivity
import com.fajralarm.app.R
import com.fajralarm.app.data.AlarmModel
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.ringing.AlarmRingingActivity

/**
 * Handles Follow-Up Wake-Up Checks:
 * Fires X minutes after mission completion, launches the on-screen Wake-Up Check UI
 * along with a full-screen Heads-Up notification, and only restarts the alarm siren
 * if 100 seconds expire without user confirmation.
 */
class WakeUpCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WakeUpCheckReceiver"
        const val ACTION_WAKE_UP_CHECK = "com.fajralarm.app.ACTION_WAKE_UP_CHECK"
        const val ACTION_CONFIRM_AWAKE = "com.fajralarm.app.ACTION_CONFIRM_AWAKE"
        const val ACTION_CANCEL_WAKE_UP_CHECK = "com.fajralarm.app.ACTION_CANCEL_WAKE_UP_CHECK"
        const val ACTION_TIMEOUT_EXPIRED = "com.fajralarm.app.ACTION_TIMEOUT_EXPIRED"

        const val CHANNEL_ID = "wake_up_check_channel_v3"
        const val NOTIF_ID = 8881
        const val TIMEOUT_SECONDS = 100

        private var wakeLock: PowerManager.WakeLock? = null

        @Volatile
        var onWakeUpCheckConfirmed: ((String) -> Unit)? = null

        /**
         * Schedules the initial Wake-Up Check in X minutes via AlarmManager.
         */
        fun scheduleWakeUpCheck(context: Context, alarm: AlarmModel) {
            if (!alarm.isWakeUpCheckEnabled || alarm.wakeUpCheckMinutes <= 0) return

            // Cancel any previous pending wake-up check for this alarm first
            cancelWakeUpCheck(context, alarm.id)

            val appContext = context.applicationContext
            val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val checkIntent = Intent(appContext, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_WAKE_UP_CHECK
                putExtra("EXTRA_ALARM_ID", alarm.id)
            }
            val pending = PendingIntent.getBroadcast(
                appContext,
                getCheckRequestCode(alarm.id),
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + (alarm.wakeUpCheckMinutes * 60_000L)
            scheduleExactAlarmClockOrFallback(appContext, am, triggerTime, pending, alarm.id)
            Log.d(TAG, "Scheduled wake-up check for alarm ${alarm.id} in ${alarm.wakeUpCheckMinutes} min")
        }

        private fun scheduleExactAlarmClockOrFallback(
            context: Context,
            am: AlarmManager,
            triggerAtMillis: Long,
            pending: PendingIntent,
            alarmId: String
        ) {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            try {
                if (canExact) {
                    val showIntent = Intent(context, MainActivity::class.java)
                    val showPending = PendingIntent.getActivity(
                        context,
                        getCheckRequestCode(alarmId),
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPending)
                    am.setAlarmClock(clockInfo, pending)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "Exact alarm restricted; falling back to setAndAllowWhileIdle", se)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        }

        private fun scheduleExactTimeoutOrFallback(
            am: AlarmManager,
            triggerAtMillis: Long,
            pending: PendingIntent
        ) {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            try {
                if (canExact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "Exact timeout restricted; falling back to setAndAllowWhileIdle", se)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        }

        /**
         * Cancels all pending wake-up checks and timeouts for an alarm ID.
         */
        fun cancelWakeUpCheck(context: Context, alarmId: String) {
            val appContext = context.applicationContext
            val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 1. Cancel initial check
            val checkIntent = Intent(appContext, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_WAKE_UP_CHECK
            }
            val checkPending = PendingIntent.getBroadcast(
                appContext,
                getCheckRequestCode(alarmId),
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(checkPending)
            checkPending.cancel()

            // 2. Cancel 100s timeout check
            val timeoutIntent = Intent(appContext, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_TIMEOUT_EXPIRED
            }
            val timeoutPending = PendingIntent.getBroadcast(
                appContext,
                getTimeoutRequestCode(alarmId),
                timeoutIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(timeoutPending)
            timeoutPending.cancel()

            // 3. Cancel confirm intent
            val confirmIntent = Intent(appContext, WakeUpCheckReceiver::class.java).apply {
                action = ACTION_CONFIRM_AWAKE
            }
            val confirmPending = PendingIntent.getBroadcast(
                appContext,
                getConfirmRequestCode(alarmId),
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(confirmPending)

            // 4. Dismiss notification
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)

            // 5. Release CPU wake lock if held
            releaseWakeLock()

            // 6. Notify active wake-up check screen if open
            onWakeUpCheckConfirmed?.invoke(alarmId)

            Log.d(TAG, "Cancelled all wake-up checks for alarm $alarmId")
        }

        private fun getCheckRequestCode(alarmId: String): Int = (alarmId + "_check").hashCode()
        private fun getConfirmRequestCode(alarmId: String): Int = (alarmId + "_confirm").hashCode()
        private fun getTimeoutRequestCode(alarmId: String): Int = (alarmId + "_timeout").hashCode()
        private fun getFullScreenRequestCode(alarmId: String): Int = (alarmId + "_fullscreen").hashCode()

        private fun acquireWakeLock(context: Context) {
            try {
                if (wakeLock == null) {
                    val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "Fo2App:WakeUpCheckLock"
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
                val repository = AlarmRepository(context)
                val alarm = repository.getAlarmById(alarmId)
                if (alarm != null && !alarm.isWakeUpCheckEnabled) {
                    cancelWakeUpCheck(context, alarmId)
                    return
                }

                acquireWakeLock(context)
                createChannel(context)

                // 1. Prepare Full-Screen Wake-Up Check Activity Intent
                val wakeUpActivityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    putExtra("EXTRA_ALARM_ID", alarmId)
                    putExtra("EXTRA_IS_WAKE_UP_CHECK", true)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                }
                val fullScreenPending = PendingIntent.getActivity(
                    context,
                    getFullScreenRequestCode(alarmId),
                    wakeUpActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. Prepare "I'm Awake" Action Intent
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

                // 3. High-Priority Full-Screen & Heads-Up Notification with Action Button
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_alarm)
                    .setContentTitle("👀 Wake-Up Check! (${alarm?.label ?: "Fo2"})")
                    .setContentText("Are you still awake? Tap 'I'm Awake' within 100 seconds or the alarm will ring!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(fullScreenPending, true)
                    .setContentIntent(fullScreenPending)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_stat_alarm, "I'm Awake", confirmPending)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, notification)

                // 4. Launch the Wake-Up Check screen directly on top of the display
                try {
                    context.startActivity(wakeUpActivityIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Direct Wake-Up Check activity launch deferred to fullScreenIntent", e)
                }

                // 5. Schedule 100-Second Timeout via AlarmManager (Tracked & Cancellable)
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
                scheduleExactTimeoutOrFallback(am, triggerAtMillis, timeoutPending)
            }

            ACTION_TIMEOUT_EXPIRED -> {
                // User did NOT confirm within 100 seconds! Cancel check notification and sound the siren.
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIF_ID)
                releaseWakeLock()

                val repository = AlarmRepository(context)
                val alarm = repository.getAlarmById(alarmId)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("EXTRA_ALARM_ID", alarmId)
                    putExtra("EXTRA_SOUND_RES", alarm?.soundResName ?: "siren")
                    putExtra(
                        "EXTRA_LABEL",
                        if (alarm?.label?.isNotBlank() == true) "${alarm.label} (Check Failed)" else "Wake-Up Check Failed!"
                    )
                    putExtra("EXTRA_VIBRATE", alarm?.isVibrate ?: true)
                    putExtra("EXTRA_VOLUME", alarm?.volume ?: 1.0f)
                    putExtra("EXTRA_LOUD_BOOST", alarm?.isLoudBoostEnabled ?: true)
                    putExtra("EXTRA_IS_WAKE_UP_RETRIGGER", true)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                try {
                    val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                        putExtra("EXTRA_ALARM_ID", alarmId)
                        putExtra("EXTRA_IS_WAKE_UP_CHECK", false)
                        putExtra("EXTRA_IS_WAKE_UP_RETRIGGER", true)
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
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wake-Up Check Verification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Follow-up verification check to prevent falling back asleep"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}

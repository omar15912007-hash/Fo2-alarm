package com.fajralarm.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import com.fajralarm.app.R
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.ui.ringing.AlarmRingingActivity
import kotlinx.coroutines.*

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isMuted: Boolean = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var volumeLockJob: Job? = null
    private var gentleWakeJob: Job? = null
    private var antiDismissWatchdogJob: Job? = null
    private var serviceWakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: String = ""
    private var currentSoundResName: String = "siren"
    private var currentLabel: String = "Wake Up"

    companion object {
        const val CHANNEL_ID = "alarm_urgent_channel_v2"
        const val NOTIFICATION_ID = 9991
        const val ACTION_PAUSE_AUDIO = "ACTION_PAUSE_AUDIO"
        const val ACTION_RESUME_AUDIO = "ACTION_RESUME_AUDIO"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"

        var isAlarmCurrentlyRinging = false
            private set

        var isActivityInForeground = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_AUDIO -> {
                pauseAudio()
                return START_STICKY
            }
            ACTION_RESUME_AUDIO -> {
                resumeAudio()
                return START_STICKY
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getStringExtra("EXTRA_ALARM_ID") ?: ""
        val soundResName = intent?.getStringExtra("EXTRA_SOUND_RES") ?: "siren"
        val label = intent?.getStringExtra("EXTRA_LABEL") ?: "Wake Up"
        val gentleMinutes = intent?.getIntExtra("EXTRA_GENTLE_MINUTES", 0) ?: 0
        val isVibrate = intent?.getBooleanExtra("EXTRA_VIBRATE", true) ?: true

        currentAlarmId = alarmId
        currentSoundResName = soundResName
        currentLabel = label
        isAlarmCurrentlyRinging = true

        // Acquire CPU WakeLock for foreground ringing duration
        if (serviceWakeLock == null) {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                serviceWakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Fo2App:AlarmServiceLock"
                )
                serviceWakeLock?.acquire(600_000L) // 10 min max timeout
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(label, alarmId))
        startAudioPlayback(soundResName, gentleMinutes)

        if (isVibrate) {
            startVibration()
        }

        // Start Volume Lock routine (forces STREAM_ALARM to max every 500ms)
        val repository = AlarmRepository(this)
        val globalSettings = repository.getGlobalSettings()
        if (globalSettings.isVolumeLockEnabled) {
            startVolumeLock()
        }

        // Start Relentless 400ms Anti-Dismiss Watchdog
        if (globalSettings.isPreventTurnOffEnabled) {
            startAntiDismissWatchdog(alarmId)
        }

        return START_STICKY
    }

    private fun startAntiDismissWatchdog(alarmId: String) {
        antiDismissWatchdogJob?.cancel()
        antiDismissWatchdogJob = serviceScope.launch {
            while (isActive && isAlarmCurrentlyRinging) {
                delay(400)
                if (!isActivityInForeground && isAlarmCurrentlyRinging) {
                    val ringIntent = Intent(applicationContext, AlarmRingingActivity::class.java).apply {
                        putExtra("EXTRA_ALARM_ID", alarmId)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }
                    startActivity(ringIntent)
                }
            }
        }
    }

    private fun startAudioPlayback(soundResName: String, gentleMinutes: Int) {
        try {
            mediaPlayer?.release()
            val resId = resources.getIdentifier(soundResName, "raw", packageName)
            val audioRes = if (resId != 0) resId else R.raw.siren

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val afd = resources.openRawResourceFd(audioRes)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                prepare()
            }

            if (gentleMinutes > 0) {
                // Gentle Wake-Up: Gradually ramp up volume from 0.0 to 1.0
                mediaPlayer?.setVolume(0.05f, 0.05f)
                mediaPlayer?.start()
                startGentleFadeIn(gentleMinutes)
            } else {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startGentleFadeIn(durationMinutes: Int) {
        gentleWakeJob?.cancel()
        gentleWakeJob = serviceScope.launch {
            val totalSteps = 20
            val stepDelayMs = (durationMinutes * 60 * 1000L) / totalSteps
            for (step in 1..totalSteps) {
                delay(stepDelayMs)
                val vol = step.toFloat() / totalSteps.toFloat()
                if (!isMuted) {
                    mediaPlayer?.setVolume(vol, vol)
                }
            }
        }
    }

    private fun startVolumeLock() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        volumeLockJob?.cancel()
        volumeLockJob = serviceScope.launch {
            while (isActive && isAlarmCurrentlyRinging) {
                try {
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                    if (current < maxVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                    }
                } catch (e: Exception) {
                    // Ignore security restrictions if any
                }
                delay(500)
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 800, 400, 800, 400, 1200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    fun pauseAudio() {
        isMuted = true
        mediaPlayer?.setVolume(0.0f, 0.0f)
    }

    fun resumeAudio() {
        isMuted = false
        mediaPlayer?.setVolume(1.0f, 1.0f)
    }

    private fun stopAlarm() {
        isAlarmCurrentlyRinging = false
        volumeLockJob?.cancel()
        gentleWakeJob?.cancel()
        antiDismissWatchdogJob?.cancel()

        // 1. Stop audio playback and release MediaPlayer resources
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Release CPU WakeLock
        try {
            if (serviceWakeLock?.isHeld == true) {
                serviceWakeLock?.release()
            }
            serviceWakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Stop foreground and stop self
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isAlarmCurrentlyRinging) {
            val restartIntent = Intent(applicationContext, AlarmService::class.java).apply {
                putExtra("EXTRA_ALARM_ID", currentAlarmId)
                putExtra("EXTRA_SOUND_RES", currentSoundResName)
                putExtra("EXTRA_LABEL", currentLabel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(label: String, alarmId: String): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (label.isNotBlank() && !label.equals("Alarm", ignoreCase = true)) "⏰ Fo2! $label" else "⏰ Fo2 Men El Noom!")
            .setContentText("Fo2 delwa2ty w 7el el mission 3ashan te2fel el alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Critical Alarm & Wake-Up Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority full-screen alarms with wake-up missions"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

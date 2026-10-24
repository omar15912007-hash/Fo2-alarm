package com.fajralarm.app.ui.ringing

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.data.AlarmHistoryRecord
import com.fajralarm.app.data.AlarmModel
import com.fajralarm.app.data.AlarmRepository
import com.fajralarm.app.data.MissionType
import com.fajralarm.app.receiver.AlarmScheduler
import com.fajralarm.app.receiver.WakeUpCheckReceiver
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.components.TwinBellAlarmClockLogo
import com.fajralarm.app.ui.ringing.missions.*
import com.fajralarm.app.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

fun canSnoozeAlarm(alarm: AlarmModel, isPreview: Boolean): Boolean {
    if (isPreview || !alarm.isSnoozeEnabled) return false
    return alarm.maxSnoozeCount >= 999 || alarm.currentSnoozeCount < alarm.maxSnoozeCount
}

class AlarmRingingActivity : ComponentActivity() {

    private lateinit var repository: AlarmRepository
    private val gson = Gson()
    private val activeAlarmState = mutableStateOf<AlarmModel?>(null)
    private val isPreviewModeState = mutableStateOf(false)
    private var startTimeMillis: Long = 0L

    private fun resolveAlarmFromIntent(intent: Intent?) {
        val isPreview = intent?.getBooleanExtra("EXTRA_IS_PREVIEW", false) ?: false
        val alarmJson = intent?.getStringExtra("EXTRA_ALARM_JSON")
        val alarmId = intent?.getStringExtra("EXTRA_ALARM_ID") ?: ""

        val resolved = if (!alarmJson.isNullOrBlank()) {
            runCatching { gson.fromJson(alarmJson, AlarmModel::class.java) }.getOrNull()
        } else {
            null
        } ?: repository.getAlarmById(alarmId) ?: AlarmModel(label = "Wake Up!")

        isPreviewModeState.value = isPreview
        activeAlarmState.value = resolved
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::repository.isInitialized) {
            resolveAlarmFromIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTimeMillis = System.currentTimeMillis()

        // 1. Full-Screen Lock Screen & Wake-up Flags
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // 2. Anti-Dismissal: Block Back Navigation completely
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent dismiss via back button while ringing
            }
        })

        repository = AlarmRepository(this)
        resolveAlarmFromIntent(intent)

        val globalSettings = repository.getGlobalSettings()
        val isUnlimitedMutes = globalSettings.maxQuickMutes == -1

        setContent {
            FajrAlarmTheme {
                val targetAlarm = activeAlarmState.value ?: AlarmModel(label = "Wake Up!")
                val isPreview = isPreviewModeState.value
                var isMissionActive by remember(targetAlarm.id, targetAlarm.missionType) {
                    mutableStateOf(targetAlarm.missionType == MissionType.NONE)
                }
                var isMuted by remember(targetAlarm.id) { mutableStateOf(false) }
                var remainingMuteUses by remember(targetAlarm.id) {
                    mutableIntStateOf(if (isUnlimitedMutes) 9999 else globalSettings.maxQuickMutes)
                }
                var missionTimeoutRemaining by remember(targetAlarm.id) {
                    mutableIntStateOf(globalSettings.missionTimeLimitSeconds)
                }

                // Inactivity countdown: if muted, countdown runs
                LaunchedEffect(isMissionActive, isMuted) {
                    if (isMissionActive && isMuted) {
                        missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                        while (missionTimeoutRemaining > 0) {
                            delay(1000)
                            missionTimeoutRemaining--
                        }
                        // Timeout reached! Unmute siren at full volume
                        resumeSiren()
                        isMuted = false
                    }
                }

                Scaffold(containerColor = PitchBlack) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!isMissionActive && targetAlarm.missionType != MissionType.NONE) {
                            // Ringing Alarm Landing Screen
                            RingingAlarmLandingScreen(
                                alarm = targetAlarm,
                                canSnooze = canSnoozeAlarm(targetAlarm, isPreview),
                                onStartMission = {
                                    isMissionActive = true
                                    if (targetAlarm.muteDuringMission && globalSettings.isMuteAudioDuringMissionEnabled) {
                                        pauseSiren()
                                        isMuted = true
                                    }
                                },
                                onSnooze = {
                                    handleSnooze(targetAlarm)
                                }
                            )
                        } else {
                            // Active Mission Solving View
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                // Top Glowing Gradient Countdown Timer Bar
                                if (isMuted) {
                                    val progress by animateFloatAsState(
                                        targetValue = missionTimeoutRemaining.toFloat() / globalSettings.missionTimeLimitSeconds.toFloat(),
                                        label = "muteTimer"
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(GlassCardStart)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                                                    contentDescription = null,
                                                    tint = CyanGlow,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Mute active: ${missionTimeoutRemaining}s",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyanGlow
                                                )
                                            }
                                            Text(
                                                text = "Siren will resume if idle",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        GlowingGradientProgressBar(
                                            progress = progress,
                                            height = 6.dp
                                        )
                                    }
                                }

                                // Interactive Mission Component
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    when (targetAlarm.missionType) {
                                        MissionType.MATH -> {
                                            MathMissionView(
                                                difficulty = targetAlarm.mathDifficulty,
                                                totalQuestions = targetAlarm.mathQuestionsCount,
                                                onSuccess = { onMissionCompleted(targetAlarm) },
                                                onUserInteraction = {
                                                    // Reset inactivity timer
                                                    missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                                                }
                                            )
                                        }
                                        MissionType.SHAKE -> {
                                            ShakeMissionView(
                                                targetShakes = targetAlarm.shakeTargetCount,
                                                sensitivityLevel = targetAlarm.shakeSensitivity,
                                                onSuccess = { onMissionCompleted(targetAlarm) },
                                                onUserInteraction = {
                                                    missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                                                }
                                            )
                                        }
                                        MissionType.MEMORY_MATRIX -> {
                                            MemoryMatrixMissionView(
                                                gridDimension = targetAlarm.memoryGridSize,
                                                onSuccess = { onMissionCompleted(targetAlarm) },
                                                onUserInteraction = {
                                                    missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                                                }
                                            )
                                        }
                                        MissionType.TYPING -> {
                                            TypingMissionView(
                                                targetQuote = targetAlarm.typingQuote,
                                                onSuccess = { onMissionCompleted(targetAlarm) },
                                                onUserInteraction = {
                                                    missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                                                }
                                            )
                                        }
                                        MissionType.STEPS -> {
                                            WalkStepsMissionView(
                                                targetCount = targetAlarm.stepTargetCount,
                                                onSuccess = { onMissionCompleted(targetAlarm) },
                                                onUserInteraction = {
                                                    missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                                                }
                                            )
                                        }
                                        MissionType.SQUAT -> {
                                            SquatExerciseMissionView(
                                                targetCount = targetAlarm.squatTargetCount,
                                                onSuccess = { onMissionCompleted(targetAlarm) },
                                                onUserInteraction = {
                                                    missionTimeoutRemaining = globalSettings.missionTimeLimitSeconds
                                                }
                                            )
                                        }
                                        MissionType.NONE -> {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Button(
                                                    onClick = { onMissionCompleted(targetAlarm) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier.padding(32.dp)
                                                ) {
                                                    Text("Dismiss Alarm", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom Temporary Mute Controls (limited uses or unlimited)
                                Surface(
                                    color = DarkSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .height(56.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isUnlimitedMutes) "Mutes left: ∞" else "Mutes left: $remainingMuteUses",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextMuted
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (isMuted && missionTimeoutRemaining > 0) {
                                                CircularCountdownMuteRing(
                                                    progress = (missionTimeoutRemaining.toFloat() / 10f).coerceIn(0f, 1f),
                                                    secondsRemaining = missionTimeoutRemaining,
                                                    enabled = false,
                                                    onClick = {}
                                                )
                                            }

                                            val muteEnabled = (isUnlimitedMutes || remainingMuteUses > 0) && !isMuted
                                            val muteInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = {
                                                    if (isUnlimitedMutes) {
                                                        pauseSiren()
                                                        isMuted = true
                                                        missionTimeoutRemaining = 10
                                                    } else if (remainingMuteUses > 0) {
                                                        remainingMuteUses--
                                                        pauseSiren()
                                                        isMuted = true
                                                        missionTimeoutRemaining = 10
                                                    }
                                                },
                                                enabled = muteEnabled,
                                                interactionSource = muteInteraction,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = CyanGlow
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f)),
                                                modifier = Modifier.bouncyPress(muteInteraction)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isMuted) "Muted" else "Quick Mute (10s)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmService.isActivityInForeground = true
    }

    override fun onPause() {
        super.onPause()
        AlarmService.isActivityInForeground = false
    }

    private fun pauseSiren() {
        if (!AlarmService.isAlarmCurrentlyRinging) return
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_PAUSE_AUDIO
        }
        startService(intent)
    }

    private fun resumeSiren() {
        if (!AlarmService.isAlarmCurrentlyRinging) return
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_RESUME_AUDIO
        }
        startService(intent)
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        runCatching { startService(stopIntent) }
    }

    private fun handleSnooze(alarm: AlarmModel) {
        if (!canSnoozeAlarm(alarm, isPreviewModeState.value)) return

        stopAlarmService()
        WakeUpCheckReceiver.cancelWakeUpCheck(this, alarm.id)

        // Persist updated snooze count WITHOUT mutating the alarm's saved hour/minute or repeat days
        val updatedSnoozeAlarm = alarm.copy(currentSnoozeCount = alarm.currentSnoozeCount + 1)
        repository.saveOrUpdateAlarm(updatedSnoozeAlarm)

        val triggerAtMillis = System.currentTimeMillis() + (alarm.snoozeIntervalMinutes * 60_000L)
        AlarmScheduler.scheduleSnooze(this, alarm.id, triggerAtMillis)

        // Record Snooze in History
        val timeTakenSec = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
        repository.addHistoryRecord(
            AlarmHistoryRecord(
                alarmId = alarm.id,
                alarmLabel = alarm.label,
                missionType = alarm.missionType,
                timeToSolveSeconds = timeTakenSec,
                wasSnoozed = true,
                isSuccess = false
            )
        )

        finish()
    }

    private fun onMissionCompleted(alarm: AlarmModel) {
        // 1. Stop audio service and teardown hardware resources
        stopAlarmService()

        // Preview/Test mode: never mutate saved alarms, history, or wake-up checks
        if (isPreviewModeState.value) {
            finish()
            return
        }

        // 2. Permanently cancel any active snooze schedulers for this alarm
        AlarmScheduler.cancelAlarm(this, alarm.id)

        // 3. Reset snooze count and either auto-disable one-shot alarm or reschedule repeating alarm
        if (alarm.daysOfWeek.isEmpty()) {
            val updatedAlarm = alarm.copy(isEnabled = false, currentSnoozeCount = 0)
            repository.saveOrUpdateAlarm(updatedAlarm)
        } else {
            val resetAlarm = alarm.copy(currentSnoozeCount = 0)
            repository.saveOrUpdateAlarm(resetAlarm)
            AlarmScheduler.scheduleAlarm(this, resetAlarm)
        }

        // 4. Record success in History
        val timeTakenSec = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
        repository.addHistoryRecord(
            AlarmHistoryRecord(
                alarmId = alarm.id,
                alarmLabel = alarm.label,
                missionType = alarm.missionType,
                timeToSolveSeconds = timeTakenSec,
                wasSnoozed = false,
                isSuccess = true
            )
        )

        // 5. Tracked Wake-Up Check or explicit cancellation
        if (alarm.isWakeUpCheckEnabled) {
            WakeUpCheckReceiver.scheduleWakeUpCheck(this, alarm)
        } else {
            WakeUpCheckReceiver.cancelWakeUpCheck(this, alarm.id)
        }

        finish()
    }
}

@Composable
fun RingingAlarmLandingScreen(
    alarm: AlarmModel,
    canSnooze: Boolean,
    onStartMission: () -> Unit,
    onSnooze: () -> Unit
) {
    var nowDate by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowDate = Date()
        }
    }
    val currentTime = remember(nowDate) {
        SimpleDateFormat("hh:mm", Locale.getDefault()).format(nowDate)
    }
    val amPm = remember(nowDate) {
        SimpleDateFormat("a", Locale.getDefault()).format(nowDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Time & Alarm Label
        Column(
            modifier = Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (alarm.label.isNotBlank() && !alarm.label.equals("Alarm", ignoreCase = true)) "FO2! • " + alarm.label.uppercase() else "FO2 MEN EL NOOM!",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = VividCyan,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currentTime,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = amPm,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // Center Vibrating Twin-Bell Canvas Logo with Pulsing Neon Aura
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingNeonAura(baseSize = 190.dp)
            TwinBellAlarmClockLogo(
                size = 140.dp,
                isRinging = true
            )
        }

        // Bottom Action Buttons with Spring Bouncy Micro-Interactions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Big Electric Blue Button: Start Mission
            val startInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onStartMission,
                interactionSource = startInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bouncyPress(startInteraction)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = TextWhite, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Fo2 w 7el el Mission (${alarm.missionType.displayName})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            // Snooze Button (if enabled & within snooze limit)
            if (canSnooze) {
                val snoozeInteraction = remember { MutableInteractionSource() }
                val snoozeLabel = if (alarm.maxSnoozeCount < 999) {
                    val remaining = (alarm.maxSnoozeCount - alarm.currentSnoozeCount).coerceAtLeast(1)
                    "Snooze (${alarm.snoozeIntervalMinutes} min • $remaining left)"
                } else {
                    "Snooze (${alarm.snoozeIntervalMinutes} min)"
                }
                OutlinedButton(
                    onClick = onSnooze,
                    interactionSource = snoozeInteraction,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bouncyPress(snoozeInteraction)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null, tint = TextMuted)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = snoozeLabel,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

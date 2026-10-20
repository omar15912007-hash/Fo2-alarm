package com.fajralarm.app.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.data.AlarmModel
import com.fajralarm.app.data.MissionType
import com.fajralarm.app.service.AlarmService
import com.fajralarm.app.ui.ringing.AlarmRingingActivity
import com.fajralarm.app.ui.theme.*
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    initialAlarm: AlarmModel? = null,
    onDismiss: () -> Unit,
    onSave: (AlarmModel) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Decoupled 12-hour Time breakdown
    val initial24Hr = initialAlarm?.hour ?: 7
    var hour12 by remember {
        val h = initial24Hr % 12
        mutableIntStateOf(if (h == 0) 12 else h)
    }
    var isAm by remember { mutableStateOf(initial24Hr < 12) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }

    var label by remember { mutableStateOf(initialAlarm?.label ?: "Wake Up") }
    var selectedMission by remember { mutableStateOf(initialAlarm?.missionType ?: MissionType.MATH) }
    var soundName by remember { mutableStateOf(initialAlarm?.soundResName ?: "siren") }
    var volume by remember { mutableFloatStateOf(initialAlarm?.volume ?: 1.0f) }
    var isLoudBoost by remember { mutableStateOf(initialAlarm?.isLoudBoostEnabled ?: true) }
    var gentleWakeMinutes by remember { mutableIntStateOf(initialAlarm?.gentleWakeUpMinutes ?: 0) }
    var isVibrate by remember { mutableStateOf(initialAlarm?.isVibrate ?: true) }

    // Mission Details
    var mathDifficulty by remember { mutableIntStateOf(initialAlarm?.mathDifficulty ?: 2) }
    var mathCount by remember { mutableIntStateOf(initialAlarm?.mathQuestionsCount ?: 3) }
    var targetShakes by remember { mutableIntStateOf(initialAlarm?.shakeTargetCount ?: 30) }
    var shakeSensitivity by remember { mutableIntStateOf(initialAlarm?.shakeSensitivity ?: 2) }
    var memoryGridSize by remember { mutableIntStateOf(initialAlarm?.memoryGridSize ?: 3) }
    var typingQuote by remember { mutableStateOf(initialAlarm?.typingQuote ?: "I am awake, energized, and ready for today!") }
    var stepCount by remember { mutableIntStateOf(initialAlarm?.stepTargetCount ?: 20) }
    var squatCount by remember { mutableIntStateOf(initialAlarm?.squatTargetCount ?: 15) }

    // Repeat Days (Default: emptyList() with no days pre-selected)
    var selectedDays by remember { mutableStateOf(initialAlarm?.daysOfWeek ?: emptyList()) }

    // Snooze
    var isSnoozeEnabled by remember { mutableStateOf(initialAlarm?.isSnoozeEnabled ?: true) }
    var snoozeInterval by remember { mutableIntStateOf(initialAlarm?.snoozeIntervalMinutes ?: 5) }
    var maxSnoozeCount by remember { mutableIntStateOf(initialAlarm?.maxSnoozeCount ?: 3) }

    // Wake-Up Check (Default OFF)
    var isWakeUpCheckEnabled by remember { mutableStateOf(initialAlarm?.isWakeUpCheckEnabled ?: false) }
    var wakeUpCheckMinutes by remember { mutableIntStateOf(initialAlarm?.wakeUpCheckMinutes ?: 15) }

    // Mute While Solving Mission
    var muteDuringMission by remember { mutableStateOf(initialAlarm?.muteDuringMission ?: true) }

    // Dialog sheets state
    var showSoundSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Direct sound preview state
    var soundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSoundPreviewing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            soundPlayer = null
        }
    }

    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    // Dynamic 24-hr calculation
    val current24Hour = when {
        isAm && hour12 == 12 -> 0
        isAm -> hour12
        !isAm && hour12 == 12 -> 12
        else -> hour12 + 12
    }
    val remainingTimeText = formatAlarmRemainingTime(current24Hour, minute, selectedDays)

    fun buildCurrentAlarm(): AlarmModel {
        return (initialAlarm ?: AlarmModel(hour = current24Hour, minute = minute)).copy(
            hour = current24Hour,
            minute = minute,
            label = label,
            daysOfWeek = selectedDays,
            missionType = selectedMission,
            mathDifficulty = mathDifficulty,
            mathQuestionsCount = mathCount,
            shakeTargetCount = targetShakes,
            shakeSensitivity = shakeSensitivity,
            memoryGridSize = memoryGridSize,
            typingQuote = typingQuote,
            stepTargetCount = stepCount,
            squatTargetCount = squatCount,
            soundResName = soundName,
            volume = volume,
            isLoudBoostEnabled = isLoudBoost,
            gentleWakeUpMinutes = gentleWakeMinutes,
            isVibrate = isVibrate,
            isSnoozeEnabled = isSnoozeEnabled,
            snoozeIntervalMinutes = snoozeInterval,
            maxSnoozeCount = maxSnoozeCount,
            isWakeUpCheckEnabled = isWakeUpCheckEnabled,
            wakeUpCheckMinutes = wakeUpCheckMinutes,
            muteDuringMission = muteDuringMission
        )
    }

    Scaffold(
        topBar = {
            // BUG FIX #1: Strict Status Bar Padding & 64.dp App Bar Alignment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchBlack)
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (initialAlarm == null) "Set Alarm" else "Edit Alarm",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Button(
                    onClick = { onSave(buildCurrentAlarm()) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text("Save", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        bottomBar = {
            // BUG FIX #1: Dedicated Pinned Bottom Container with Navigation Bar Insets
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val preview = buildCurrentAlarm()
                            val sIntent = Intent(context, AlarmService::class.java).apply {
                                putExtra("EXTRA_ALARM_ID", preview.id)
                                putExtra("EXTRA_SOUND_RES", preview.soundResName)
                                putExtra("EXTRA_LABEL", preview.label)
                            }
                            context.startService(sIntent)

                            val rIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                                putExtra("EXTRA_ALARM_ID", preview.id)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(rIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VividCyan)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onSave(buildCurrentAlarm()) },
                        modifier = Modifier
                            .weight(1.4f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Save Alarm", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = PitchBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BUG FIX #2: Ultra-Smooth 3D Infinite Wheel Picker (Completely Decoupled)
            WheelTimePicker(
                hour12 = hour12,
                minute = minute,
                isAm = isAm,
                onHourChanged = { newHour -> hour12 = newHour },
                onMinuteChanged = { newMinute -> minute = newMinute },
                onAmPmChanged = { newAm -> isAm = newAm }
            )

            // Dynamic Remaining Time Subtitle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = VividCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = remainingTimeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VividCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Repeat Days Selector
            Text(
                text = "Repeat Days",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { index, labelText ->
                    val dayNum = index + 1
                    val isSelected = selectedDays.contains(dayNum)

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Brush.horizontalGradient(listOf(ElectricBlue, CyanGlow))
                                else GlassCardGradient
                            )
                            .border(
                                1.dp,
                                if (isSelected) CyanGlow.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f),
                                CircleShape
                            )
                            .bouncyClickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDays = if (isSelected) {
                                    selectedDays.filter { it != dayNum }
                                } else {
                                    selectedDays + dayNum
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextWhite else TextMuted
                        )
                    }
                }
            }

            // Quick Presets: Weekdays, Weekends, Everyday
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Everyday" to listOf(1, 2, 3, 4, 5, 6, 7),
                    "Weekdays" to listOf(2, 3, 4, 5, 6),
                    "Weekends" to listOf(1, 7)
                ).forEach { (presetName, presetDays) ->
                    val isCurrent = selectedDays.sorted() == presetDays.sorted()
                    FilterChip(
                        selected = isCurrent,
                        onClick = { selectedDays = presetDays },
                        label = { Text(presetName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.3f),
                            selectedLabelColor = VividCyan,
                            containerColor = DarkSurface,
                            labelColor = TextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Mission Selector Grid
            Text(
                text = "Wake-Up Mission",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VividCyan,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val missionOptions = listOf(
                        Triple(MissionType.MATH, "Math Problems", Icons.Default.Calculate),
                        Triple(MissionType.SHAKE, "Shake Phone", Icons.Default.Vibration),
                        Triple(MissionType.MEMORY_MATRIX, "Memory Matrix", Icons.Default.Psychology),
                        Triple(MissionType.TYPING, "Typing Quote", Icons.Default.Keyboard),
                        Triple(MissionType.STEPS, "Walk Steps Mission", Icons.AutoMirrored.Filled.DirectionsWalk),
                        Triple(MissionType.SQUAT, "Squat Exercise Mission", Icons.Default.FitnessCenter),
                        Triple(MissionType.NONE, "Normal Dismiss", Icons.Default.NotificationsActive)
                    )

                    missionOptions.forEach { (type, title, iconVector) ->
                        val isSelected = selectedMission == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMission = type }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (isSelected) VividCyan else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) VividCyan else TextWhite
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMission = type },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = VividCyan,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 10.dp))

                    when (selectedMission) {
                        MissionType.MATH -> {
                            Text("Math Difficulty:", fontSize = 12.sp, color = TextMuted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1 to "V.Easy", 2 to "Easy", 3 to "Normal", 4 to "Hard", 5 to "V.Hard").forEach { (lvl, name) ->
                                    FilterChip(
                                        selected = mathDifficulty == lvl,
                                        onClick = { mathDifficulty = lvl },
                                        label = { Text(name, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricBlue,
                                            selectedLabelColor = TextWhite,
                                            containerColor = CardBackground,
                                            labelColor = TextMuted
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Number of Problems: $mathCount", fontSize = 12.sp, color = TextMuted)
                            Slider(
                                value = mathCount.toFloat(),
                                onValueChange = { mathCount = it.toInt() },
                                valueRange = 1f..20f,
                                steps = 18,
                                colors = SliderDefaults.colors(thumbColor = VividCyan, activeTrackColor = ElectricBlue)
                            )
                        }

                        MissionType.SHAKE -> {
                            Text("Shake Sensitivity:", fontSize = 12.sp, color = TextMuted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1 to "Low (Firm)", 2 to "Medium", 3 to "High (Sensitive)").forEach { (lvl, name) ->
                                    FilterChip(
                                        selected = shakeSensitivity == lvl,
                                        onClick = { shakeSensitivity = lvl },
                                        label = { Text(name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricBlue,
                                            selectedLabelColor = TextWhite,
                                            containerColor = CardBackground,
                                            labelColor = TextMuted
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Target Shakes: $targetShakes", fontSize = 12.sp, color = TextMuted)
                            Slider(
                                value = targetShakes.toFloat(),
                                onValueChange = { targetShakes = it.toInt() },
                                valueRange = 5f..100f,
                                steps = 18,
                                colors = SliderDefaults.colors(thumbColor = VividCyan, activeTrackColor = ElectricBlue)
                            )
                        }

                        MissionType.MEMORY_MATRIX -> {
                            Text("Grid Size:", fontSize = 12.sp, color = TextMuted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(3 to "3x3 (Easy)", 4 to "4x4 (Normal)", 5 to "5x5 (Master)").forEach { (dim, name) ->
                                    FilterChip(
                                        selected = memoryGridSize == dim,
                                        onClick = { memoryGridSize = dim },
                                        label = { Text(name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricBlue,
                                            selectedLabelColor = TextWhite,
                                            containerColor = CardBackground,
                                            labelColor = TextMuted
                                        )
                                    )
                                }
                            }
                        }

                        MissionType.TYPING -> {
                            Text("Affirmation Quote:", fontSize = 12.sp, color = TextMuted)
                            OutlinedTextField(
                                value = typingQuote,
                                onValueChange = { typingQuote = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = CardBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        MissionType.STEPS -> {
                            Text("Target Steps (Cadence Validated): $stepCount", fontSize = 12.sp, color = TextMuted)
                            Slider(
                                value = stepCount.toFloat(),
                                onValueChange = { stepCount = it.toInt() },
                                valueRange = 5f..100f,
                                steps = 18,
                                colors = SliderDefaults.colors(thumbColor = VividCyan, activeTrackColor = ElectricBlue)
                            )
                        }

                        MissionType.SQUAT -> {
                            Text("Target Squats (800ms Cooldown): $squatCount", fontSize = 12.sp, color = TextMuted)
                            Slider(
                                value = squatCount.toFloat(),
                                onValueChange = { squatCount = it.toInt() },
                                valueRange = 5f..50f,
                                steps = 44,
                                colors = SliderDefaults.colors(thumbColor = VividCyan, activeTrackColor = ElectricBlue)
                            )
                        }

                        MissionType.NONE -> {
                            Text("Alarm stops with a single dismiss tap.", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Sound & Volume Controls
            Text(
                text = "Sound & Volume",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VividCyan,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showSoundSheet = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = VividCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Ringtone", fontSize = 14.sp, color = TextWhite)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Play / Stop test button
                            IconButton(
                                onClick = {
                                    if (isSoundPreviewing) {
                                        soundPlayer?.let {
                                            if (it.isPlaying) it.stop()
                                            it.release()
                                        }
                                        soundPlayer = null
                                        isSoundPreviewing = false
                                    } else {
                                        val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
                                        if (resId != 0) {
                                            val player = MediaPlayer.create(context, resId)
                                            player?.setVolume(volume, volume)
                                            player?.isLooping = true
                                            player?.start()
                                            soundPlayer = player
                                            isSoundPreviewing = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSoundPreviewing) VividCyan else CardBackground)
                            ) {
                                Icon(
                                    imageVector = if (isSoundPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Test Sound",
                                    tint = if (isSoundPreviewing) PitchBlack else VividCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { showSoundSheet = true }
                            ) {
                                val displaySound = soundName.replace("_", " ").replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                }
                                Text(
                                    text = displaySound,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VividCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 10.dp))

                    Text("Alarm Volume: ${(volume * 100).toInt()}%", fontSize = 12.sp, color = TextMuted)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            if (isSoundPreviewing) {
                                soundPlayer?.setVolume(it, it)
                            }
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = VividCyan, activeTrackColor = ElectricBlue)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Loud Effect / Boost", fontSize = 13.sp, color = TextWhite)
                        Switch(
                            checked = isLoudBoost,
                            onCheckedChange = { isLoudBoost = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CardBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gentle Wake-Up (Gradual Fade-In):", fontSize = 12.sp, color = TextMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Off", 1 to "1 min", 2 to "2 min", 5 to "5 min").forEach { (mins, name) ->
                            FilterChip(
                                selected = gentleWakeMinutes == mins,
                                onClick = { gentleWakeMinutes = mins },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = TextWhite,
                                    containerColor = CardBackground,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = CardBackground, modifier = Modifier.padding(vertical = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = VividCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibration", fontSize = 13.sp, color = TextWhite)
                        }
                        Switch(
                            checked = isVibrate,
                            onCheckedChange = { isVibrate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CardBackground
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Snooze Settings Card
            Text(
                text = "Snooze Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VividCyan,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Snooze", fontSize = 14.sp, color = TextWhite)
                        Switch(
                            checked = isSnoozeEnabled,
                            onCheckedChange = { isSnoozeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CardBackground
                            )
                        )
                    }

                    if (isSnoozeEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Snooze Interval:", fontSize = 12.sp, color = TextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 3, 5, 10, 15).forEach { interval ->
                                FilterChip(
                                    selected = snoozeInterval == interval,
                                    onClick = { snoozeInterval = interval },
                                    label = { Text("${interval}m", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = TextWhite,
                                        containerColor = CardBackground,
                                        labelColor = TextMuted
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Max Snooze Count:", fontSize = 12.sp, color = TextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "1x", 2 to "2x", 3 to "3x", 5 to "5x", 999 to "Unlimited").forEach { (cnt, title) ->
                                FilterChip(
                                    selected = maxSnoozeCount == cnt,
                                    onClick = { maxSnoozeCount = cnt },
                                    label = { Text(title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = TextWhite,
                                        containerColor = CardBackground,
                                        labelColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Wake Up Check Feature
            Text(
                text = "Wake Up Check Feature",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VividCyan,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Follow-Up Wake-Up Check", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            Text(
                                "Checks if you fell back asleep after X minutes; requires confirmation within 100s",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isWakeUpCheckEnabled,
                            onCheckedChange = { isWakeUpCheckEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = ElectricBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CardBackground
                            )
                        )
                    }

                    if (isWakeUpCheckEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Quick Intervals:", fontSize = 12.sp, color = TextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 3, 5, 10, 15, 20, 30).forEach { mins ->
                                FilterChip(
                                    selected = wakeUpCheckMinutes == mins,
                                    onClick = { wakeUpCheckMinutes = mins },
                                    label = { Text("${mins}m", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = TextWhite,
                                        containerColor = CardBackground,
                                        labelColor = TextMuted
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fine Stepper (1 - 30 min):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (wakeUpCheckMinutes > 1) wakeUpCheckMinutes-- },
                                    enabled = wakeUpCheckMinutes > 1,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CardBackground, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (wakeUpCheckMinutes > 1) VividCyan else TextMuted
                                    )
                                }
                                Surface(
                                    color = PrimaryBlue.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$wakeUpCheckMinutes min",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VividCyan,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { if (wakeUpCheckMinutes < 30) wakeUpCheckMinutes++ },
                                    enabled = wakeUpCheckMinutes < 30,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CardBackground, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = if (wakeUpCheckMinutes < 30) VividCyan else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Alarm Label (Stabilized singleLine, zero invasive haptics)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                singleLine = true,
                maxLines = 1,
                label = { Text("Alarm Label (e.g. Work, Workout)", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = CardBackground
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Delete Button (if editing existing alarm)
            if (initialAlarm != null && onDelete != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Alarm", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        if (showSoundSheet) {
            SoundPickerSheet(
                currentSound = soundName,
                onSoundSelected = {
                    soundName = it
                    showSoundSheet = false
                },
                onDismiss = { showSoundSheet = false }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Alarm", color = TextWhite) },
                text = { Text("Are you sure you want to delete this alarm?", color = TextMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                    }) {
                        Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TextWhite)
                    }
                },
                containerColor = Color.Transparent,
                modifier = Modifier.glassDialog(shape = RoundedCornerShape(24.dp))
            )
        }
    }
}

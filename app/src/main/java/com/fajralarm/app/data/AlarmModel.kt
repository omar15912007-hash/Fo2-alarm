package com.fajralarm.app.data

import java.util.UUID

enum class MissionType(val displayName: String) {
    NONE("Normal Dismiss"),
    MATH("Math Problems"),
    SHAKE("Shake Phone"),
    MEMORY_MATRIX("Memory Matrix"),
    TYPING("Typing Mission"),
    STEPS("Walk Steps Mission"),
    SQUAT("Squat Exercise Mission")
}

data class AlarmModel(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 7,       // 24-hr format (0-23)
    val minute: Int = 0,     // (0-59)
    val label: String = "Wake Up",
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = emptyList(), // Default: no days pre-selected (empty)
    val missionType: MissionType = MissionType.MATH,

    // Math Mission Settings
    val mathDifficulty: Int = 2, // 1: Very Easy, 2: Easy, 3: Normal, 4: Hard, 5: Very Hard
    val mathQuestionsCount: Int = 3, // 1 to 20

    // Shake Mission Settings
    val shakeTargetCount: Int = 30, // 5 to 100
    val shakeSensitivity: Int = 2,  // 1: Low, 2: Medium, 3: High

    // Memory Matrix Settings
    val memoryGridSize: Int = 3, // 3: 3x3, 4: 4x4, 5: 5x5

    // Typing Mission Settings
    val typingQuote: String = "I am awake, energized, and ready for today!",

    // Walk Steps Mission Settings
    val stepTargetCount: Int = 20, // 5 to 100

    // Squat Exercise Mission Settings
    val squatTargetCount: Int = 15, // 5 to 50

    // Sound & Volume Settings
    val soundResName: String = "siren",
    val volume: Float = 1.0f, // 0.0 to 1.0
    val isLoudBoostEnabled: Boolean = true,
    val gentleWakeUpMinutes: Int = 0, // 0: Off, 1: 1 min, 2: 2 min, 5: 5 min
    val isVibrate: Boolean = true,

    // Snooze Settings
    val isSnoozeEnabled: Boolean = true,
    val snoozeIntervalMinutes: Int = 5, // 1, 3, 5, 10, 15
    val maxSnoozeCount: Int = 3, // 1, 2, 3, 5, 999 (Unlimited)
    val currentSnoozeCount: Int = 0,

    // Wake Up Check Settings (Follow-up verification check: 1 to 30 mins)
    val isWakeUpCheckEnabled: Boolean = false,
    val wakeUpCheckMinutes: Int = 15, // 1 to 30 minutes after alarm

    // Anti-Cheat & Mission Behaviors
    val muteDuringMission: Boolean = true
)

data class AlarmHistoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val alarmId: String,
    val alarmLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val missionType: MissionType,
    val timeToSolveSeconds: Int,
    val wasSnoozed: Boolean = false,
    val isSuccess: Boolean = true
)

data class AppGlobalSettings(
    val missionTimeLimitSeconds: Int = 20, // 10s, 15s, 20s, 30s, 60s
    val isVolumeLockEnabled: Boolean = true, // Force volume back to 100% every 500ms
    val isPreventTurnOffEnabled: Boolean = true,
    val isMuteAudioDuringMissionEnabled: Boolean = true,
    val maxQuickMutes: Int = 3, // 1, 2, 3, 5, -1 (Unlimited)
    val isDeepBlueAccent: Boolean = false // false = Electric Blue (#2979FF), true = Deep Blue (#1E88E5)
)

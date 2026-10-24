package com.fajralarm.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.fajralarm.app.ui.ringing.AlarmRingingActivity

class AlarmAccessibilityService : AccessibilityService() {

    private val suspiciousPackages = listOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only enforce anti-dismissal shield while an alarm is actively ringing
        if (!AlarmService.isAlarmCurrentlyRinging) return

        val packageName = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""

        Log.d("ShieldAccessibility", "Event from: $packageName / $className")

        // Intercept attempts to open Settings to force-stop or Power Off menu
        val isTryingToEvade = suspiciousPackages.any { packageName.contains(it) } ||
                className.contains("GlobalActions", ignoreCase = true) || // Power menu dialog
                className.contains("Shutdown", ignoreCase = true) ||
                className.contains("PowerOff", ignoreCase = true)

        if (isTryingToEvade) {
            Toast.makeText(this, "⚠️ Alarm is active! Solve your mission to dismiss.", Toast.LENGTH_SHORT).show()

            // Re-launch Alarm Ringing Activity immediately to cover the screen
            try {
                val ringIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                    if (AlarmService.activeAlarmId.isNotBlank()) {
                        putExtra("EXTRA_ALARM_ID", AlarmService.activeAlarmId)
                    }
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(ringIntent)
            } catch (e: Exception) {
                Log.w("ShieldAccessibility", "Unable to bring AlarmRingingActivity to front", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.w("ShieldAccessibility", "Accessibility service interrupted.")
    }
}

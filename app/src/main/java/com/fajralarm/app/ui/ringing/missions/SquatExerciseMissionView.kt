package com.fajralarm.app.ui.ringing.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.ui.theme.*
import kotlin.math.sqrt

/**
 * Squat Exercise Mission:
 * Uses accelerometer vertical phase detection (downward drop followed by upward push)
 * with a mandatory 800ms cooldown between reps to prevent cheating by shaking.
 */
@Composable
fun SquatExerciseMissionView(
    targetCount: Int = 15,
    onSuccess: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentSquats by remember { mutableIntStateOf(0) }
    var phaseText by remember { mutableStateOf("Hold phone upright & squat down") }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastSquatTime = 0L
        var hasEnteredDipPhase = false
        var hasPushedUp = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val now = System.currentTimeMillis()

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                // Phase 1: Downward dip (body descending reduces apparent gravity force < 7.5 m/s²)
                if (magnitude < 7.5f && (now - lastSquatTime >= 800)) {
                    hasEnteredDipPhase = true
                    phaseText = "Push back up!"
                }

                // Phase 2: Deceleration / upward push (legs braking and pushing back up > 12.2 m/s²)
                if (hasEnteredDipPhase && magnitude > 12.2f) {
                    hasPushedUp = true
                }

                // Phase 3: Return to standing neutral (~9.8 m/s² ± 1.5) with completion
                if (hasEnteredDipPhase && hasPushedUp && magnitude in 8.5f..11.2f) {
                    // Check strict 800ms anti-cheat cooldown
                    if (now - lastSquatTime >= 800) {
                        lastSquatTime = now
                        hasEnteredDipPhase = false
                        hasPushedUp = false
                        phaseText = "Great rep! Next squat..."
                        onUserInteraction()

                        if (currentSquats + 1 >= targetCount) {
                            currentSquats = targetCount
                            phaseText = "Mission Complete!"
                            onSuccess()
                        } else {
                            currentSquats++
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val progress by animateFloatAsState(
        targetValue = (currentSquats.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f),
        label = "squatProg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = VividCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Squat Exercise Mission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = "$currentSquats / $targetCount",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            GlowingGradientProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Center Squat Counter Display Ring
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                color = ElectricBlue,
                trackColor = CardBackground
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${targetCount - currentSquats}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Text(
                    text = "SQUATS LEFT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan,
                    letterSpacing = 1.sp
                )
            }
        }

        // Live Guidance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = phaseText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hold your phone against your chest and perform full squats. An 800ms cooldown prevents cheating.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

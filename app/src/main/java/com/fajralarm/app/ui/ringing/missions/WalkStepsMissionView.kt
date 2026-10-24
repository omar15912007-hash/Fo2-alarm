package com.fajralarm.app.ui.ringing.missions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
 * Walk Steps Mission:
 * Utilizes hardware Sensor.TYPE_STEP_DETECTOR with cadence validation (ignoring rapid <250ms shakes)
 * and falls back seamlessly to accelerometer peak detection on devices without a step detector.
 */
@Composable
fun WalkStepsMissionView(
    targetCount: Int = 20,
    onSuccess: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentSteps by remember { mutableIntStateOf(0) }
    var isUsingFallback by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastStepTime = 0L
        var isAboveThreshold = false
        var hasCompleted = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || hasCompleted) return
                val now = System.currentTimeMillis()

                if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    // Hardware step detector: validate cadence to prevent fast jitter/shaking
                    if (now - lastStepTime >= 250) {
                        lastStepTime = now
                        onUserInteraction()
                        if (currentSteps + 1 >= targetCount) {
                            currentSteps = targetCount
                            hasCompleted = true
                            onSuccess()
                        } else {
                            currentSteps++
                        }
                    }
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Accelerometer fallback with realistic step frequency filtering
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    if (magnitude > 12.8f && !isAboveThreshold && (now - lastStepTime >= 280)) {
                        isAboveThreshold = true
                        lastStepTime = now
                        onUserInteraction()
                        if (currentSteps + 1 >= targetCount) {
                            currentSteps = targetCount
                            hasCompleted = true
                            onSuccess()
                        } else {
                            currentSteps++
                        }
                    } else if (magnitude < 9.8f) {
                        isAboveThreshold = false
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val hasActivityPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        val registeredHardwareStep = hasActivityPermission &&
            stepDetectorSensor != null &&
            sensorManager.registerListener(listener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)

        if (!registeredHardwareStep && accelerometer != null) {
            isUsingFallback = true
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val progress by animateFloatAsState(
        targetValue = (currentSteps.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f),
        label = "walkStepProg"
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
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = VividCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Walk Steps Mission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = "$currentSteps / $targetCount",
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

        // Center Step Counter Display Ring
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
                    text = "${targetCount - currentSteps}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Text(
                    text = "STEPS REMAINING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividCyan,
                    letterSpacing = 1.sp
                )
            }
        }

        // Instructions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🚶 Walk with natural steps to dismiss the alarm.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cadence validation enforces actual walking. Shaking the phone rapidly will not count.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

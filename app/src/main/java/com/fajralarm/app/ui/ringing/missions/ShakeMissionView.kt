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
import androidx.compose.material.icons.filled.Vibration
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

@Composable
fun ShakeMissionView(
    targetShakes: Int,
    sensitivityLevel: Int, // 1: Low, 2: Medium, 3: High
    onSuccess: () -> Unit,
    onUserInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    var shakesRemaining by remember { mutableIntStateOf(targetShakes) }

    val threshold = when (sensitivityLevel) {
        1 -> 16.5f // Low sensitivity (requires vigorous shakes)
        2 -> 13.0f // Medium sensitivity
        else -> 10.0f // High sensitivity
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastUpdate = 0L
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val curTime = System.currentTimeMillis()
                if ((curTime - lastUpdate) > 130) {
                    val diffTime = curTime - lastUpdate
                    lastUpdate = curTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed = sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)).toDouble()) / diffTime * 10000

                    if (speed > threshold * 100) {
                        onUserInteraction()
                        if (shakesRemaining > 1) {
                            shakesRemaining--
                        } else {
                            shakesRemaining = 0
                            onSuccess()
                        }
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val progress by animateFloatAsState(
        targetValue = (targetShakes - shakesRemaining).toFloat() / targetShakes.toFloat(),
        label = "shakeProg"
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
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = VividCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shake Mission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}% Done",
                    fontSize = 14.sp,
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

        // Center Animated Shakes Remaining Ring
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
                    text = "$shakesRemaining",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Text(
                    text = "SHAKES LEFT",
                    fontSize = 12.sp,
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
            Text(
                text = "Shake your phone up and down firmly until the counter reaches zero!",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

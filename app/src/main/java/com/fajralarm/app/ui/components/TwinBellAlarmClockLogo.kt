package com.fajralarm.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fajralarm.app.R
import com.fajralarm.app.ui.theme.ElectricBlue
import com.fajralarm.app.ui.theme.PitchBlack
import com.fajralarm.app.ui.theme.TextWhite

/**
 * Crisp Electric Blue Twin-Bell Mechanical Alarm Clock Logo.
 * Renders the exact crisp Vector XML with optional vibration animation when ringing.
 */
@Composable
fun AlarmLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isRinging: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarmVibe")
    val rotation by if (isRinging) {
        infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 75, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rot"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Fo2 Alarm 3D Clock Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Backward compatibility alias for AlarmLogo.
 */
@Composable
fun TwinBellAlarmClockLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isRinging: Boolean = false,
    accentColor: Color = ElectricBlue,
    dialColor: Color = PitchBlack,
    handColor: Color = TextWhite
) {
    AlarmLogo(
        modifier = modifier,
        size = size,
        isRinging = isRinging
    )
}

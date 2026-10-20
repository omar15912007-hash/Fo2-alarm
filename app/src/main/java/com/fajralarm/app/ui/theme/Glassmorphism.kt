package com.fajralarm.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * World-Class Glassmorphic modifier:
 * Applies Elevated Glass Card gradient (Brush.verticalGradient from #121622 to #181D2C)
 * and subtle 1.dp gradient glass border (White 8% to ElectricBlue 25%).
 */
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(20.dp),
    borderGradient: Brush = GlassBorderGradient,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(GlassCardGradient, shape)
    .border(borderWidth, borderGradient, shape)

/**
 * World-Class Glassmorphic dialog container modifier.
 */
fun Modifier.glassDialog(
    shape: Shape = RoundedCornerShape(24.dp),
    borderGradient: Brush = GlassBorderGradient
): Modifier = this
    .clip(shape)
    .background(GlassCardGradient, shape)
    .border(1.dp, borderGradient, shape)

/**
 * Fluid Spring Micro-Interaction Modifier:
 * Adds buttery bouncy scale press animation (animateFloatAsState with spring(dampingRatio = Spring.DampingRatioMediumBouncy))
 * on buttons, cards, and day chips.
 */
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncyScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Glowing Gradient Progress Bar:
 * Displays a smooth gradient progress from ElectricBlue (#2979FF) to CyanGlow (#00E5FF).
 */
@Composable
fun GlowingGradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(height / 2),
    backgroundColor: Color = Color(0xFF0C101A)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "glowingProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(backgroundColor)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(CyanGlowGradient)
            )
        }
    }
}

/**
 * Cinematic InfiniteTransition Pulsing Neon Aura:
 * Pulses behind the ringing alarm clock with dynamic scale and alpha breathing.
 */
@Composable
fun PulsingNeonAura(
    modifier: Modifier = Modifier,
    baseSize: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingNeonAura")

    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Box(
        modifier = modifier
            .size(baseSize)
            .graphicsLayer {
                scaleX = auraScale
                scaleY = auraScale
                alpha = auraAlpha
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CyanGlow.copy(alpha = 0.6f),
                        ElectricBlue.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

/**
 * Sleek Circular Countdown Ring around Quick Mute button:
 * Animates a glowing radial arc indicating remaining mute duration.
 */
@Composable
fun CircularCountdownMuteRing(
    progress: Float,
    secondsRemaining: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sweepAngle by animateFloatAsState(
        targetValue = (progress.coerceIn(0f, 1f)) * 360f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "muteRingSweep"
    )

    Box(
        modifier = modifier
            .size(54.dp)
            .bouncyClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Background track circle
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            drawCircle(
                color = GlassCardEnd,
                style = Stroke(width = strokeWidth)
            )
            if (sweepAngle > 0f) {
                drawArc(
                    brush = CyanGlowGradient,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        Text(
            text = "${secondsRemaining}s",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (secondsRemaining > 0) CyanGlow else TextMuted
        )
    }
}

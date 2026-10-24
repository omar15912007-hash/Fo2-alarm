package com.fajralarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fajralarm.app.receiver.AlarmScheduler
import com.fajralarm.app.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * World-Class 3D Haptic Time Wheel Picker.
 * Features 3D perspective scaling/alpha, glowing central selection capsule,
 * fontFeatureSettings = "tnum" (tabular numerals so numbers never jitter),
 * and crisp tick haptic feedback as numbers snap into place.
 */
@Composable
fun WheelTimePicker(
    hour12: Int,
    minute: Int,
    isAm: Boolean,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onAmPmChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnHourChanged by rememberUpdatedState(onHourChanged)
    val currentOnMinuteChanged by rememberUpdatedState(onMinuteChanged)
    val currentOnAmPmChanged by rememberUpdatedState(onAmPmChanged)
    val haptic = LocalHapticFeedback.current

    val hoursList = remember { (1..12).toList() }
    val minutesList = remember { (0..59).toList() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Central Selection Capsule (Glassmorphic Glow & Gradient Border)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.18f),
                            CyanGlow.copy(alpha = 0.10f),
                            ElectricBlue.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.45f),
                            CyanGlow.copy(alpha = 0.70f),
                            ElectricBlue.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Decoupled Infinite Hours Wheel (1..12) with Haptics and tnum
            Infinite3DWheel(
                items = hoursList,
                selectedValue = hour12,
                onValueSelected = { newHour ->
                    if (newHour != hour12) {
                        currentOnHourChanged(newHour)
                    }
                },
                itemLabel = { String.format("%02d", it) },
                modifier = Modifier.weight(1f)
            )

            // Colon separator
            Text(
                text = ":",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // 2. Decoupled Infinite Minutes Wheel (00..59) with Haptics and tnum
            Infinite3DWheel(
                items = minutesList,
                selectedValue = minute,
                onValueSelected = { newMinute ->
                    if (newMinute != minute) {
                        currentOnMinuteChanged(newMinute)
                    }
                },
                itemLabel = { String.format("%02d", it) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Decoupled AM / PM Selector Column with Spring Bouncy Micro-Interactions & Tick Haptics
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .height(96.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // AM Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isAm) Brush.horizontalGradient(listOf(ElectricBlue, CyanGlow.copy(alpha = 0.85f)))
                            else Brush.verticalGradient(listOf(GlassCardStart, GlassCardEnd))
                        )
                        .border(
                            1.dp,
                            if (isAm) CyanGlow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                        .bouncyClickable {
                            if (!isAm) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnAmPmChanged(true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AM",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isAm) TextWhite else TextMuted
                    )
                }

                // PM Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (!isAm) Brush.horizontalGradient(listOf(ElectricBlue, CyanGlow.copy(alpha = 0.85f)))
                            else Brush.verticalGradient(listOf(GlassCardStart, GlassCardEnd))
                        )
                        .border(
                            1.dp,
                            if (!isAm) CyanGlow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                        .bouncyClickable {
                            if (isAm) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnAmPmChanged(false)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PM",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (!isAm) TextWhite else TextMuted
                    )
                }
            }
        }
    }
}

/**
 * 3D Infinite Circular Wheel Component with snap fling behavior and perspective transformations.
 */
@Composable
private fun <T> Infinite3DWheel(
    items: List<T>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 56.dp
) {
    val currentOnValueSelected by rememberUpdatedState(onValueSelected)
    val haptic = LocalHapticFeedback.current
    val repeatMultiplier = 10_000
    val totalItemCount = items.size * repeatMultiplier

    val selectedIndex = remember(selectedValue, items) {
        val idx = items.indexOf(selectedValue)
        if (idx >= 0) idx else 0
    }

    // Midpoint aligned with the target item
    val initialListIndex = remember {
        val half = totalItemCount / 2
        half - (half % items.size) + selectedIndex
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    // Listen to scroll settlements and emit value changes with crisp tick haptics (skip initial mount emission)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { index ->
                val realIndex = index % items.size
                if (realIndex in items.indices) {
                    val value = items[realIndex]
                    currentOnValueSelected(value)
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        modifier = modifier.height(itemHeight * 3),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(totalItemCount) { index ->
            val realIndex = index % items.size
            val itemValue = items[realIndex]

            // 3D Perspective Transformation based on item distance from viewport center
            val itemOffsetRatio = remember(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                calculateItemOffsetRatio(listState, index)
            }

            val scale = (1f - (abs(itemOffsetRatio) * 0.20f)).coerceIn(0.75f, 1f)
            val alpha = (1f - (abs(itemOffsetRatio) * 0.55f)).coerceIn(0.25f, 1f)
            val rotationX = itemOffsetRatio * 24f
            val isCenter = abs(itemOffsetRatio) < 0.45f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.rotationX = rotationX
                        cameraDistance = 12f * density
                    }
                    .clickable {
                        // Smoothly scroll clicked item to center
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                        currentOnValueSelected(itemValue)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itemLabel(itemValue),
                    fontSize = if (isCenter) 36.sp else 24.sp,
                    fontWeight = if (isCenter) FontWeight.Black else FontWeight.Medium,
                    color = if (isCenter) TextWhite else TextMuted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}

private fun calculateItemOffsetRatio(listState: LazyListState, index: Int): Float {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 1f
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
    return (itemCenter - viewportCenter) / itemInfo.size.toFloat()
}

/**
 * Formats live dynamic remaining time e.g. "Alarm rings in 7 hours and 25 minutes".
 */
fun formatAlarmRemainingTime(hour24: Int, minute: Int, daysOfWeek: List<Int>): String {
    val nowMillis = System.currentTimeMillis()
    val targetMillis = AlarmScheduler.computeNextTriggerMillis(hour24, minute, daysOfWeek, nowMillis)
    val diffMillis = (targetMillis - nowMillis).coerceAtLeast(0L)
    val diffMinutes = (diffMillis / (1000 * 60)).toInt()
    val hours = diffMinutes / 60
    val minutes = diffMinutes % 60

    return when {
        hours <= 0 && minutes <= 1 -> "Alarm rings in less than a minute"
        hours <= 0 -> "Alarm rings in $minutes minutes"
        minutes == 0 -> "Alarm rings in $hours hours"
        else -> "Alarm rings in $hours hours and $minutes minutes"
    }
}

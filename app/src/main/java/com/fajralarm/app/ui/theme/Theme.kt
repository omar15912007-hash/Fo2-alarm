package com.fajralarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = VividCyan,
    tertiary = ElectricBlue,
    background = PitchBlack,
    surface = DarkSurface,
    onPrimary = TextWhite,
    onSecondary = PitchBlack,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun FajrAlarmTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

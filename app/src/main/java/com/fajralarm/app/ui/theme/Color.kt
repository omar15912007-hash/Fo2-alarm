package com.fajralarm.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Deep Obsidian & Glassmorphic Surfaces
val DeepObsidian = Color(0xFF07090E)        // Rich Deep Obsidian Background
val PitchBlack = DeepObsidian              // Backwards-compatible alias for primary background
val GlassCardStart = Color(0xFF121622)      // Elevated Glass Card Start (#121622)
val GlassCardEnd = Color(0xFF181D2C)        // Elevated Glass Card End (#181D2C)
val DarkSurface = GlassCardStart            // Dark Surface Cards
val CardBackground = Color(0xFF191F2D)      // Elevated Card Surface
val BorderStrokeColor = Color(0xFF262D3D)   // Subtle Border

// Vibrant Electric Blue Accents & Cyan Glow Highlights
val ElectricBlue = Color(0xFF2979FF)        // Vibrant Electric Blue (#2979FF)
val PrimaryBlue = ElectricBlue              // Alias for consistent component styling
val CyanGlow = Color(0xFF00E5FF)            // Cyan Glow Highlights (#00E5FF)
val VividCyan = CyanGlow                    // Alias for glow & highlights
val DeepBlue = Color(0xFF1E88E5)            // Alternative Deep Accent
val SoftBlue = Color(0xFF82B1FF)            // Soft Accent Text

// Glassmorphic Gradients
val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(GlassCardStart, GlassCardEnd)
)
val GlassBorderGradient = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.08f),
        ElectricBlue.copy(alpha = 0.25f)
    )
)
val CyanGlowGradient = Brush.horizontalGradient(
    colors = listOf(ElectricBlue, CyanGlow)
)

// Typography Colors
val TextWhite = Color(0xFFFFFFFF)           // Pure White Header & Active Text
val TextMuted = Color(0xFF8E929F)           // Subtle Gray Secondary Text
val TextSubtle = Color(0xFF6B7280)          // De-emphasized Text

// Status & Alert Colors
val DangerRed = Color(0xFFEF4444)           // Delete & Stop Accent
val EmeraldGreen = Color(0xFF10B981)        // Success & Active Shield
val AmberWarning = Color(0xFFF59E0B)        // Warning Accent

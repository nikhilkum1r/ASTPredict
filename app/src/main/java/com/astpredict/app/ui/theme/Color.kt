package com.astpredict.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors — deep teal/cyan scientific palette
val PrimaryDark = Color(0xFF00897B)      // Teal 600
val PrimaryLight = Color(0xFF4DB6AC)     // Teal 300
val Primary = Color(0xFF009688)          // Teal 500

// Secondary accent — amber for highlights
val Secondary = Color(0xFFFFB300)        // Amber 600
val SecondaryLight = Color(0xFFFFD54F)   // Amber 300

// Background palette (dark theme)
val DarkBackground = Color(0xFF0A0E1A)
val DarkSurface = Color(0xFF121829)
val DarkSurfaceVariant = Color(0xFF1A2035)
val DarkCard = Color(0xFF1E2640)

// Background palette (light theme)
val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightCard = Color(0xFFFFFFFF)

// Text colors
val TextPrimaryDark = Color(0xFFE8EAF0)
val TextSecondaryDark = Color(0xFFB0B8C8)
val TextPrimaryLight = Color(0xFF1A1C2E)
val TextSecondaryLight = Color(0xFF5A6072)

// Status colors
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Error = Color(0xFFF44336)
val Info = Color(0xFF2196F3)

// Colony detection overlay colors (for bounding boxes)
val DetectionOverlay = Color(0x4000E676) // Semi-transparent green
val DetectionBorder = Color(0xFF00E676)  // Bright green
val ConfidenceHigh = Color(0xFF00E676)   // > 0.8
val ConfidenceMedium = Color(0xFFFFEB3B) // 0.5 - 0.8
val ConfidenceLow = Color(0xFFFF5722)    // < 0.5

// Gradient colors
val GradientStart = Color(0xFF00897B)
val GradientMid = Color(0xFF00695C)
val GradientEnd = Color(0xFF004D40)

val GradientAccentStart = Color(0xFF6200EA)
val GradientAccentEnd = Color(0xFF00BFA5)
package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// Brand gradient — cyan to purple (matches logo)
val AccentCyan = Color(0xFF06D6D6)
val AccentPurple = Color(0xFFA855F7)
val AccentMid = Color(0xFF7C6BF0)

// Legacy aliases mapped to new accent palette
val BrandBlue = Color(0xFFFFFFFF)
val BrandBlueDark = Color(0xFFE0E0E0)
val BrandTeal = AccentCyan
val BrandTealDark = Color(0xFF059999)
val BrandPurple = AccentPurple
val BrandPurpleDark = Color(0xFF7C3AED)
val BrandCyan = AccentCyan
val BrandPink = Color(0xFFFF4444)
val BrandGold = Color(0xFFFFFFFF)

// Dark theme colors
val PrimaryDark = Color(0xFF111111)
val SecondaryDark = Color(0xFF1A1A1A)
val TertiaryDark = Color(0xFF2A2A2A)
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF111111)
val SurfaceDarkGlass = Color(0xFF111111).copy(alpha = 0.8f)

// Gradient colors for backgrounds (flat black)
val GradientStart = Color(0xFF000000)
val GradientMiddle = Color(0xFF000000)
val GradientEnd = Color(0xFF000000)

// Accent colors for focus states
val FocusBorder = AccentCyan
val FocusGlow = AccentCyan.copy(alpha = 0.15f)

// Text colors — high contrast for TV
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFBBBBBB)
val TextTertiary = Color(0xFF777777)
val TextDisabled = Color(0xFF444444)

// Card colors
val CardBackground = Color(0xFF141414).copy(alpha = 0.6f)
val CardBackgroundHover = Color(0xFF1E1E1E).copy(alpha = 0.8f)
val CardBackgroundFocused = Color(0xFFFFFFFF).copy(alpha = 0.1f)

// Navigation colors
val NavBackground = Color(0xFF000000).copy(alpha = 0.95f)
val NavItemActive = Color(0xFFFFFFFF)
val NavItemInactive = Color(0xFF555555)
val NavItemFocused = Color(0xFFFFFFFF)

// Light theme colors
val BackgroundLight = Color(0xFFF5F5F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SecondaryLight = Color(0xFFE0E0E0)
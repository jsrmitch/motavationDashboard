package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = BrandBlue,
            onPrimary = TextPrimary,
            primaryContainer = BrandBlueDark,
            onPrimaryContainer = TextPrimary,
            secondary = BrandTeal,
            onSecondary = TextPrimary,
            secondaryContainer = BrandTealDark,
            onSecondaryContainer = TextPrimary,
            tertiary = BrandPurple,
            onTertiary = TextPrimary,
            tertiaryContainer = BrandPurpleDark,
            onTertiaryContainer = TextPrimary,
            background = BackgroundDark,
            onBackground = TextPrimary,
            surface = SurfaceDark,
            onSurface = TextPrimary,
            surfaceVariant = SecondaryDark,
            onSurfaceVariant = TextSecondary,
            error = BrandPink,
            onError = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = BrandBlue,
            onPrimary = TextPrimary,
            primaryContainer = BrandBlueDark,
            onPrimaryContainer = TextPrimary,
            secondary = BrandTeal,
            onSecondary = TextPrimary,
            secondaryContainer = BrandTealDark,
            onSecondaryContainer = TextPrimary,
            tertiary = BrandPurple,
            onTertiary = TextPrimary,
            tertiaryContainer = BrandPurpleDark,
            onTertiaryContainer = TextPrimary,
            background = BackgroundLight,
            onBackground = PrimaryDark,
            surface = SurfaceLight,
            onSurface = PrimaryDark,
            surfaceVariant = SecondaryLight,
            onSurfaceVariant = TextSecondary,
            error = BrandPink,
            onError = TextPrimary
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}